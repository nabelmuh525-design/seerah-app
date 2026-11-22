package com.seerah.data.geo

import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import com.seerah.domain.model.GeographicCertainty
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

/**
 * محلل GeoJSON متوافق مع معيار RFC 7946 ([longitude, latitude])
 * مع التحقق من صحة الإحداثيات وحساب المسافات الجغرافية الحقيقية (Haversine).
 */
object GeoJsonParser {

    fun isValidCoordinate(lat: Double, lon: Double): Boolean =
        !lat.isNaN() && !lon.isNaN() && lat in -90.0..90.0 && lon in -180.0..180.0

    fun haversineKm(a: GeoPointModel, b: GeoPointModel): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = sin(dLat / 2.0)
        val sinLon = sin(dLon / 2.0)
        val h = sinLat * sinLat + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinLon * sinLon
        return 2.0 * r * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun parseFeatureCollection(rawJson: String): List<GeoFeatureModel> {
        val result = mutableListOf<GeoFeatureModel>()
        val root = JSONObject(rawJson)
        val features = root.optJSONArray("features") ?: return emptyList()

        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: JSONObject()
            val geometry = feature.optJSONObject("geometry") ?: continue
            val geomType = geometry.optString("type", "")
            val coordsArray = geometry.optJSONArray("coordinates") ?: continue

            val points = extractPoints(geomType, coordsArray)
            if (points.isEmpty()) continue

            result.add(
                GeoFeatureModel(
                    id = feature.optString("id", "feature_$i"),
                    layerKey = props.optString("layer", "historical_sites"),
                    geometryType = geomType,
                    label = props.optString("name", props.optString("label", "موقع غير مسمى")),
                    coordinates = points,
                    certainty = GeographicCertainty.fromCode(props.optString("certainty", "UNSPECIFIED")),
                    sourceCitation = props.optString("source", "غير مكتمل التوثيق — يحتاج مراجعة"),
                    phaseOrder = if (props.has("phase")) props.optInt("phase") else null,
                    featureType = props.optString("feature_type", "SITE")
                )
            )
        }
        return result
    }

    private fun extractPoints(type: String, coords: JSONArray): List<GeoPointModel> {
        return when (type) {
            "Point" -> {
                if (coords.length() >= 2) {
                    val lon = coords.optDouble(0)
                    val lat = coords.optDouble(1)
                    if (isValidCoordinate(lat, lon)) listOf(GeoPointModel(latitude = lat, longitude = lon))
                    else emptyList()
                } else emptyList()
            }
            "LineString" -> parsePointList(coords)
            "Polygon", "MultiLineString" -> {
                val ring = coords.optJSONArray(0) ?: return emptyList()
                parsePointList(ring)
            }
            else -> emptyList()
        }
    }

    private fun parsePointList(arr: JSONArray): List<GeoPointModel> {
        val list = mutableListOf<GeoPointModel>()
        for (i in 0 until arr.length()) {
            val pt = arr.optJSONArray(i) ?: continue
            if (pt.length() >= 2) {
                val lon = pt.optDouble(0)
                val lat = pt.optDouble(1)
                if (isValidCoordinate(lat, lon)) {
                    list.add(GeoPointModel(latitude = lat, longitude = lon))
                }
            }
        }
        return list
    }
}
