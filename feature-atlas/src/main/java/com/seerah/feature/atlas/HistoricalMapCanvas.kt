package com.seerah.feature.atlas

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.seerah.core.theme.*
import com.seerah.data.geo.GeoJsonParser
import com.seerah.data.geo.MapProjectionEngine
import com.seerah.data.geo.MbTilesReader
import com.seerah.database.entity.BattleMapFeatureEntity
import com.seerah.database.entity.PlaceCoordinateEntity
import com.seerah.database.entity.PlaceEntity
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import com.seerah.domain.model.GeographicCertainty
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.*

/**
 * محرك الأطلس الجغرافي (v3.0.2-alpha.6):
 * - خريطة أساس أوفلاين مدمجة (سواحل البحر الأحمر والخليج العربي والتضاريس والأودية)
 *   مع مسارات السيرة والهجرة والغزوات التقريبية.
 * - يُسقط بلاطات MBTiles (عند استيرادها) باستخدام إحداثياتها الجغرافية الحقيقية (Web Mercator + Zoom + Pan).
 * - يرسم جميع الأنواع السبعة لـ GeoJSON RFC 7946 مع توزيع ذكي لتسميات المواقع يمنع التداخل.
 */
@Composable
fun HistoricalMapCanvas(
    mapDefinition: AtlasMapDefinition,
    enabledLayers: Set<String>,
    features: List<BattleMapFeatureEntity>,
    bundledBaseFeatures: List<GeoFeatureModel> = emptyList(),
    places: List<PlaceEntity> = emptyList(),
    coordinates: List<PlaceCoordinateEntity> = emptyList(),
    mbtilesLocalPath: String? = null,
    zoom: Float,
    panOffset: Offset,
    onTransform: (zoomDelta: Float, panDelta: Offset) -> Unit,
    onFeatureSelected: (GeoFeatureModel?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val minLat = mapDefinition.minLat
    val maxLat = mapDefinition.maxLat
    val minLon = mapDefinition.minLon
    val maxLon = mapDefinition.maxLon

    val coordByPlace = remember(coordinates) { coordinates.associateBy { it.placeId } }

    val geoPoints = remember(places, coordByPlace, enabledLayers, mapDefinition) {
        if (!enabledLayers.contains("historical_sites") && !enabledLayers.contains("cities_villages")) {
            emptyList()
        } else {
            places.mapNotNull { place ->
                val c = coordByPlace[place.id] ?: return@mapNotNull null
                if (!GeoJsonParser.isValidCoordinate(c.latitude, c.longitude)) return@mapNotNull null
                val inBounds = mapDefinition.contains(c.latitude, c.longitude) || place.mapId == mapDefinition.mapId
                if (!inBounds) return@mapNotNull null
                GeoFeatureModel(
                    id = "place_${place.id}",
                    layerKey = "historical_sites",
                    geometryType = "Point",
                    label = place.name,
                    coordinates = listOf(GeoPointModel(c.latitude, c.longitude)),
                    certainty = GeographicCertainty.fromCode(place.certaintyLevel),
                    sourceCitation = c.sourceCitation.ifBlank { "يحتاج توثيق مرجع الإحداثية" },
                    featureType = place.mapId
                )
            }
        }
    }

    val activeBundledFeatures = remember(bundledBaseFeatures, enabledLayers) {
        bundledBaseFeatures.filter { enabledLayers.contains(it.layerKey) }
    }

    val importedGeoFeatures = remember(features, enabledLayers) {
        features.flatMap { entity ->
            runCatching { GeoJsonParser.parseFeatureCollection(entity.geojsonGeometry) }
                .getOrDefault(emptyList())
        }.filter { enabledLayers.contains(it.layerKey) }
    }

    val allGeoFeatures = remember(activeBundledFeatures, importedGeoFeatures) {
        activeBundledFeatures + importedGeoFeatures
    }

    // تقسيم المعالم: طبقة الأساس (مياه + تضاريس + أودية) تُرسم تحت الشبكة، والمسارات فوقها
    val baseTerrainFeatures = remember(allGeoFeatures) {
        allGeoFeatures.filter { it.featureType in setOf("WATER", "MOUNTAIN", "VALLEY") || it.layerKey in setOf("terrain", "mountains", "valleys") }
    }
    val overlayFeatures = remember(allGeoFeatures) {
        allGeoFeatures.filterNot { it.featureType in setOf("WATER", "MOUNTAIN", "VALLEY") || it.layerKey in setOf("terrain", "mountains", "valleys") }
    }

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var lastTwoSelected by remember { mutableStateOf<List<GeoFeatureModel>>(emptyList()) }
    var tileRevision by remember { mutableIntStateOf(0) }

    var mbTilesReader by remember(mbtilesLocalPath) { mutableStateOf<MbTilesReader?>(null) }
    LaunchedEffect(mbtilesLocalPath) {
        val path = mbtilesLocalPath?.takeIf { it.isNotBlank() }
        if (path == null) {
            mbTilesReader = null
        } else {
            val candidate = MbTilesReader(File(path))
            if (candidate.openAsync()) {
                mbTilesReader = candidate
            } else {
                candidate.close()
                mbTilesReader = null
            }
        }
    }
    DisposableEffect(mbTilesReader) {
        val active = mbTilesReader
        onDispose { active?.close() }
    }

    // تحميل بلاطات MBTiles المرئية فقط على Dispatchers.IO مع إلغاء تلقائي عند تغيير Zoom/Pan
    LaunchedEffect(mbTilesReader, mapDefinition, zoom, panOffset, canvasSize) {
        val reader = mbTilesReader ?: return@LaunchedEffect
        if (canvasSize == Size.Zero) return@LaunchedEffect
        val proj = MapProjectionEngine(minLat, maxLat, minLon, maxLon, canvasSize.width, canvasSize.height, zoom, panOffset.x, panOffset.y)
        val vis = proj.visibleGeoBounds()
        val lonSpan = (vis.eastLon - vis.westLon).coerceAtLeast(0.01)
        val meta = reader.metadata
        val tileZoom = (ln(360.0 / lonSpan) / ln(2.0) + 1.0).toInt().coerceIn(meta.minZoom, meta.maxZoom)
        val (xMin, yMin) = MbTilesReader.latLonToTileXY(vis.northLat, vis.westLon, tileZoom)
        val (xMax, yMax) = MbTilesReader.latLonToTileXY(vis.southLat, vis.eastLon, tileZoom)
        val maxIdx = (1 shl tileZoom) - 1
        for (tx in xMin.coerceAtLeast(0)..xMax.coerceAtMost(maxIdx)) {
            for (ty in yMin.coerceAtLeast(0)..yMax.coerceAtMost(maxIdx)) {
                val b = MbTilesReader.tileToBounds(tileZoom, tx, ty)
                val tl = proj.project(b.northLat, b.westLon)
                val br = proj.project(b.southLat, b.eastLon)
                if (br.x < 0f || tl.x > canvasSize.width || br.y < 0f || tl.y > canvasSize.height) continue
                if (reader.getTileBitmapIfCached(tileZoom, tx, ty) == null) {
                    launch {
                        if (reader.loadTileAsync(tileZoom, tx, ty) != null) {
                            tileRevision++
                        }
                    }
                }
            }
        }
    }

    fun mercatorY(lat: Double): Double {
        val rad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        return ln(tan(Math.PI / 4.0 + rad / 2.0))
    }

    val yMinMerc = mercatorY(minLat)
    val yMaxMerc = mercatorY(maxLat)

    fun projectLatLon(lat: Double, lon: Double, size: Size): Offset {
        val nx = ((lon - minLon) / (maxLon - minLon).coerceAtLeast(0.0001)).toFloat()
        val ny = (1.0 - (mercatorY(lat) - yMinMerc) / (yMaxMerc - yMinMerc).coerceAtLeast(0.0001)).toFloat()
        val bx = nx * size.width
        val by = ny * size.height
        val cx = size.width / 2f
        val cy = size.height / 2f
        return Offset(
            x = cx + (bx - cx) * zoom + panOffset.x,
            y = cy + (by - cy) * zoom + panOffset.y
        )
    }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#0B2923")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val waterLabelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#3E666B")
            textSize = 23f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
    }
    val terrainLabelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#6E5438")
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }
    val gridTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#8A6D4B")
            textSize = 18f
            textAlign = Paint.Align.LEFT
        }
    }
    val labelBgPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(240, 250, 247, 240)
            style = Paint.Style.FILL
        }
    }
    val labelBorderPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#B18A4A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    val waterFill = Color(0xFFBED6D9)
    val waterCoast = Color(0xFF6B9398)
    val mountainFill = Color(0xFFDECBA8)
    val mountainStroke = Color(0xFFA07E54)
    val valleyStroke = Color(0xFF7FA3A6)
    val landColor = Color(0xFFF5EEDF)

    val allClickable = remember(geoPoints, overlayFeatures) { geoPoints + overlayFeatures }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(landColor)
            .border(2.dp, SeerahAntiqueGold, RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onTransform(gestureZoom, pan)
                }
            }
            .pointerInput(allClickable, zoom, panOffset, canvasSize, mapDefinition) {
                detectTapGestures { tap ->
                    if (canvasSize == Size.Zero) return@detectTapGestures
                    val proj = MapProjectionEngine(minLat, maxLat, minLon, maxLon, canvasSize.width, canvasSize.height, zoom, panOffset.x, panOffset.y)
                    val hit = allClickable.firstOrNull { f -> proj.hitTest(tap.x, tap.y, f, tolerancePx = 44f) }
                    if (hit != null) {
                        onFeatureSelected(hit)
                        if (hit.geometryType == "Point" && hit.coordinates.isNotEmpty()) {
                            lastTwoSelected = (lastTwoSelected + hit).takeLast(2)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            drawRect(color = landColor)

            // 0. رسم بلاطات MBTiles إن وُجدت، وإلا رسم طبقة الأساس الجغرافية الأوفلاين (البحار والتضاريس والأودية)
            if (mbTilesReader != null) {
                val activeReader = mbTilesReader!!
                val projDraw = MapProjectionEngine(minLat, maxLat, minLon, maxLon, size.width, size.height, zoom, panOffset.x, panOffset.y)
                val vis = projDraw.visibleGeoBounds()
                val lonSpan = (vis.eastLon - vis.westLon).coerceAtLeast(0.01)
                val meta = activeReader.metadata
                val tileZoom = (ln(360.0 / lonSpan) / ln(2.0) + 1.0).toInt().coerceIn(meta.minZoom, meta.maxZoom)
                val (xMin, yMin) = MbTilesReader.latLonToTileXY(vis.northLat, vis.westLon, tileZoom)
                val (xMax, yMax) = MbTilesReader.latLonToTileXY(vis.southLat, vis.eastLon, tileZoom)
                val maxTileIdx = (1 shl tileZoom) - 1

                for (tx in xMin.coerceAtLeast(0)..xMax.coerceAtMost(maxTileIdx)) {
                    for (ty in yMin.coerceAtLeast(0)..yMax.coerceAtMost(maxTileIdx)) {
                        val b = MbTilesReader.tileToBounds(tileZoom, tx, ty)
                        val tl = projectLatLon(b.northLat, b.westLon, size)
                        val br = projectLatLon(b.southLat, b.eastLon, size)
                        if (br.x < 0f || tl.x > size.width || br.y < 0f || tl.y > size.height) continue
                        val wPx = ceil(br.x - tl.x).toInt().coerceAtLeast(1)
                        val hPx = ceil(br.y - tl.y).toInt().coerceAtLeast(1)
                        val _rev = tileRevision
                        val bmp = activeReader.getTileBitmapIfCached(tileZoom, tx, ty)
                        if (bmp != null) {
                            drawImage(
                                image = bmp.asImageBitmap(),
                                dstOffset = IntOffset(floor(tl.x).toInt(), floor(tl.y).toInt()),
                                dstSize = IntSize(wPx, hPx)
                            )
                        }
                    }
                }
            } else {
                // رسم المسطحات المائية والتضاريس والأودية المدمجة أوفلاين
                baseTerrainFeatures.forEach { feat ->
                    when (feat.geometryType) {
                        "Polygon", "MultiPolygon" -> {
                            val isWater = feat.featureType == "WATER"
                            val fillCol = if (isWater) waterFill else mountainFill.copy(alpha = 0.55f)
                            val strokeCol = if (isWater) waterCoast else mountainStroke.copy(alpha = 0.70f)
                            val strokeW = if (isWater) 2.5f else 1.6f
                            val dash = if (isWater) null else PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                            feat.polygons.forEach { polyRings ->
                                val outer = polyRings.outerRing.map { projectLatLon(it.latitude, it.longitude, size) }
                                if (outer.size >= 3) {
                                    val path = Path().apply {
                                        fillType = PathFillType.EvenOdd
                                        moveTo(outer.first().x, outer.first().y)
                                        outer.drop(1).forEach { lineTo(it.x, it.y) }
                                        close()
                                        polyRings.holes.forEach { hole ->
                                            val hPts = hole.map { projectLatLon(it.latitude, it.longitude, size) }
                                            if (hPts.size >= 3) {
                                                moveTo(hPts.first().x, hPts.first().y)
                                                hPts.drop(1).forEach { lineTo(it.x, it.y) }
                                                close()
                                            }
                                        }
                                    }
                                    drawPath(path, color = fillCol)
                                    drawPath(path, color = strokeCol, style = Stroke(strokeW, pathEffect = dash))

                                    // كتابة اسم البحر أو الجبل داخل النطاق المرئي إن ظهر جزء كافٍ منه
                                    val visiblePts = outer.filter { it.x in 30f..(size.width - 30f) && it.y in 40f..(size.height - 40f) }
                                    if (visiblePts.size >= 3) {
                                        val cx = visiblePts.map { it.x }.average().toFloat()
                                        val cy = visiblePts.map { it.y }.average().toFloat()
                                        if (isWater) {
                                            drawContext.canvas.nativeCanvas.drawText(feat.label, cx, cy, waterLabelPaint)
                                        } else if (feat.id != "mtn_hijaz_sarawat") {
                                            drawContext.canvas.nativeCanvas.drawText(feat.label, cx, cy, terrainLabelPaint)
                                        }
                                    }
                                }
                            }
                        }
                        "LineString", "MultiLineString" -> {
                            val lines = if (feat.multiLines.isNotEmpty()) feat.multiLines else listOf(feat.coordinates)
                            lines.forEach { lineCoords ->
                                val screenPts = lineCoords.map { projectLatLon(it.latitude, it.longitude, size) }
                                if (screenPts.size >= 2) {
                                    val path = Path().apply {
                                        moveTo(screenPts.first().x, screenPts.first().y)
                                        screenPts.drop(1).forEach { lineTo(it.x, it.y) }
                                    }
                                    drawPath(
                                        path,
                                        color = valleyStroke.copy(alpha = 0.78f),
                                        style = Stroke(3.2f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. شبكة إحداثيات WGS84 خفيفة وهادئة
            val latStep = ((maxLat - minLat) / 4.0).coerceAtLeast(0.02)
            val lonStep = ((maxLon - minLon) / 4.0).coerceAtLeast(0.02)
            var latCur = minLat
            while (latCur <= maxLat + 0.0001) {
                val y = projectLatLon(latCur, minLon, size).y
                if (y in 24f..(size.height - 28f)) {
                    drawLine(SeerahAntiqueGold.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), 1f)
                    drawContext.canvas.nativeCanvas.drawText("${"%.2f".format(latCur)}°N", 8f, y - 4f, gridTextPaint)
                }
                latCur += latStep
            }
            var lonCur = minLon
            while (lonCur <= maxLon + 0.0001) {
                val x = projectLatLon(minLat, lonCur, size).x
                if (x in 24f..(size.width - 36f)) {
                    drawLine(SeerahAntiqueGold.copy(alpha = 0.15f), Offset(x, 0f), Offset(x, size.height), 1f)
                    drawContext.canvas.nativeCanvas.drawText("${"%.2f".format(lonCur)}°E", x + 4f, 20f, gridTextPaint)
                }
                lonCur += lonStep
            }

            // 2. رسم المسارات والمعالم المستوردة والمدمجة (جميع الأنواع السبعة لـ GeoJSON)
            overlayFeatures.forEach { feat ->
                val isDisputed = feat.certainty != GeographicCertainty.CONFIRMED
                val dash = when {
                    feat.layerKey == "roads" -> PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                    isDisputed -> PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                    else -> null
                }

                when (feat.geometryType) {
                    "Polygon", "MultiPolygon" -> {
                        feat.polygons.forEach { polyRings ->
                            val path = Path().apply {
                                fillType = PathFillType.EvenOdd
                                val outer = polyRings.outerRing.map { projectLatLon(it.latitude, it.longitude, size) }
                                if (outer.size >= 3) {
                                    moveTo(outer.first().x, outer.first().y)
                                    outer.drop(1).forEach { lineTo(it.x, it.y) }
                                    close()
                                }
                                polyRings.holes.forEach { hole ->
                                    val hPts = hole.map { projectLatLon(it.latitude, it.longitude, size) }
                                    if (hPts.size >= 3) {
                                        moveTo(hPts.first().x, hPts.first().y)
                                        hPts.drop(1).forEach { lineTo(it.x, it.y) }
                                        close()
                                    }
                                }
                            }
                            drawPath(path, color = SeerahHistoricalBrown.copy(alpha = 0.25f))
                            drawPath(path, color = SeerahMountainDark, style = Stroke(2f, pathEffect = dash))
                        }
                    }
                    "LineString", "MultiLineString" -> {
                        val lineColor = when (feat.layerKey) {
                            "valleys" -> SeerahMutedBlue
                            "hijra_routes" -> SeerahRouteGreen
                            "battles", "military_roads" -> SeerahRouteRed
                            else -> SeerahHistoricalBrown
                        }
                        val lines = if (feat.multiLines.isNotEmpty()) feat.multiLines else listOf(feat.coordinates)
                        lines.forEach { lineCoords ->
                            val screenPts = lineCoords.map { projectLatLon(it.latitude, it.longitude, size) }
                            if (screenPts.size >= 2) {
                                val path = Path().apply {
                                    moveTo(screenPts.first().x, screenPts.first().y)
                                    screenPts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                // هالة فاتحة خلف المسار لوضوح ممتاز فوق أي خلفية
                                drawPath(path, color = SeerahIvory.copy(alpha = 0.72f), style = Stroke(7.5f, cap = StrokeCap.Round))
                                drawPath(path, color = lineColor, style = Stroke(4.2f, cap = StrokeCap.Round, pathEffect = dash))
                                val p1 = screenPts[screenPts.lastIndex - 1]
                                val p2 = screenPts.last()
                                if (hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()) > 12.0) {
                                    val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                                    val aLen = 14f
                                    drawLine(lineColor, p2, Offset(p2.x - (aLen * cos(angle - 0.5)).toFloat(), p2.y - (aLen * sin(angle - 0.5)).toFloat()), 3.5f)
                                    drawLine(lineColor, p2, Offset(p2.x - (aLen * cos(angle + 0.5)).toFloat(), p2.y - (aLen * sin(angle + 0.5)).toFloat()), 3.5f)
                                }
                            }
                        }
                    }
                    "Point", "MultiPoint" -> {
                        feat.coordinates.forEach { c ->
                            val pt = projectLatLon(c.latitude, c.longitude, size)
                            drawCircle(SeerahIvory, radius = 9.5f, center = pt)
                            drawCircle(SeerahAntiqueGold, radius = 7f, center = pt)
                        }
                    }
                }
            }

            // 3. خط قياس المسافة المستقيمة بين آخر موقعين مختارين
            if (lastTwoSelected.size == 2) {
                val s1 = projectLatLon(lastTwoSelected[0].coordinates.first().latitude, lastTwoSelected[0].coordinates.first().longitude, size)
                val s2 = projectLatLon(lastTwoSelected[1].coordinates.first().latitude, lastTwoSelected[1].coordinates.first().longitude, size)
                drawLine(SeerahRouteGreen, s1, s2, strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
            }

            // 4. رسم المواقع مع توزيع ذكي في 4 اتجاهات لمنع تداخل أو اختفاء الأسماء القريبة
            val placedLabelRects = mutableListOf<RectF>()
            geoPoints.forEach { feat ->
                val coord = feat.coordinates.first()
                val pt = projectLatLon(coord.latitude, coord.longitude, size)
                if (pt.x < -30f || pt.x > size.width + 30f || pt.y < -30f || pt.y > size.height + 30f) return@forEach

                val isDisputed = feat.certainty != GeographicCertainty.CONFIRMED
                val pinColor = when {
                    feat.featureType in setOf("badr", "uhud", "khandaq", "khaybar", "hunayn_taif", "tabuk") -> SeerahRouteRed
                    isDisputed -> SeerahAntiqueGold
                    else -> SeerahEmerald
                }
                if (isDisputed) drawCircle(pinColor.copy(alpha = 0.24f), radius = 17f, center = pt)
                drawCircle(SeerahIvory, radius = 10f, center = pt)
                drawCircle(pinColor, radius = 7.2f, center = pt)

                val textW = labelPaint.measureText(feat.label) + 22f
                val boxH = 32f
                val candidates = listOf(
                    // أعلى النقطة
                    RectF(pt.x - textW / 2f, pt.y - 16f - boxH, pt.x + textW / 2f, pt.y - 16f),
                    // أسفل النقطة
                    RectF(pt.x - textW / 2f, pt.y + 16f, pt.x + textW / 2f, pt.y + 16f + boxH),
                    // يمين النقطة
                    RectF(pt.x + 16f, pt.y - boxH / 2f, pt.x + 16f + textW, pt.y + boxH / 2f),
                    // يسار النقطة
                    RectF(pt.x - 16f - textW, pt.y - boxH / 2f, pt.x - 16f, pt.y + boxH / 2f)
                )
                val chosen = candidates.firstOrNull { rect ->
                    rect.left >= 6f && rect.right <= size.width - 6f &&
                        rect.top >= 26f && rect.bottom <= size.height - 34f &&
                        placedLabelRects.none { RectF.intersects(it, rect) }
                }
                if (chosen != null) {
                    placedLabelRects.add(chosen)
                    // خط ربط لطيف إذا كانت التسمية على الجانب أو الأسفل
                    drawLine(SeerahAntiqueGold.copy(alpha = 0.6f), pt, Offset(chosen.centerX(), chosen.centerY()), 1.4f)
                    drawContext.canvas.nativeCanvas.drawRoundRect(chosen, 10f, 10f, labelBgPaint)
                    drawContext.canvas.nativeCanvas.drawRoundRect(chosen, 10f, 10f, labelBorderPaint)
                    drawContext.canvas.nativeCanvas.drawText(feat.label, chosen.centerX(), chosen.top + 22f, labelPaint)
                }
            }

            // 5. بوصلة الشمال (Compass Rose) في أسفل اليمين
            val compassCenter = Offset(size.width - 36f, size.height - 36f)
            drawCircle(SeerahIvory.copy(alpha = 0.90f), radius = 20f, center = compassCenter)
            drawCircle(SeerahAntiqueGold, radius = 20f, center = compassCenter, style = Stroke(1.8f))
            drawLine(SeerahRouteRed, compassCenter, Offset(compassCenter.x, compassCenter.y - 14f), 3f, cap = StrokeCap.Round)
            drawLine(SeerahInkDark.copy(alpha = 0.5f), compassCenter, Offset(compassCenter.x, compassCenter.y + 12f), 2f, cap = StrokeCap.Round)
            drawContext.canvas.nativeCanvas.drawText("ش", compassCenter.x, compassCenter.y - 22f, terrainLabelPaint)

            // 6. مقياس الرسم الديناميكي عند خط عرض المركز
            val centerLat = ((minLat + maxLat) / 2.0).coerceIn(-80.0, 80.0)
            val widthKm = GeoJsonParser.haversineKm(GeoPointModel(centerLat, minLon), GeoPointModel(centerLat, maxLon))
            val kmPerPx = (widthKm / (size.width * zoom)).coerceAtLeast(0.001)
            val barPx = 104f
            val barKm = barPx * kmPerPx
            val scaleLabel = if (barKm >= 1.0) "≈ ${"%.1f".format(barKm)} كم" else "≈ ${(barKm * 1000).toInt()} م"
            val by = size.height - 18f
            drawLine(SeerahInkDark, Offset(18f, by), Offset(18f + barPx, by), 3f)
            drawLine(SeerahInkDark, Offset(18f, by - 5f), Offset(18f, by + 5f), 2f)
            drawLine(SeerahInkDark, Offset(18f + barPx, by - 5f), Offset(18f + barPx, by + 5f), 2f)
            drawContext.canvas.nativeCanvas.drawText("$scaleLabel (${"%.1f".format(centerLat)}°N)", 18f, by - 8f, gridTextPaint)
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SeerahDeepGreen.copy(alpha = 0.90f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${mapDefinition.arabicTitle} • ${if (mbTilesReader != null) "MBTiles" else "أطلس أوفلاين"} • ${"%.1f".format(zoom)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeerahGoldLight
                )
            }
            if (lastTwoSelected.size == 2) {
                val distKm = GeoJsonParser.haversineKm(lastTwoSelected[0].coordinates.first(), lastTwoSelected[1].coordinates.first())
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SeerahIvory.copy(alpha = 0.94f))
                        .border(1.dp, SeerahAntiqueGold, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "المسافة المستقيمة: ${"%.1f".format(distKm)} كم",
                        style = MaterialTheme.typography.labelSmall,
                        color = SeerahDeepGreen
                    )
                }
            }
        }
    }
}
