package com.seerah.feature.atlas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.seerah.core.R as CoreR
import com.seerah.core.theme.*
import com.seerah.database.entity.BattleMapFeatureEntity
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import com.seerah.domain.model.GeographicCertainty
import kotlin.math.hypot

private data class BuiltInLandmark(
    val id: String,
    val name: String,
    val mapId: String,
    val normX: Float, // 0f..1f on art_map_hijaz_atlas
    val normY: Float,
    val type: String,
    val certainty: GeographicCertainty,
    val source: String
)

private val HIJAZ_LANDMARKS = listOf(
    BuiltInLandmark("madinah", "المدينة المنورة", "madinah", 0.49f, 0.30f, "CITY", GeographicCertainty.CONFIRMED, "صحيح البخاري؛ أخبار المدينة"),
    BuiltInLandmark("uhud", "جبل أحد", "uhud", 0.53f, 0.25f, "MOUNTAIN", GeographicCertainty.CONFIRMED, "صحيح البخاري (حديث 4083)"),
    BuiltInLandmark("quba", "قباء", "madinah", 0.50f, 0.35f, "SITE", GeographicCertainty.CONFIRMED, "صحيح البخاري (حديث 3906)"),
    BuiltInLandmark("badr", "بدر", "badr", 0.36f, 0.44f, "BATTLE", GeographicCertainty.CONFIRMED, "القرآن الكريم؛ صحيح البخاري"),
    BuiltInLandmark("hudaybiyyah", "الحديبية", "hudaybiyyah", 0.48f, 0.69f, "SITE", GeographicCertainty.CONFIRMED, "صحيح البخاري (كتاب الشروط)"),
    BuiltInLandmark("makkah", "مكة المكرمة", "makkah", 0.55f, 0.72f, "CITY", GeographicCertainty.CONFIRMED, "القرآن الكريم؛ أخبار مكة للأزرقي"),
    BuiltInLandmark("thawr", "غار ثور", "hijra", 0.58f, 0.77f, "MOUNTAIN", GeographicCertainty.CONFIRMED, "سورة التوبة 40؛ صحيح البخاري")
)

/**
 * محرك الأطلس التاريخي التفاعلي v2.0:
 * يجمع بين الخريطة التضاريسية الفنية المرسومة للحجاز والبحر الأحمر،
 * وبين طبقات Canvas تفاعلية حقيقية (مسار الهجرة المتقطع، دبابيس المعالم،
 * التكبير والتصغير والتحريك، والتقاط اللمس لإظهار بطاقة الموقع الموثقة).
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
    var boxSize by remember { mutableStateOf(Size.Zero) }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#0B2923")
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
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
            strokeWidth = 2.5f
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
            .pointerInput(zoom, panOffset, boxSize) {
                detectTapGestures { tap ->
                    if (boxSize == Size.Zero) return@detectTapGestures
                    val cx = boxSize.width / 2f
                    val cy = boxSize.height / 2f
                    val hit = HIJAZ_LANDMARKS.minByOrNull { lm ->
                        val sx = cx + (lm.normX * boxSize.width - cx) * zoom + panOffset.x
                        val sy = cy + (lm.normY * boxSize.height - cy) * zoom + panOffset.y
                        hypot(tap.x - sx, tap.y - sy)
                    }
                    if (hit != null) {
                        val sx = cx + (hit.normX * boxSize.width - cx) * zoom + panOffset.x
                        val sy = cy + (hit.normY * boxSize.height - cy) * zoom + panOffset.y
                        if (hypot(tap.x - sx, tap.y - sy) <= 68f) {
                            onFeatureSelected(
                                GeoFeatureModel(
                                    id = hit.id,
                                    layerKey = "historical_sites",
                                    geometryType = "Point",
                                    label = hit.name,
                                    coordinates = listOf(GeoPointModel(0.0, 0.0)),
                                    certainty = hit.certainty,
                                    sourceCitation = hit.source,
                                    featureType = hit.type
                                )
                            )
                        }
                    }
                }
            }
    ) {
        // 1. الخريطة التضاريسية الفنية المرسومة للحجاز والبحر الأحمر
        Image(
            painter = painterResource(id = CoreR.drawable.art_map_hijaz_atlas),
            contentDescription = "خريطة الحجاز التاريخية",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = panOffset.x,
                    translationY = panOffset.y
                )
        )

        // 2. طبقة المسارات والمعالم التفاعلية فوق الخريطة
        Canvas(modifier = Modifier.fillMaxSize()) {
            boxSize = size
            val cx = size.width / 2f
            val cy = size.height / 2f

            fun toScreen(nx: Float, ny: Float): Offset = Offset(
                x = cx + (nx * size.width - cx) * zoom + panOffset.x,
                y = cy + (ny * size.height - cy) * zoom + panOffset.y
            )

            // رسم مسار الهجرة النبوية (أخضر متقطع كما في التصور)
            if (enabledLayers.contains("hijra_routes")) {
                val hijraPath = Path().apply {
                    val p1 = toScreen(0.55f, 0.72f) // مكة
                    val p2 = toScreen(0.58f, 0.77f) // غار ثور
                    val p3 = toScreen(0.42f, 0.62f) // الطريق الساحلي
                    val p4 = toScreen(0.36f, 0.44f) // قرب بدر
                    val p5 = toScreen(0.50f, 0.35f) // قباء
                    val p6 = toScreen(0.49f, 0.30f) // المدينة
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    quadraticTo(p3.x - 24f, p3.y, p4.x, p4.y)
                    quadraticTo(p4.x + 20f, (p4.y + p5.y) / 2f, p5.x, p5.y)
                    lineTo(p6.x, p6.y)
                }
                drawPath(
                    path = hijraPath,
                    color = SeerahRouteGreen,
                    style = Stroke(
                        width = (5.5f * zoom).coerceIn(4f, 10f),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                    )
                )
            }

            // رسم دبابيس المعالم وأسمائها العربية داخل بطاقات عاجية أنيقة
            if (enabledLayers.contains("historical_sites") || enabledLayers.contains("battles")) {
                HIJAZ_LANDMARKS.forEach { lm ->
                    val pt = toScreen(lm.normX, lm.normY)
                    val pinColor = when (lm.type) {
                        "CITY" -> SeerahEmerald
                        "BATTLE" -> SeerahRouteRed
                        "MOUNTAIN" -> SeerahHistoricalBrown
                        else -> SeerahAntiqueGold
                    }
                    val radius = if (lm.type == "CITY") 11f else 8.5f
                    drawCircle(color = SeerahIvory, radius = radius + 3.5f, center = pt)
                    drawCircle(color = pinColor, radius = radius, center = pt)

                    // بطاقة الاسم العربي فوق الدبوس
                    val textW = labelPaint.measureText(lm.name) + 28f
                    val boxH = 42f
                    val left = pt.x - textW / 2f
                    val top = pt.y - radius - boxH - 10f
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        left, top, left + textW, top + boxH, 14f, 14f, labelBgPaint
                    )
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        left, top, left + textW, top + boxH, 14f, 14f, labelBorderPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        lm.name,
                        pt.x,
                        top + 30f,
                        labelPaint
                    )
                }
            }
        }

        // شارة العنوان العلوية
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SeerahDeepGreen.copy(alpha = 0.90f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${mapDefinition.arabicTitle}  •  ${"%.1f".format(zoom)}x",
                style = MaterialTheme.typography.labelLarge,
                color = SeerahGoldLight
            )
        }
    }
}
