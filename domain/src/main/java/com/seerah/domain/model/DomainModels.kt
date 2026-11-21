package com.seerah.domain.model

data class SearchResultItem(
    val id: Long,
    val contentType: SearchContentFilter,
    val title: String,
    val snippet: String,
    val sourceCitation: String,
    val stageCode: String? = null
)

data class ImportReport(
    val success: Boolean,
    val fileName: String,
    val format: String,
    val isDuplicate: Boolean = false,
    val isOcrFlagged: Boolean = false,
    val importedPagesCount: Int = 0,
    val importedRecordsCount: Int = 0,
    val message: String
)

data class GeoPointModel(
    val latitude: Double,
    val longitude: Double
)

data class GeoFeatureModel(
    val id: String,
    val layerKey: String,
    val geometryType: String, // Point, LineString, Polygon
    val label: String,
    val coordinates: List<GeoPointModel>,
    val certainty: GeographicCertainty,
    val sourceCitation: String,
    val phaseOrder: Int? = null,
    val featureType: String = "SITE"
)

data class UserPreferencesState(
    val darkMode: Boolean = false,
    val fontScale: Float = 1.0f,
    val onboardingCompleted: Boolean = false,
    val lastReadTitle: String = "",
    val lastReadRoute: String = ""
)

data class DatabaseHealthReport(
    val isHealthy: Boolean,
    val detail: String,
    val booksCount: Int,
    val eventsCount: Int
)
