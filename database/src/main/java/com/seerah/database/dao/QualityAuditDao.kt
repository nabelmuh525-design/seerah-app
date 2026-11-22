package com.seerah.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seerah.database.entity.*
import kotlinx.coroutines.flow.Flow

data class QualityAuditSnapshot(
    val totalEvents: Int,
    val eventsWithoutFormalSourceLinks: Int,
    val narrationsWithoutGradeRef: Int,
    val hadithsWithoutGrades: Int,
    val placesWithoutCoordinates: Int,
    val disputedPlaces: Int,
    val booksNeedingOcrReview: Int,
    val peopleTotal: Int
)

@Dao
interface QualityAuditDao {
    @Query("""
        SELECT
            (SELECT COUNT(*) FROM events) AS totalEvents,
            (SELECT COUNT(*) FROM events e WHERE NOT EXISTS (SELECT 1 FROM event_sources es WHERE es.event_id = e.id)) AS eventsWithoutFormalSourceLinks,
            (SELECT COUNT(*) FROM narrations WHERE grade_reference = '' OR grade_status IN ('INCOMPLETE_CITATION', 'UNDER_REVIEW', 'UNVERIFIED')) AS narrationsWithoutGradeRef,
            (SELECT COUNT(*) FROM hadiths h WHERE NOT EXISTS (SELECT 1 FROM hadith_grades hg WHERE hg.hadith_id = h.id)) AS hadithsWithoutGrades,
            (SELECT COUNT(*) FROM places p WHERE NOT EXISTS (SELECT 1 FROM place_coordinates pc WHERE pc.place_id = p.id)) AS placesWithoutCoordinates,
            (SELECT COUNT(*) FROM places WHERE certainty_level IN ('DISPUTED', 'UNSPECIFIED', 'UNVERIFIED')) AS disputedPlaces,
            (SELECT COUNT(*) FROM books WHERE is_ocr_extracted = 1 AND needs_review = 1) AS booksNeedingOcrReview,
            (SELECT COUNT(*) FROM people) AS peopleTotal
    """)
    suspend fun getQualityAuditSnapshot(): QualityAuditSnapshot

    @Query("UPDATE geographic_layers SET is_enabled_by_default = :enabled WHERE layer_key = :layerKey")
    suspend fun updateLayerEnabled(layerKey: String, enabled: Boolean)

    @Query("SELECT * FROM place_coordinates")
    fun observeAllCoordinates(): Flow<List<PlaceCoordinateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks ORDER BY created_at_epoch_ms DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity): Long

    @Query("SELECT * FROM content_versions WHERE pack_key = :packKey LIMIT 1")
    suspend fun getContentVersion(packKey: String): ContentVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContentVersion(version: ContentVersionEntity): Long

    @Query("INSERT OR IGNORE INTO geographic_layers (layer_key, display_name, is_enabled_by_default, z_index, geojson_path, mbtiles_path) VALUES (:key, :name, :enabled, :zIndex, '', '')")
    suspend fun insertLayerIfMissing(key: String, name: String, enabled: Boolean, zIndex: Int)

    @Query("DELETE FROM geographic_layers WHERE layer_key = :layerKey")
    suspend fun deleteLayerByKey(layerKey: String)

    @Query("UPDATE geographic_layers SET display_name = :name WHERE layer_key = :layerKey")
    suspend fun updateLayerDisplayName(layerKey: String, name: String)

    @Query("SELECT * FROM battle_map_features")
    fun observeAllBattleFeatures(): Flow<List<BattleMapFeatureEntity>>
}
