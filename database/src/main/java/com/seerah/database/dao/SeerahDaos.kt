package com.seerah.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.seerah.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceLibraryDao {
    @Query("SELECT * FROM sources ORDER BY is_primary_source DESC, title ASC")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM books ORDER BY is_imported DESC, is_primary_source DESC, title ASC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("SELECT * FROM books WHERE file_hash = :hash LIMIT 1")
    suspend fun findBookByHash(hash: String): BookEntity?

    @Query("SELECT * FROM source_pages WHERE book_id = :bookId ORDER BY volume_number ASC, page_number ASC")
    fun observeBookPages(bookId: Long): Flow<List<SourcePageEntity>>

    @Query("SELECT * FROM editions WHERE book_id = :bookId")
    fun observeEditions(bookId: Long): Flow<List<EditionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthor(author: AuthorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdition(edition: EditionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<SourcePageEntity>)

    @Query("DELETE FROM source_pages WHERE book_id = :bookId")
    suspend fun deletePagesForBook(bookId: Long)

    @Query("""
        SELECT sp.* FROM source_pages sp
        JOIN source_pages_fts ON sp.id = source_pages_fts.rowid
        WHERE source_pages_fts MATCH :ftsQuery
        LIMIT :limit
    """)
    suspend fun searchPagesFts(ftsQuery: String, limit: Int = 50): List<SourcePageEntity>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun countBooks(): Int
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY chronological_order ASC")
    fun observeAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE stage_code = :stageCode ORDER BY chronological_order ASC")
    fun observeEventsByStage(stageCode: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE event_type = :eventType ORDER BY chronological_order ASC")
    fun observeEventsByType(eventType: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?

    @Query("SELECT * FROM event_dates WHERE event_id = :eventId")
    fun observeEventDates(eventId: Long): Flow<List<EventDateEntity>>

    @Query("SELECT * FROM event_sources WHERE event_id = :eventId")
    fun observeEventSources(eventId: Long): Flow<List<EventSourceEntity>>

    @Query("SELECT * FROM narrations WHERE event_id = :eventId ORDER BY id ASC")
    fun observeNarrationsForEvent(eventId: Long): Flow<List<NarrationEntity>>

    @Query("SELECT * FROM narrations ORDER BY id DESC")
    fun observeAllNarrations(): Flow<List<NarrationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("SELECT * FROM events WHERE title = :title LIMIT 1")
    suspend fun findEventByTitle(title: String): EventEntity?

    @Query("SELECT * FROM events WHERE stable_id = :stableId LIMIT 1")
    suspend fun findEventByStableId(stableId: String): EventEntity?

    @Query("DELETE FROM event_dates WHERE event_id = :eventId")
    suspend fun deleteEventDates(eventId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventDate(date: EventDateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventSource(eventSource: EventSourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNarration(narration: NarrationEntity): Long

    @Query("""
        SELECT e.* FROM events e
        JOIN events_fts ON e.id = events_fts.rowid
        WHERE events_fts MATCH :ftsQuery
        LIMIT :limit
    """)
    suspend fun searchEventsFts(ftsQuery: String, limit: Int = 50): List<EventEntity>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun countEvents(): Int
}

@Dao
interface PeopleDao {
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun observeAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE category = :category ORDER BY name ASC")
    fun observePeopleByCategory(category: String): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :personId")
    suspend fun getPersonById(personId: Long): PersonEntity?

    @Query("SELECT * FROM person_events WHERE person_id = :personId")
    fun observePersonEvents(personId: Long): Flow<List<PersonEventEntity>>

    @Query("SELECT * FROM person_events WHERE event_id = :eventId")
    fun observeEventParticipants(eventId: Long): Flow<List<PersonEventEntity>>

    @Query("SELECT * FROM people WHERE normalized_name LIKE '%' || :normalizedQuery || '%' LIMIT :limit")
    suspend fun searchPeople(normalizedQuery: String, limit: Int = 40): List<PersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("SELECT * FROM people WHERE name = :name LIMIT 1")
    suspend fun findPersonByName(name: String): PersonEntity?

    @Query("SELECT * FROM people WHERE stable_id = :stableId LIMIT 1")
    suspend fun findPersonByStableId(stableId: String): PersonEntity?

    @Query("SELECT COUNT(*) FROM person_events WHERE person_id = :personId AND event_id = :eventId")
    suspend fun countPersonEvent(personId: Long, eventId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonEvent(link: PersonEventEntity): Long
}

@Dao
interface AtlasGeoDao {
    @Query("SELECT * FROM places ORDER BY name ASC")
    fun observeAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE map_id = :mapId ORDER BY name ASC")
    fun observePlacesForMap(mapId: String): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :placeId")
    suspend fun getPlaceById(placeId: Long): PlaceEntity?

    @Query("SELECT * FROM place_coordinates WHERE place_id = :placeId")
    fun observeCoordinatesForPlace(placeId: Long): Flow<List<PlaceCoordinateEntity>>

    @Query("SELECT * FROM place_sources WHERE place_id = :placeId")
    fun observeSourcesForPlace(placeId: Long): Flow<List<PlaceSourceEntity>>

    @Query("SELECT * FROM geographic_layers ORDER BY z_index ASC")
    fun observeLayers(): Flow<List<GeographicLayerEntity>>

    @Query("SELECT * FROM historical_routes WHERE map_id = :mapId")
    fun observeRoutesForMap(mapId: String): Flow<List<HistoricalRouteEntity>>

    @Query("SELECT * FROM route_points WHERE route_id = :routeId ORDER BY sequence_order ASC")
    fun observeRoutePoints(routeId: Long): Flow<List<RoutePointEntity>>

    @Query("SELECT * FROM battle_phases WHERE battle_map_id = :battleMapId ORDER BY phase_order ASC")
    fun observeBattlePhases(battleMapId: String): Flow<List<BattlePhaseEntity>>

    @Query("SELECT * FROM battle_map_features WHERE battle_map_id = :battleMapId")
    fun observeBattleFeatures(battleMapId: String): Flow<List<BattleMapFeatureEntity>>

    @Query("SELECT * FROM places WHERE normalized_name LIKE '%' || :normalizedQuery || '%' LIMIT :limit")
    suspend fun searchPlaces(normalizedQuery: String, limit: Int = 40): List<PlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity): Long

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Query("SELECT * FROM places WHERE name = :name LIMIT 1")
    suspend fun findPlaceByName(name: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE stable_id = :stableId LIMIT 1")
    suspend fun findPlaceByStableId(stableId: String): PlaceEntity?

    @Query("DELETE FROM place_coordinates WHERE place_id = :placeId")
    suspend fun deleteCoordinatesForPlace(placeId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaceCoordinate(coord: PlaceCoordinateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaceSource(source: PlaceSourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: GeographicLayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: HistoricalRouteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattlePhase(phase: BattlePhaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattleFeature(feature: BattleMapFeatureEntity): Long

    @Query("SELECT COUNT(*) FROM geographic_layers")
    suspend fun countLayers(): Int
}

@Dao
interface QuranHadithDao {
    @Query("SELECT * FROM quran_verses ORDER BY surah_number ASC, ayah_number ASC")
    fun observeAllVerses(): Flow<List<QuranVerseEntity>>

    @Query("SELECT * FROM quran_verses WHERE id = :verseId")
    suspend fun getVerseById(verseId: Long): QuranVerseEntity?

    @Query("""
        SELECT q.* FROM quran_verses q
        INNER JOIN event_verses ev ON q.id = ev.verse_id
        WHERE ev.event_id = :eventId
    """)
    fun observeVersesForEvent(eventId: Long): Flow<List<QuranVerseEntity>>

    @Query("SELECT * FROM hadiths ORDER BY id DESC")
    fun observeAllHadiths(): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE event_id = :eventId")
    fun observeHadithsForEvent(eventId: Long): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE id = :hadithId")
    suspend fun getHadithById(hadithId: Long): HadithEntity?

    @Query("SELECT * FROM hadith_grades WHERE hadith_id = :hadithId")
    fun observeGradesForHadith(hadithId: Long): Flow<List<HadithGradeEntity>>

    @Query("SELECT * FROM quran_verses WHERE normalized_text LIKE '%' || :normalizedQuery || '%' LIMIT :limit")
    suspend fun searchVerses(normalizedQuery: String, limit: Int = 30): List<QuranVerseEntity>

    @Query("SELECT * FROM hadiths WHERE normalized_text LIKE '%' || :normalizedQuery || '%' LIMIT :limit")
    suspend fun searchHadiths(normalizedQuery: String, limit: Int = 30): List<HadithEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(verse: QuranVerseEntity): Long

    @Update
    suspend fun updateVerse(verse: QuranVerseEntity)

    @Query("SELECT * FROM quran_verses WHERE surah_number = :surah AND ayah_number = :ayah LIMIT 1")
    suspend fun findVerse(surah: Int, ayah: Int): QuranVerseEntity?

    @Query("SELECT * FROM quran_verses WHERE stable_id = :stableId LIMIT 1")
    suspend fun findVerseByStableId(stableId: String): QuranVerseEntity?

    @Query("SELECT COUNT(*) FROM event_verses WHERE event_id = :eventId AND verse_id = :verseId")
    suspend fun countEventVerse(eventId: Long, verseId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventVerse(link: EventVerseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadith(hadith: HadithEntity): Long

    @Update
    suspend fun updateHadith(hadith: HadithEntity)

    @Query("SELECT * FROM hadiths WHERE source_name = :source AND hadith_number = :num LIMIT 1")
    suspend fun findHadith(source: String, num: String): HadithEntity?

    @Query("SELECT * FROM hadiths WHERE stable_id = :stableId LIMIT 1")
    suspend fun findHadithByStableId(stableId: String): HadithEntity?

    @Query("SELECT * FROM hadith_grades WHERE hadith_id = :hadithId")
    suspend fun getGradesForHadithSync(hadithId: Long): List<HadithGradeEntity>

    @Query("DELETE FROM hadith_grades WHERE hadith_id = :hadithId AND is_human_review = 0")
    suspend fun deleteNonHumanGradesForHadith(hadithId: Long)

    @Query("UPDATE hadith_grades SET review_state = 'NEEDS_REVALIDATION' WHERE hadith_id = :hadithId AND is_human_review = 1")
    suspend fun markHumanReviewsNeedsRevalidation(hadithId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadithGrade(grade: HadithGradeEntity): Long
}

@Dao
interface UserDataDao {
    @Query("SELECT * FROM favorites ORDER BY created_at_epoch_ms DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE item_type = :type AND item_id = :itemId)")
    fun observeIsFavorite(type: String, itemId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE item_type = :type AND item_id = :itemId")
    suspend fun removeFavorite(type: String, itemId: Long)

    @Query("SELECT * FROM user_notes ORDER BY updated_at_epoch_ms DESC")
    fun observeNotes(): Flow<List<UserNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: UserNoteEntity): Long

    @Delete
    suspend fun deleteNote(note: UserNoteEntity)

    @Query("SELECT * FROM downloads ORDER BY title ASC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: DownloadEntity): Long

    @Query("SELECT COUNT(*) FROM downloads")
    suspend fun countDownloads(): Int
}
