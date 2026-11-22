package com.seerah.feature.atlas

import android.graphics.Paint
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.seerah.core.theme.*
import com.seerah.data.geo.GeoJsonParser
import com.seerah.database.entity.BattleMapFeatureEntity
import com.seerah.database.entity.PlaceCoordinateEntity
import com.seerah.database.entity.PlaceEntity
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import com.seerah.domain.model.GeographicCertainty
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.tan

/**
 * محرك الأطلس الجغرافي التاريخي الحقيقي (WGS84 / Web-Mercator GIS Canvas v3.0):
 * - يُسقط الإحداثيات الجغرافية الحقيقية (latitude, longitude) من قاعدة بيانات Room
 *   وملفات GeoJSON (RFC 7946) بدلاً من الإحداثيات النسبية لصور ثابتة.
 * - يعرض شبكة خطوط الطول والعرض الحقيقية بدرجاتها، ومقياس رسم بالكيلومتر،
 *   وأداة قياس المسافة الجغرافية (Haversine km) بين آخر موقعين تم اختيارهما.
 * - يميّز بصريًا بين المواقع المؤكدة والمختلف فيها دون رسم مسارات تخمينية.
 */
@Composable
fun HistoricalMapCanvas(
    mapDefinition: AtlasMapDefinition,
    enabledLayers: Set<String>,
    features: List<BattleMapFeatureEntity>,
    places: List<PlaceEntity> = emptyList(),
    coordinates: List<PlaceCoordinateEntity> = emptyList(),
    zoom: Float,
    panOffset: Offset,
    onTransform: (zoomDelta: Float, panDelta: Offset) -> Unit,
    onFeatureSelected: (GeoFeatureModel?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // بناء قائمة النقاط الجغرافية الحقيقية من Room (Places + Coordinates)
    val coordByPlace = remember(coordinates) { coordinates.associateBy { it.placeId } }
    val geoPoints = remember(places, coordByPlace, enabledLayers) {
        if (!enabledLayers.contains("historical_sites") && !enabledLayers.contains("cities_villages")) {
            emptyList()
        } else {
            places.mapNotNull { place ->
                val c = coordByPlace[place.id] ?: return@mapNotNull null
                if (!GeoJsonParser.isValidCoordinate(c.latitude, c.longitude)) return@mapNotNull null
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

    // نطاق الإحداثيات الجغرافية (الحجاز والجزيرة العربية: 19°N..32°N, 34°E..43°E)
    val minLat = 20.0
    val maxLat = 32.0
    val minLon = 34.5
    val maxLon = 42.5

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var lastTwoSelected by remember { mutableStateOf<List<GeoFeatureModel>>(emptyList()) }

    fun mercatorY(lat: Double): Double {
        val rad = Math.toRadians(lat.coerceIn(-85.0, 85.0))
        return ln(tan(Math.PI / 4.0 + rad / 2.0))
    }

    val yMinMerc = mercatorY(minLat)
    val yMaxMerc = mercatorY(maxLat)

    fun projectLatLon(lat: Double, lon: Double, size: Size): Offset {
        val nx = ((lon - minLon) / (maxLon - minLon)).toFloat()
        val ny = (1.0 - (mercatorY(lat) - yMinMerc) / (yMaxMerc - yMinMerc)).toFloat()
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
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val gridTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#795735")
            textSize = 20f
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
            .pointerInput(geoPoints, zoom, panOffset, canvasSize) {
                detectTapGestures { tap ->
                    if (canvasSize == Size.Zero) return@detectTapGestures
                    val hit = geoPoints.minByOrNull { f ->
                        val pt = f.coordinates.first()
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

            // 1. شبكة خطوط الطول والعرض الحقيقية (WGS84 Graticule)
            for (lat in 20..32 step 2) {
                val y = projectLatLon(lat.toDouble(), minLon, size).y
                if (y in 0f..size.height) {
                    drawLine(SeerahAntiqueGold.copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y), 1.2f)
                    drawContext.canvas.nativeCanvas.drawText("${lat}°N", 10f, y - 4f, gridTextPaint)
                }
            }
            for (lon in 35..42 step 2) {
                val x = projectLatLon(minLat, lon.toDouble(), size).x
                if (x in 0f..size.width) {
                    drawLine(SeerahAntiqueGold.copy(alpha = 0.28f), Offset(x, 0f), Offset(x, size.height), 1.2f)
                    drawContext.canvas.nativeCanvas.drawText("${lon}°E", x + 4f, 24f, gridTextPaint)
                }
            }

            // 2. خط قياس المسافة الجغرافية الحقيقية (Haversine) بين آخر موقعين مختارين
            if (lastTwoSelected.size == 2) {
                val p1 = lastTwoSelected[0].coordinates.first()
                val p2 = lastTwoSelected[1].coordinates.first()
                val s1 = projectLatLon(p1.latitude, p1.longitude, size)
                val s2 = projectLatLon(p2.latitude, p2.longitude, size)
                drawLine(
                    color = SeerahRouteGreen,
                    start = s1,
                    end = s2,
                    strokeWidth = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // 3. رسم المواقع التاريخية بإحداثياتها الجغرافية الحقيقية
            geoPoints.forEach { feat ->
                val coord = feat.coordinates.first()
                val pt = projectLatLon(coord.latitude, coord.longitude, size)
                val isDisputed = feat.certainty == GeographicCertainty.DISPUTED ||
                    feat.certainty == GeographicCertainty.UNSPECIFIED ||
                    feat.certainty == GeographicCertainty.APPROXIMATE

                val pinColor = when {
                    isDisputed -> SeerahAntiqueGold
                    feat.featureType in setOf("badr", "uhud", "khandaq", "khaybar", "hunayn_taif", "tabuk") -> SeerahRouteRed
                    else -> SeerahEmerald
                }

                if (isDisputed) {
                    drawCircle(
                        color = SeerahAntiqueGold.copy(alpha = 0.28f),
                        radius = 20f,
                        center = pt
                    )
                }
                drawCircle(color = SeerahIvory, radius = 11f, center = pt)
                drawCircle(color = pinColor, radius = 8f, center = pt)

                val textW = labelPaint.measureText(feat.label) + 22f
                val boxH = 34f
                val left = pt.x - textW / 2f
                val top = pt.y - 12f - boxH - 4f
                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + textW, top + boxH, 10f, 10f, labelBgPaint)
                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + textW, top + boxH, 10f, 10f, labelBorderPaint)
                drawContext.canvas.nativeCanvas.drawText(feat.label, pt.x, top + 24f, labelPaint)
            }

            // 4. مقياس الرسم الجغرافي الحقيقي (كم)
            val kmPerPx = (GeoJsonParser.haversineKm(GeoPointModel(24.0, minLon), GeoPointModel(24.0, maxLon)) / (size.width * zoom)).coerceAtLeast(0.01)
            val barPx = 110f
            val barKm = (barPx * kmPerPx).toInt().coerceAtLeast(1)
            val by = size.height - 20f
            drawLine(SeerahInkDark, Offset(18f, by), Offset(18f + barPx, by), 3f)
            drawContext.canvas.nativeCanvas.drawText("≈ $barKm كم (WGS84)", 18f, by - 8f, gridTextPaint)
        }

        // شارة العنوان وقياس المسافة الحقيقية
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
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
