package com.seerah.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "people",
    indices = [Index(value = ["category"]), Index(value = ["normalized_name"])]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stable_id", defaultValue = "") val stableId: String = "",
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    val kunyah: String = "",
    val lineage: String = "",
    val category: String, // MOTHERS_OF_BELIEVERS, AHL_AL_BAYT, RIGHTLY_GUIDED_CALIPHS, MUHAJIRUN, ANSAR, FEMALE_COMPANIONS, COMMANDERS, NARRATORS, OTHER
    @ColumnInfo(name = "historical_bio") val historicalBio: String = "",
    @ColumnInfo(name = "role_in_seerah") val roleInSeerah: String = "",
    @ColumnInfo(name = "primary_sources") val primarySources: String = ""
)

@Entity(
    tableName = "person_events",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["person_id"]), Index(value = ["event_id"])]
)
data class PersonEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "person_id") val personId: Long,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "role_description") val roleDescription: String = "",
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(
    tableName = "places",
    indices = [Index(value = ["normalized_name"]), Index(value = ["map_id"])]
)
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stable_id", defaultValue = "") val stableId: String = "",
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "historical_name") val historicalName: String = "",
    val description: String = "",
    @ColumnInfo(name = "historical_significance") val historicalSignificance: String = "",
    @ColumnInfo(name = "certainty_level") val certaintyLevel: String = "UNVERIFIED", // CONFIRMED, PROBABLE, DISPUTED, UNVERIFIED
    @ColumnInfo(name = "map_id") val mapId: String = "arabia"
)

@Entity(
    tableName = "place_coordinates",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["place_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["place_id"])]
)
data class PlaceCoordinateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "place_id") val placeId: Long,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "elevation_meters") val elevationMeters: Int? = null,
    @ColumnInfo(name = "accuracy_note") val accuracyNote: String = "",
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(
    tableName = "place_sources",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["place_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["place_id"]), Index(value = ["source_id"])]
)
data class PlaceSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "place_id") val placeId: Long,
    @ColumnInfo(name = "source_id") val sourceId: Long,
    val volume: String = "",
    val page: String = "",
    val note: String = ""
)

@Entity(
    tableName = "geographic_layers",
    indices = [Index(value = ["layer_key"], unique = true)]
)
data class GeographicLayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "layer_key") val layerKey: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "is_enabled_by_default") val isEnabledByDefault: Boolean = true,
    @ColumnInfo(name = "z_index") val zIndex: Int = 0,
    @ColumnInfo(name = "geojson_path") val geojsonPath: String = "",
    @ColumnInfo(name = "mbtiles_path") val mbtilesPath: String = ""
)

@Entity(
    tableName = "historical_routes",
    indices = [Index(value = ["map_id"]), Index(value = ["event_id"])]
)
data class HistoricalRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "map_id") val mapId: String,
    @ColumnInfo(name = "event_id") val eventId: Long? = null,
    val title: String,
    @ColumnInfo(name = "route_type") val routeType: String, // HIJRA, MUSLIM_ARMY, OPPOSING_FORCE, CARAVAN, EXPEDITION
    @ColumnInfo(name = "certainty_level") val certaintyLevel: String = "DISPUTED",
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = HistoricalRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["route_id", "sequence_order"])]
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "route_id") val routeId: Long,
    @ColumnInfo(name = "sequence_order") val sequenceOrder: Int,
    @ColumnInfo(name = "stage_name") val stageName: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    @ColumnInfo(name = "is_confirmed_in_sources") val isConfirmedInSources: Boolean = false,
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(
    tableName = "battle_phases",
    indices = [Index(value = ["battle_map_id", "phase_order"])]
)
data class BattlePhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "battle_map_id") val battleMapId: String,
    @ColumnInfo(name = "event_id") val eventId: Long? = null,
    @ColumnInfo(name = "phase_order") val phaseOrder: Int,
    val title: String,
    val description: String,
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(
    tableName = "battle_map_features",
    indices = [Index(value = ["battle_map_id"]), Index(value = ["phase_id"])]
)
data class BattleMapFeatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "battle_map_id") val battleMapId: String,
    @ColumnInfo(name = "phase_id") val phaseId: Long? = null,
    @ColumnInfo(name = "layer_key") val layerKey: String,
    @ColumnInfo(name = "feature_type") val featureType: String, // MOUNTAIN, VALLEY, WELL, CAMP, MOVEMENT_ARROW, SITE, TRENCH
    val label: String,
    @ColumnInfo(name = "geojson_geometry") val geojsonGeometry: String,
    @ColumnInfo(name = "certainty_level") val certaintyLevel: String = "UNVERIFIED",
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)
