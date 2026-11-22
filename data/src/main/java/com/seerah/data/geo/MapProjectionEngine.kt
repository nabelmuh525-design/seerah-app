package com.seerah.data.geo

import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import kotlin.math.*

data class ScreenPt(val x: Float, val y: Float)

/**
 * محرك الإسقاط الهندسي واختبار النقر الحقيقي (Pure Kotlin — قابل لاختبار الوحدة بالكامل):
 * - يُسقط الإحداثيات الجغرافية (WGS84 -> Web Mercator -> Screen) بتحويل واحد موحد للبلاطات والمعالم.
 * - يدعم اختبار النقر على جميع نقاط MultiPoint، وجميع مقاطع LineString/MultiLineString،
 *   ومساحات Polygon/MultiPolygon مع احترام الثقوب الداخلية (Ray-Casting).
 */
class MapProjectionEngine(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val widthPx: Float,
    val heightPx: Float,
    val zoom: Float,
    val panX: Float,
    val panY: Float
) {
    private fun mercatorY(lat: Double): Double {
        val rad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        return ln(tan(Math.PI / 4.0 + rad / 2.0))
    }

    private val yMinMerc = mercatorY(minLat)
    private val yMaxMerc = mercatorY(maxLat)

    fun project(lat: Double, lon: Double): ScreenPt {
        val nx = ((lon - minLon) / (maxLon - minLon).coerceAtLeast(0.0001)).toFloat()
        val ny = (1.0 - (mercatorY(lat) - yMinMerc) / (yMaxMerc - yMinMerc).coerceAtLeast(0.0001)).toFloat()
        val bx = nx * widthPx
        val by = ny * heightPx
        val cx = widthPx / 2f
        val cy = heightPx / 2f
        return ScreenPt(
            x = cx + (bx - cx) * zoom + panX,
            y = cy + (by - cy) * zoom + panY
        )
    }

    fun unproject(screenX: Float, screenY: Float): GeoPointModel {
        val cx = widthPx / 2f
        val cy = heightPx / 2f
        val safeZoom = zoom.coerceAtLeast(0.01f)
        val bx = cx + (screenX - cx - panX) / safeZoom
        val by = cy + (screenY - cy - panY) / safeZoom
        val nx = (bx / widthPx.coerceAtLeast(1f)).toDouble()
        val ny = (by / heightPx.coerceAtLeast(1f)).toDouble()
        val lon = (minLon + nx * (maxLon - minLon)).coerceIn(-180.0, 180.0)
        val mercY = yMinMerc + (1.0 - ny) * (yMaxMerc - yMinMerc)
        val lat = Math.toDegrees(2.0 * atan(exp(mercY)) - Math.PI / 2.0).coerceIn(-85.0511, 85.0511)
        return GeoPointModel(latitude = lat, longitude = lon)
    }

    fun visibleGeoBounds(): TileGeoBounds {
        val tl = unproject(0f, 0f)
        val br = unproject(widthPx, heightPx)
        return TileGeoBounds(
            northLat = maxOf(tl.latitude, br.latitude),
            southLat = minOf(tl.latitude, br.latitude),
            westLon = minOf(tl.longitude, br.longitude),
            eastLon = maxOf(tl.longitude, br.longitude)
        )
    }

    fun hitTest(tapX: Float, tapY: Float, feature: GeoFeatureModel, tolerancePx: Float = 28f): Boolean {
        return when (feature.geometryType) {
            "Point", "MultiPoint" -> feature.coordinates.any { pt ->
                val s = project(pt.latitude, pt.longitude)
                hypot(tapX - s.x, tapY - s.y) <= tolerancePx
            }
            "LineString", "MultiLineString" -> {
                val lines = if (feature.multiLines.isNotEmpty()) feature.multiLines else listOf(feature.coordinates)
                lines.any { line ->
                    val pts = line.map { project(it.latitude, it.longitude) }
                    (0 until pts.size - 1).any { i ->
                        distanceToSegment(tapX, tapY, pts[i], pts[i + 1]) <= tolerancePx
                    }
                }
            }
            "Polygon", "MultiPolygon" -> feature.polygons.any { poly ->
                val outer = poly.outerRing.map { project(it.latitude, it.longitude) }
                val insideOuter = isPointInRing(tapX, tapY, outer)
                val insideAnyHole = poly.holes.any { hole ->
                    isPointInRing(tapX, tapY, hole.map { project(it.latitude, it.longitude) })
                }
                insideOuter && !insideAnyHole
            }
            else -> false
        }
    }

    private fun distanceToSegment(px: Float, py: Float, a: ScreenPt, b: ScreenPt): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return hypot(px - a.x, py - a.y)
        val t = (((px - a.x) * dx + (py - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot(px - projX, py - projY)
    }

    private fun isPointInRing(px: Float, py: Float, ring: List<ScreenPt>): Boolean {
        if (ring.size < 3) return false
        var inside = false
        var j = ring.lastIndex
        for (i in ring.indices) {
            val xi = ring[i].x
            val yi = ring[i].y
            val xj = ring[j].x
            val yj = ring[j].y
            val intersect = ((yi > py) != (yj > py)) &&
                (px < (xj - xi) * (py - yi) / ((yj - yi).takeIf { it != 0f } ?: 0.0001f) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
