package com.seerah.data.geo

import com.seerah.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

/**
 * محلل GeoJSON متوافق بالكامل مع معيار RFC 7946:
 * - يدعم جميع الأنواع السبعة: Point, MultiPoint, LineString, MultiLineString, Polygon (مع الثقوب وإغلاق الحلقات), MultiPolygon, GeometryCollection.
 * - يحافظ على ترتيب [longitude, latitude] ويتحقق من صحة كل إحداثية.
 * - لا يتجاهل العناصر غير الصالحة بصمت، بل يعيدها في GeoJsonParseReport.rejected.
 */
object GeoJsonParser {

    fun isValidCoordinate(lat: Double, lon: Double): Boolean =
        !lat.isNaN() && !lon.isNaN() && lat in -90.0..90.0 && lon in -180.0..180.0

    fun isClosedRing(ring: List<GeoPointModel>): Boolean {
        if (ring.size < 4) return false
        val first = ring.first()
        val last = ring.last()
        return first.latitude == last.latitude && first.longitude == last.longitude
    }

    fun haversineKm(a: GeoPointModel, b: GeoPointModel): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = sin(dLat / 2.0)
        val sinLon = sin(dLon / 2.0)
        val h = sinLat * sinLat + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinLon * sinLon
        return 2.0 * r * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun parseFeatureCollection(rawJson: String): List<GeoFeatureModel> =
        parseFeatureCollectionWithReport(rawJson).features

