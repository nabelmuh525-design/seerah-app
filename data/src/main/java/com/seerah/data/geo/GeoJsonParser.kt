package com.seerah.data.geo

import com.seerah.domain.model.GeoFeatureModel
import com.seerah.domain.model.GeoPointModel
import com.seerah.domain.model.GeographicCertainty
import org.json.JSONArray
import org.json.JSONObject

/**
 * محلل ملفات GeoJSON المحلية للأطلس التاريخي وخرائط الغزوات.
 * يدعم Point و LineString و Polygon ويقرأ حقول التوثيق ودرجة اليقين الجغرافي.
 */
object GeoJsonParser {

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
                    certainty = GeographicCertainty.fromCode(props.optString("certainty", "UNVERIFIED")),
                    sourceCitation = props.optString("source", "غير موثق في الملف المستورد — يحتاج مراجعة"),
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
                    listOf(GeoPointModel(latitude = coords.optDouble(1), longitude = coords.optDouble(0)))
                } else emptyList()
            }
            "LineString" -> parsePointList(coords)
            "Polygon" -> {
                val outerRing = coords.optJSONArray(0) ?: return emptyList()
                parsePointList(outerRing)
            }
            else -> emptyList()
        }
    }

    private fun parsePointList(arr: JSONArray): List<GeoPointModel> {
        val list = mutableListOf<GeoPointModel>()
        for (i in 0 until arr.length()) {
            val pt = arr.optJSONArray(i) ?: continue
            if (pt.length() >= 2) {
                list.add(GeoPointModel(latitude = pt.optDouble(1), longitude = pt.optDouble(0)))
            }
        }
        return list
    }
}
