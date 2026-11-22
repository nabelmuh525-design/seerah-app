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
}