    fun parseFeatureCollectionWithReport(rawJson: String): GeoJsonParseReport {
        val accepted = mutableListOf<GeoFeatureModel>()
        val rejected = mutableListOf<GeoJsonRejection>()

        val root = try {
            JSONObject(rawJson)
        } catch (e: Exception) {
            return GeoJsonParseReport(emptyList(), listOf(GeoJsonRejection("root", "ملف JSON تالف: ${e.message}")))
        }

        if (root.optString("type") != "FeatureCollection") {
            return GeoJsonParseReport(emptyList(), listOf(GeoJsonRejection("root", "نوع GeoJSON غير مدعوم في الجذر (المطلوب FeatureCollection)")))
        }

        val features = root.optJSONArray("features")
            ?: return GeoJsonParseReport(emptyList(), listOf(GeoJsonRejection("root", "مصفوفة features غير موجودة")))

        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i)
            val fid = feature?.optString("id", "feature_$i")?.ifBlank { "feature_$i" } ?: "feature_$i"
            if (feature == null) {
                rejected.add(GeoJsonRejection(fid, "عنصر feature فارغ أو غير صالح"))
                continue
            }
            val props = feature.optJSONObject("properties") ?: JSONObject()
            val geometry = feature.optJSONObject("geometry")
            if (geometry == null) {
                rejected.add(GeoJsonRejection(fid, "حقل geometry مفقود"))
                continue
            }
            parseGeometry(fid, geometry, props, accepted, rejected)
        }
        return GeoJsonParseReport(accepted, rejected)
    }

    private fun parseGeometry(
        fid: String,
        geometry: JSONObject,
        props: JSONObject,
        accepted: MutableList<GeoFeatureModel>,
        rejected: MutableList<GeoJsonRejection>
    ) {
        val geomType = geometry.optString("type", "")
        val layerKey = props.optString("layer", "historical_sites")
        val label = props.optString("name", props.optString("label", "موقع غير مسمى"))
        val certainty = GeographicCertainty.fromCode(props.optString("certainty", "UNSPECIFIED"))
        val source = props.optString("source", "غير مكتمل التوثيق — يحتاج مراجعة")
        val phaseOrder = if (props.has("phase")) props.optInt("phase") else null
        val featureType = props.optString("feature_type", "SITE")

        when (geomType) {
            "Point" -> {
                val pt = parseSinglePoint(geometry.optJSONArray("coordinates"))
                if (pt == null) rejected.add(GeoJsonRejection(fid, "إحداثيات Point غير صالحة"))
                else accepted.add(GeoFeatureModel(fid, layerKey, "Point", label, listOf(pt), certainty = certainty, sourceCitation = source, phaseOrder = phaseOrder, featureType = featureType))
            }
            "MultiPoint" -> {
                val pts = parsePointArray(geometry.optJSONArray("coordinates"))
                if (pts == null || pts.isEmpty()) rejected.add(GeoJsonRejection(fid, "إحداثيات MultiPoint غير صالحة"))
                else accepted.add(GeoFeatureModel(fid, layerKey, "MultiPoint", label, pts, certainty = certainty, sourceCitation = source, phaseOrder = phaseOrder, featureType = featureType))
            }
            "LineString" -> {
                val pts = parsePointArray(geometry.optJSONArray("coordinates"))
                if (pts == null || pts.size < 2) rejected.add(GeoJsonRejection(fid, "LineString يتطلب نقطتين صالحتين على الأقل"))
                else accepted.add(GeoFeatureModel(fid, layerKey, "LineString", label, pts, multiLines = listOf(pts), certainty = certainty, sourceCitation = source, phaseOrder = phaseOrder, featureType = featureType))
            }
            "MultiLineString" -> {
                val linesArr = geometry.optJSONArray("coordinates")
                if (linesArr == null || linesArr.length() == 0) {
                    rejected.add(GeoJsonRejection(fid, "MultiLineString فارغ"))
                    return
                }
                val lines = mutableListOf<List<GeoPointModel>>()
                for (i in 0 until linesArr.length()) {
                    val line = parsePointArray(linesArr.optJSONArray(i))
                    if (line == null || line.size < 2) {
                        rejected.add(GeoJsonRejection(fid, "خط فرعي غير صالح في MultiLineString عند الفهرس $i"))
                        return
                    }
                    lines.add(line)
                }
                accepted.add(GeoFeatureModel(fid, layerKey, "MultiLineString", label, lines.first(), multiLines = lines, certainty = certainty, sourceCitation = source, phaseOrder = phaseOrder, featureType = featureType))
            }
            "Polygon" -> {
                val poly = parsePolygon(geometry.optJSONArray("coordinates"))
                if (poly == null) rejected.add(GeoJsonRejection(fid, "Polygon غير صالح أو غير مغلق وفق RFC 7946"))
                else accepted.add(GeoFeatureModel(fid, layerKey, "Polygon", label, poly.outerRing, polygons = listOf(poly), certainty = certainty, sourceCitation = source, phaseOrder = phaseOrder, featureType = featureType))
            }
            "MultiPolygon" -> {
                val polysArr = geometry.optJSONArray("coordinates")
                if (polysArr == null || polysArr.length() == 0) {
                    rejected.add(GeoJsonRejection(fid, "MultiPolygon فارغ"))
                    return
                }
                val polys = mutableListOf<GeoPolygonRings>()
                for (i in 0 until polysArr.length()) {
                    val poly = parsePolygon(polysArr.optJSONArray(i))
                    if (poly == null) {
                        rejected.add(GeoJsonRejection(fid, "مضلع فرعي غير صالح أو غير مغلق في MultiPolygon عند الفهرس $i"))
                        return
                    }
                    polys.add(poly)
                }
                accepted.add(GeoFeatureModel(fid, layerKey, "MultiPolygon", label, polys.first().outerRing, polygons = polys, certainty = certainty, sourceCitation = source, phaseOrder = phaseOrder, featureType = featureType))
            }
            "GeometryCollection" -> {
                val geoms = geometry.optJSONArray("geometries")
                if (geoms == null || geoms.length() == 0) {
                    rejected.add(GeoJsonRejection(fid, "GeometryCollection فارغ"))
                    return
                }
                for (gIdx in 0 until geoms.length()) {
                    val sub = geoms.optJSONObject(gIdx)
                    if (sub == null) {
                        rejected.add(GeoJsonRejection("${fid}_g$gIdx", "عنصر فرعي فارغ في GeometryCollection"))
                    } else {
                        parseGeometry("${fid}_g$gIdx", sub, props, accepted, rejected)
                    }
                }
            }
            else -> rejected.add(GeoJsonRejection(fid, "نوع هندسة غير مدعوم: '$geomType'"))
        }
    }

    private fun parseSinglePoint(arr: JSONArray?): GeoPointModel? {
        if (arr == null || arr.length() < 2) return null
        val lon = arr.optDouble(0, Double.NaN)
        val lat = arr.optDouble(1, Double.NaN)
        return if (isValidCoordinate(lat, lon)) GeoPointModel(latitude = lat, longitude = lon) else null
    }

    private fun parsePointArray(arr: JSONArray?): List<GeoPointModel>? {
        if (arr == null) return null
        val pts = mutableListOf<GeoPointModel>()
        for (i in 0 until arr.length()) {
            val pt = parseSinglePoint(arr.optJSONArray(i)) ?: return null
            pts.add(pt)
        }
        return pts
    }

    private fun parsePolygon(ringsArr: JSONArray?): GeoPolygonRings? {
        if (ringsArr == null || ringsArr.length() == 0) return null
        val outer = parsePointArray(ringsArr.optJSONArray(0)) ?: return null
        if (!isClosedRing(outer)) return null
        val holes = mutableListOf<List<GeoPointModel>>()
        for (i in 1 until ringsArr.length()) {
            val hole = parsePointArray(ringsArr.optJSONArray(i)) ?: return null
            if (!isClosedRing(hole)) return null
            holes.add(hole)
        }
        return GeoPolygonRings(outerRing = outer, holes = holes)
    }
}
