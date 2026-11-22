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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.seerah.core.theme.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.seerah.data.geo.GeoJsonParser
import com.seerah.data.geo.MbTilesReader
import java.io.File
import com.seerah.database.entity.BattleMapFeatureEntity
import com.seerah.database.entity.PlaceCoordinateEntity
import com.seerah.database.entity.PlaceEntity
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import com.seerah.domain.model.GeographicCertainty
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.tan

/**
 * محرك الأطلس الجغرافي (v3.0.0-alpha.2):
 * - نطاق جغرافي مستقل (bbox) لكل خريطة من الخرائط الـ 14.
 * - تصفية المواقع بحسب نطاق الخريطة المختارة.
 * - رسم فعلي لجميع هندسات GeoJSON المستوردة (Point, LineString, Polygon, MultiLineString).
 * - منع تصادم وتراكم الأسماء العربية على الشاشة (Label Collision Avoidance).
 * - مقياس رسم ديناميكي محسوب عند خط العرض الفعلي لمركز العرض.
 */
@Composable
fun HistoricalMapCanvas(
    mapDefinition: AtlasMapDefinition,
    enabledLayers: Set<String>,
    features: List<BattleMapFeatureEntity>,
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

    // 1. تصفية المواقع بحسب النطاق الجغرافي للخريطة الحالية أو ارتباطها المباشر بـ mapId
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

    // 2. تحليل ورسم معالم GeoJSON المستوردة فعليًا (LineString, Polygon, MultiLineString, Point)
    val importedGeoFeatures = remember(features, enabledLayers) {
        features.flatMap { entity ->
            runCatching { GeoJsonParser.parseFeatureCollection(entity.geojsonGeometry) }
                .getOrDefault(emptyList())
        }.filter { enabledLayers.contains(it.layerKey) }
    }

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val mbTilesReader = remember(mbtilesLocalPath) {
        mbtilesLocalPath?.takeIf { it.isNotBlank() }?.let { path ->
            val reader = MbTilesReader(File(path))
            if (reader.open()) reader else null
        }
    }
    DisposableEffect(mbTilesReader) {
        onDispose { mbTilesReader?.close() }
    }
    var lastTwoSelected by remember { mutableStateOf<List<GeoFeatureModel>>(emptyList()) }

    fun mercatorY(lat: Double): Double {
        val rad = Math.toRadians(lat.coerceIn(-85.0, 85.0))
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
            textSize = 25f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val gridTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#795735")
            textSize = 19f
            textAlign = Paint.Align.LEFT
        }
    }
    val labelBgPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(235, 250, 247, 240)
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

    val allClickable = remember(geoPoints, importedGeoFeatures) { geoPoints + importedGeoFeatures }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SeerahParchment)
            .border(2.dp, SeerahAntiqueGold, RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onTransform(gestureZoom, pan)
                }
            }
            .pointerInput(allClickable, zoom, panOffset, canvasSize, mapDefinition) {
                detectTapGestures { tap ->
                    if (canvasSize == Size.Zero) return@detectTapGestures
                    val hit = allClickable.minByOrNull { f ->
                        val pt = f.coordinates.firstOrNull() ?: return@minByOrNull Float.MAX_VALUE
                        val s = projectLatLon(pt.latitude, pt.longitude, canvasSize)
                        hypot(tap.x - s.x, tap.y - s.y)
                    }
                    if (hit != null) {
                        val pt = hit.coordinates.first()
                        val s = projectLatLon(pt.latitude, pt.longitude, canvasSize)
                        if (hypot(tap.x - s.x, tap.y - s.y) <= 64f) {
                            onFeatureSelected(hit)
                            lastTwoSelected = (lastTwoSelected + hit).takeLast(2)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            drawRect(color = SeerahParchment)

            // 0. عرض بلاطات MBTiles الفعلية دون إنترنت عند توفر ملف .mbtiles محلي
            if (mbTilesReader != null) {
                val tileZoom = (6 + (zoom * 1.5f).toInt()).coerceIn(5, 13)
                val (xMin, yMin) = MbTilesReader.latLonToTileXY(maxLat, minLon, tileZoom)
                val (xMax, yMax) = MbTilesReader.latLonToTileXY(minLat, maxLon, tileZoom)
                val cols = (xMax - xMin + 1).coerceIn(1, 6)
                val rows = (yMax - yMin + 1).coerceIn(1, 6)
                val tileW = (size.width / cols).toInt().coerceAtLeast(1)
                val tileH = (size.height / rows).toInt().coerceAtLeast(1)
                for (tx in xMin..(xMin + cols - 1)) {
                    for (ty in yMin..(yMin + rows - 1)) {
                        val bmp = mbTilesReader.getTileBitmapCached(tileZoom, tx, ty)
                        if (bmp != null) {
                            drawImage(
                                image = bmp.asImageBitmap(),
                                dstOffset = IntOffset((tx - xMin) * tileW, (ty - yMin) * tileH),
                                dstSize = IntSize(tileW, tileH)
                            )
                        }
                    }
                }
            }

            // 1. شبكة إحداثيات WGS84 ديناميكية تناسب نطاق الخريطة
            val latStep = ((maxLat - minLat) / 5.0).coerceAtLeast(0.02)
            val lonStep = ((maxLon - minLon) / 5.0).coerceAtLeast(0.02)
            var latCur = minLat
            while (latCur <= maxLat + 0.0001) {
                val y = projectLatLon(latCur, minLon, size).y
                if (y in 0f..size.height) {
                    drawLine(SeerahAntiqueGold.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), 1.1f)
                    drawContext.canvas.nativeCanvas.drawText("${"%.2f".format(latCur)}°N", 8f, y - 4f, gridTextPaint)
                }
                latCur += latStep
            }
            var lonCur = minLon
            while (lonCur <= maxLon + 0.0001) {
                val x = projectLatLon(minLat, lonCur, size).x
                if (x in 0f..size.width) {
                    drawLine(SeerahAntiqueGold.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height), 1.1f)
                    drawContext.canvas.nativeCanvas.drawText("${"%.2f".format(lonCur)}°E", x + 4f, 22f, gridTextPaint)
                }
                lonCur += lonStep
            }

            // 2. رسم معالم GeoJSON المستوردة (Polygon, LineString, MultiLineString, Point)
            importedGeoFeatures.forEach { feat ->
                val screenPts = feat.coordinates.map { projectLatLon(it.latitude, it.longitude, size) }
                val isDisputed = feat.certainty != GeographicCertainty.CONFIRMED
                val dash = if (isDisputed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null

                when (feat.geometryType) {
                    "Polygon" -> if (screenPts.size >= 3) {
                        val poly = Path().apply {
                            moveTo(screenPts.first().x, screenPts.first().y)
                            screenPts.drop(1).forEach { lineTo(it.x, it.y) }
                            close()
                        }
                        drawPath(poly, color = SeerahHistoricalBrown.copy(alpha = 0.28f))
                        drawPath(poly, color = SeerahMountainDark, style = Stroke(2f, pathEffect = dash))
                    }
                    "LineString", "MultiLineString" -> if (screenPts.size >= 2) {
                        val lineColor = when (feat.layerKey) {
                            "valleys" -> SeerahMutedBlue
                            "hijra_routes" -> SeerahRouteGreen
                            "battles", "military_roads" -> SeerahRouteRed
                            else -> SeerahHistoricalBrown
                        }
                        val path = Path().apply {
                            moveTo(screenPts.first().x, screenPts.first().y)
                            screenPts.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, color = lineColor, style = Stroke(4.5f, cap = StrokeCap.Round, pathEffect = dash))
                        val p1 = screenPts[screenPts.lastIndex - 1]
                        val p2 = screenPts.last()
                        val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                        val aLen = 14f
                        drawLine(lineColor, p2, Offset(p2.x - (aLen * cos(angle - 0.5)).toFloat(), p2.y - (aLen * sin(angle - 0.5)).toFloat()), 3f)
                        drawLine(lineColor, p2, Offset(p2.x - (aLen * cos(angle + 0.5)).toFloat(), p2.y - (aLen * sin(angle + 0.5)).toFloat()), 3f)
                    }
                    else -> if (screenPts.isNotEmpty()) {
                        drawCircle(SeerahAntiqueGold, radius = 8f, center = screenPts.first())
                    }
                }
            }

            // 3. خط قياس المسافة المستقيمة بين آخر موقعين
            if (lastTwoSelected.size == 2) {
                val s1 = projectLatLon(lastTwoSelected[0].coordinates.first().latitude, lastTwoSelected[0].coordinates.first().longitude, size)
                val s2 = projectLatLon(lastTwoSelected[1].coordinates.first().latitude, lastTwoSelected[1].coordinates.first().longitude, size)
                drawLine(SeerahRouteGreen, s1, s2, strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
            }

            // 4. رسم المواقع مع منع تصادم الأسماء (Label Collision Avoidance)
            val placedLabelRects = mutableListOf<RectF>()
            geoPoints.forEach { feat ->
                val coord = feat.coordinates.first()
                val pt = projectLatLon(coord.latitude, coord.longitude, size)
                val isDisputed = feat.certainty != GeographicCertainty.CONFIRMED
                val pinColor = when {
                    isDisputed -> SeerahAntiqueGold
                    feat.featureType in setOf("badr", "uhud", "khandaq", "khaybar", "hunayn_taif", "tabuk") -> SeerahRouteRed
                    else -> SeerahEmerald
                }

                if (isDisputed) {
                    drawCircle(SeerahAntiqueGold.copy(alpha = 0.28f), radius = 18f, center = pt)
                }
                drawCircle(SeerahIvory, radius = 10f, center = pt)
                drawCircle(pinColor, radius = 7.5f, center = pt)

                val textW = labelPaint.measureText(feat.label) + 20f
                val boxH = 32f
                val candidateRect = RectF(
                    pt.x - textW / 2f,
                    pt.y - 12f - boxH - 4f,
                    pt.x + textW / 2f,
                    pt.y - 12f - 4f
                )
                val collides = placedLabelRects.any { RectF.intersects(it, candidateRect) }
                if (!collides) {
                    placedLabelRects.add(candidateRect)
                    drawContext.canvas.nativeCanvas.drawRoundRect(candidateRect, 10f, 10f, labelBgPaint)
                    drawContext.canvas.nativeCanvas.drawRoundRect(candidateRect, 10f, 10f, labelBorderPaint)
                    drawContext.canvas.nativeCanvas.drawText(feat.label, pt.x, candidateRect.top + 23f, labelPaint)
                }
            }

            // 5. مقياس الرسم الديناميكي عند خط عرض مركز الخريطة الفعلي
            val centerLat = ((minLat + maxLat) / 2.0).coerceIn(-80.0, 80.0)
            val widthKm = GeoJsonParser.haversineKm(GeoPointModel(centerLat, minLon), GeoPointModel(centerLat, maxLon))
            val kmPerPx = (widthKm / (size.width * zoom)).coerceAtLeast(0.001)
            val barPx = 110f
            val barKm = barPx * kmPerPx
            val scaleLabel = if (barKm >= 1.0) "≈ ${"%.1f".format(barKm)} كم" else "≈ ${(barKm * 1000).toInt()} م"
            val by = size.height - 20f
            drawLine(SeerahInkDark, Offset(18f, by), Offset(18f + barPx, by), 3f)
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
                    text = "${mapDefinition.arabicTitle} • WGS84 • ${"%.1f".format(zoom)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeerahGoldLight
                )
            }
            if (lastTwoSelected.size == 2) {
                val distKm = GeoJsonParser.haversineKm(
                    lastTwoSelected[0].coordinates.first(),
                    lastTwoSelected[1].coordinates.first()
                )
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
