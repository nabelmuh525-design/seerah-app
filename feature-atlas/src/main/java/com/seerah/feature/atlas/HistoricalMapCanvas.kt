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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.seerah.core.theme.*
import com.seerah.data.geo.GeoJsonParser
import com.seerah.database.entity.BattleMapFeatureEntity
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import com.seerah.domain.model.GeographicCertainty
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * محرك رسم الخرائط التاريخية والعسكرية التفاعلي المحلي (Offline Cartographic Canvas Engine).
 *
 * المزايا الفعلية:
 * - إسقاط جغرافي حقيقي لإحداثيات GeoJSON (Point, LineString, Polygon) على لوحة الرسم.
 * - تحكم كامل بالتكبير والتصغير والتحريك (Pinch-to-Zoom & Pan) والتقاط اللمس (Hit-Testing) لكل معلم.
 * - أسلوب الأطلس العسكري التاريخي: ورق قديم (#F1E5CF)، تضاريس بنية متدرجة (#795735)،
 *   أودية زرقاء رمادية (#78999A)، مسارات حركة بأسهم اتجاهية (أخضر/أحمر)، وخط متقطع للمسارات المختلف فيها.
 * - أسماء جغرافية عربية واضحة فوق المعالم مع بوصلة ومقياس رسم وإطار أطلسي مزدوج.
 */
@Composable
fun HistoricalMapCanvas(
    mapDefinition: AtlasMapDefinition,
    enabledLayers: Set<String>,
    features: List<BattleMapFeatureEntity>,
    zoom: Float,
    panOffset: Offset,
    onTransform: (zoomDelta: Float, panDelta: Offset) -> Unit,
    onFeatureSelected: (GeoFeatureModel?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val parsedFeatures = remember(features) {
        features.flatMap { entity ->
            runCatching { GeoJsonParser.parseFeatureCollection(entity.geojsonGeometry) }
                .getOrDefault(emptyList())
        }.distinctBy { "${it.id}_${it.label}_${it.layerKey}" }
    }

    val visibleFeatures = remember(parsedFeatures, enabledLayers) {
        parsedFeatures.filter { enabledLayers.contains(it.layerKey) }
    }

    // حساب الحدود الجغرافية (Bounding Box) لإسقاط الإحداثيات الحقيقية بدقة
    val bounds = remember(visibleFeatures) {
        val allPts = visibleFeatures.flatMap { it.coordinates }
        if (allPts.isEmpty()) null
        else {
            val minLat = allPts.minOf { it.latitude }
            val maxLat = allPts.maxOf { it.latitude }
            val minLon = allPts.minOf { it.longitude }
            val maxLon = allPts.maxOf { it.longitude }
            val latPad = ((maxLat - minLat) * 0.18).coerceAtLeast(0.02)
            val lonPad = ((maxLon - minLon) * 0.18).coerceAtLeast(0.02)
            GeoBounds(minLat - latPad, maxLat + latPad, minLon - lonPad, maxLon + lonPad)
        }
    }

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#1C1915")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
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
            .pointerInput(visibleFeatures, bounds, zoom, panOffset, canvasSize) {
                detectTapGestures { tapOffset ->
                    if (bounds == null || canvasSize == Size.Zero) {
                        onFeatureSelected(null)
                        return@detectTapGestures
                    }
                    val hit = visibleFeatures.minByOrNull { feat ->
                        val anchor = feat.coordinates.firstOrNull() ?: return@minByOrNull Float.MAX_VALUE
                        val projected = projectToCanvas(anchor, bounds, canvasSize, zoom, panOffset)
                        hypot(tapOffset.x - projected.x, tapOffset.y - projected.y)
                    }
                    if (hit != null) {
                        val anchor = hit.coordinates.first()
                        val projected = projectToCanvas(anchor, bounds, canvasSize, zoom, panOffset)
                        val dist = hypot(tapOffset.x - projected.x, tapOffset.y - projected.y)
                        onFeatureSelected(if (dist <= 52f) hit else null)
                    } else {
                        onFeatureSelected(null)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            val w = size.width
            val h = size.height
            val cx = w / 2f + panOffset.x
            val cy = h / 2f + panOffset.y

            // 1. خلفية الورق القديم وتدرج المناطق الرملية
            drawRect(color = SeerahParchment)
            if (enabledLayers.contains("terrain")) {
                drawCircle(
                    color = SeerahSand.copy(alpha = 0.68f),
                    radius = (w.coerceAtLeast(h) * 0.48f) * zoom,
                    center = Offset(cx, cy)
                )
            }

            // شبكة خطوط الطول والعرض الأطلسية
            val gridStep = (72f * zoom).coerceIn(32f, 180f)
            var gx = (cx % gridStep)
            while (gx < w) {
                drawLine(SeerahAntiqueGold.copy(alpha = 0.16f), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = (cy % gridStep)
            while (gy < h) {
                drawLine(SeerahAntiqueGold.copy(alpha = 0.16f), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                gy += gridStep
            }

            // 2. رسم المعالم الموثقة وفق إسقاطها الجغرافي الحقيقي
            if (bounds != null) {
                visibleFeatures.forEach { feat ->
                    val screenPts = feat.coordinates.map { pt ->
                        projectToCanvas(pt, bounds, size, zoom, panOffset)
                    }
                    val isDisputed = feat.certainty == GeographicCertainty.DISPUTED ||
                        feat.certainty == GeographicCertainty.UNVERIFIED
                    val dashEffect = if (isDisputed) PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) else null

                    when (feat.geometryType) {
                        "Polygon" -> {
                            if (screenPts.size >= 3) {
                                val poly = Path().apply {
                                    moveTo(screenPts.first().x, screenPts.first().y)
                                    screenPts.drop(1).forEach { lineTo(it.x, it.y) }
                                    close()
                                }
                                val fillColor = if (feat.featureType.equals("MOUNTAIN", true)) {
                                    SeerahHistoricalBrown.copy(alpha = 0.38f)
                                } else {
                                    SeerahSand.copy(alpha = 0.55f)
                                }
                                drawPath(poly, color = fillColor)
                                drawPath(poly, color = SeerahMountainDark.copy(alpha = 0.7f), style = Stroke(2.2f, pathEffect = dashEffect))
                            }
                        }
                        "LineString" -> {
                            if (screenPts.size >= 2) {
                                val routeColor = when {
                                    feat.featureType.equals("VALLEY", true) -> SeerahMutedBlue
                                    feat.layerKey == "hijra_routes" -> SeerahRouteGreen
                                    feat.featureType.equals("OPPOSING_FORCE", true) -> SeerahRouteRed
                                    else -> SeerahRouteGreen
                                }
                                val path = Path().apply {
                                    moveTo(screenPts.first().x, screenPts.first().y)
                                    screenPts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = path,
                                    color = routeColor,
                                    style = Stroke(width = (4.5f * zoom).coerceIn(3f, 9f), cap = StrokeCap.Round, pathEffect = dashEffect)
                                )
                                // سهم اتجاه الحركة عند نهاية المسار
                                val p1 = screenPts[screenPts.lastIndex - 1]
                                val p2 = screenPts.last()
                                val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                                val arrowLen = (14f * zoom).coerceIn(10f, 22f)
                                val left = Offset(
                                    p2.x - (arrowLen * cos(angle - 0.5)).toFloat(),
                                    p2.y - (arrowLen * sin(angle - 0.5)).toFloat()
                                )
                                val right = Offset(
                                    p2.x - (arrowLen * cos(angle + 0.5)).toFloat(),
                                    p2.y - (arrowLen * sin(angle + 0.5)).toFloat()
                                )
                                drawLine(routeColor, p2, left, strokeWidth = 3.5f)
                                drawLine(routeColor, p2, right, strokeWidth = 3.5f)
                            }
                        }
                        else -> {
                            val pt = screenPts.first()
                            when (feat.featureType.uppercase()) {
                                "MOUNTAIN" -> {
                                    val r = (16f * zoom).coerceIn(12f, 28f)
                                    val tri = Path().apply {
                                        moveTo(pt.x, pt.y - r)
                                        lineTo(pt.x - r, pt.y + r * 0.75f)
                                        lineTo(pt.x + r, pt.y + r * 0.75f)
                                        close()
                                    }
                                    drawPath(tri, color = SeerahHistoricalBrown.copy(alpha = 0.85f))
                                    drawPath(tri, color = SeerahMountainDark, style = Stroke(2f))
                                }
                                "WELL" -> {
                                    val r = (8f * zoom).coerceIn(6f, 15f)
                                    drawCircle(SeerahMutedBlue, radius = r, center = pt)
                                    drawCircle(SeerahDeepGreen, radius = r, center = pt, style = Stroke(2.2f))
                                }
                                "CAMP" -> {
                                    val r = (10f * zoom).coerceIn(8f, 18f)
                                    drawRect(SeerahEmerald, topLeft = Offset(pt.x - r, pt.y - r), size = Size(r * 2, r * 2))
                                    drawRect(SeerahAntiqueGold, topLeft = Offset(pt.x - r, pt.y - r), size = Size(r * 2, r * 2), style = Stroke(2f))
                                }
                                else -> {
                                    val r = (9f * zoom).coerceIn(7f, 16f)
                                    drawCircle(SeerahAntiqueGold, radius = r, center = pt)
                                    drawCircle(SeerahDeepGreen, radius = r * 0.42f, center = pt)
                                }
                            }
                        }
                    }

                    // كتابة الاسم الجغرافي العربي بوضوح فوق المعلم
                    val labelAnchor = screenPts.first()
                    drawContext.canvas.nativeCanvas.drawText(
                        feat.label,
                        labelAnchor.x,
                        labelAnchor.y - (16f * zoom).coerceIn(12f, 26f),
                        labelPaint
                    )
                }
            }

            // 3. إطار الأطلس التاريخي الداخلي الرفيع
            drawRect(
                color = SeerahHistoricalBrown.copy(alpha = 0.45f),
                topLeft = Offset(10f, 10f),
                size = Size(w - 20f, h - 20f),
                style = Stroke(1.5f)
            )

            // 4. وردة البوصلة التاريخية في الزاوية العلوية اليسرى
            val compassCenter = Offset(52f, 52f)
            drawCircle(SeerahInkDark.copy(alpha = 0.25f), radius = 24f, center = compassCenter, style = Stroke(1.5f))
            drawLine(SeerahRouteRed, Offset(52f, 28f), Offset(52f, 52f), strokeWidth = 3f)
            drawLine(SeerahHistoricalBrown, Offset(52f, 52f), Offset(52f, 76f), strokeWidth = 2f)
            drawLine(SeerahHistoricalBrown, Offset(30f, 52f), Offset(74f, 52f), strokeWidth = 1.5f)

            // 5. مقياس الرسم (Scale Bar) أسفل اليسار
            val barY = h - 26f
            drawLine(SeerahInkDark, Offset(24f, barY), Offset(114f, barY), strokeWidth = 3f)
            drawLine(SeerahInkDark, Offset(24f, barY - 6f), Offset(24f, barY + 6f), strokeWidth = 2f)
            drawLine(SeerahInkDark, Offset(114f, barY - 6f), Offset(114f, barY + 6f), strokeWidth = 2f)
        }

        // شارة عنوان الخريطة ومستوى التكبير
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SeerahDeepGreen.copy(alpha = 0.90f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${mapDefinition.arabicTitle}  •  ${"%.1f".format(zoom)}x",
                style = MaterialTheme.typography.labelLarge,
                color = SeerahParchment
            )
        }

        if (visibleFeatures.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SeerahIvory.copy(alpha = 0.95f))
                    .border(1.dp, SeerahAntiqueGold, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "بانتظار استيراد حزمة GeoJSON أو MBTiles الموثقة لخريطة «${mapDefinition.arabicTitle}». لا يتم رسم معالم أو إحداثيات بالتخمين.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeerahInkDark
                )
            }
        }
    }
}

private data class GeoBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

private fun projectToCanvas(
    pt: GeoPointModel,
    bounds: GeoBounds,
    canvasSize: Size,
    zoom: Float,
    panOffset: Offset
): Offset {
    val lonSpan = (bounds.maxLon - bounds.minLon).coerceAtLeast(0.0001)
    val latSpan = (bounds.maxLat - bounds.minLat).coerceAtLeast(0.0001)
    val normX = ((pt.longitude - bounds.minLon) / lonSpan).toFloat()
    val normY = (1.0 - (pt.latitude - bounds.minLat) / latSpan).toFloat()

    val baseX = normX * canvasSize.width
    val baseY = normY * canvasSize.height
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f

    return Offset(
        x = cx + (baseX - cx) * zoom + panOffset.x,
        y = cy + (baseY - cy) * zoom + panOffset.y
    )
}
