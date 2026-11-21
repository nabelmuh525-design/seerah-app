package com.seerah.data.repository

import android.net.Uri
import com.seerah.core.text.ArabicNormalizer
import com.seerah.data.backup.BackupRestoreManager
import com.seerah.data.importer.BookImportEngine
import com.seerah.data.prefs.SeerahPreferences
import com.seerah.data.seed.SeerahCatalogSeeder
import com.seerah.data.updater.SeerahAppUpdater
import com.seerah.database.SeerahDatabase
import com.seerah.database.entity.*
import com.seerah.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * المستودع الموحد لتطبيق سِيرَة (Offline-First Repository).
 */
class SeerahRepository(
    val database: SeerahDatabase,
    val preferences: SeerahPreferences,
    val importEngine: BookImportEngine,
    val backupRestoreManager: BackupRestoreManager,
    val appUpdater: SeerahAppUpdater
) {
    private val seeder = SeerahCatalogSeeder(database)

    suspend fun initializeOfflineCatalog() = withContext(Dispatchers.IO) {
        seeder.seedCatalogIfNeeded()
    }

    // Events & Timeline
    fun observeAllEvents(): Flow<List<EventEntity>> = database.eventDao().observeAllEvents()
    fun observeEventsByStage(stageCode: String): Flow<List<EventEntity>> = database.eventDao().observeEventsByStage(stageCode)
    fun observeBattles(): Flow<List<EventEntity>> = database.eventDao().observeEventsByType("GHAZWAH")
    fun observeExpeditions(): Flow<List<EventEntity>> = database.eventDao().observeEventsByType("SARIYYAH")
    suspend fun getEventById(id: Long): EventEntity? = database.eventDao().getEventById(id)
    fun observeEventDates(eventId: Long): Flow<List<EventDateEntity>> = database.eventDao().observeEventDates(eventId)
    fun observeEventSources(eventId: Long): Flow<List<EventSourceEntity>> = database.eventDao().observeEventSources(eventId)
    fun observeNarrationsForEvent(eventId: Long): Flow<List<NarrationEntity>> = database.eventDao().observeNarrationsForEvent(eventId)

    // People
    fun observeAllPeople(): Flow<List<PersonEntity>> = database.peopleDao().observeAllPeople()
    fun observePeopleByCategory(category: String): Flow<List<PersonEntity>> = database.peopleDao().observePeopleByCategory(category)
    suspend fun getPersonById(id: Long): PersonEntity? = database.peopleDao().getPersonById(id)
    fun observePersonEvents(personId: Long): Flow<List<PersonEventEntity>> = database.peopleDao().observePersonEvents(personId)
    fun observeEventParticipants(eventId: Long): Flow<List<PersonEventEntity>> = database.peopleDao().observeEventParticipants(eventId)

    // Places & Atlas
    fun observeAllPlaces(): Flow<List<PlaceEntity>> = database.atlasGeoDao().observeAllPlaces()
    fun observePlacesForMap(mapId: String): Flow<List<PlaceEntity>> = database.atlasGeoDao().observePlacesForMap(mapId)
    suspend fun getPlaceById(id: Long): PlaceEntity? = database.atlasGeoDao().getPlaceById(id)
    fun observeCoordinatesForPlace(placeId: Long): Flow<List<PlaceCoordinateEntity>> = database.atlasGeoDao().observeCoordinatesForPlace(placeId)
    fun observeLayers(): Flow<List<GeographicLayerEntity>> = database.atlasGeoDao().observeLayers()
    fun observeRoutesForMap(mapId: String): Flow<List<HistoricalRouteEntity>> = database.atlasGeoDao().observeRoutesForMap(mapId)
    fun observeRoutePoints(routeId: Long): Flow<List<RoutePointEntity>> = database.atlasGeoDao().observeRoutePoints(routeId)
    fun observeBattlePhases(battleMapId: String): Flow<List<BattlePhaseEntity>> = database.atlasGeoDao().observeBattlePhases(battleMapId)
    fun observeBattleFeatures(battleMapId: String): Flow<List<BattleMapFeatureEntity>> = database.atlasGeoDao().observeBattleFeatures(battleMapId)

    // Quran & Hadith
    fun observeAllVerses(): Flow<List<QuranVerseEntity>> = database.quranHadithDao().observeAllVerses()
    suspend fun getVerseById(id: Long): QuranVerseEntity? = database.quranHadithDao().getVerseById(id)
    fun observeVersesForEvent(eventId: Long): Flow<List<QuranVerseEntity>> = database.quranHadithDao().observeVersesForEvent(eventId)
    fun observeAllHadiths(): Flow<List<HadithEntity>> = database.quranHadithDao().observeAllHadiths()
    fun observeHadithsForEvent(eventId: Long): Flow<List<HadithEntity>> = database.quranHadithDao().observeHadithsForEvent(eventId)
    suspend fun getHadithById(id: Long): HadithEntity? = database.quranHadithDao().getHadithById(id)
    fun observeGradesForHadith(hadithId: Long): Flow<List<HadithGradeEntity>> = database.quranHadithDao().observeGradesForHadith(hadithId)

    // Library & Books
    fun observeBooks(): Flow<List<BookEntity>> = database.sourceLibraryDao().observeBooks()
    suspend fun getBookById(id: Long): BookEntity? = database.sourceLibraryDao().getBookById(id)
    fun observeBookPages(bookId: Long): Flow<List<SourcePageEntity>> = database.sourceLibraryDao().observeBookPages(bookId)
    fun observeEditions(bookId: Long): Flow<List<EditionEntity>> = database.sourceLibraryDao().observeEditions(bookId)

    suspend fun deleteImportedBook(book: BookEntity) = withContext(Dispatchers.IO) {
        if (book.localFilePath.isNotBlank()) {
            runCatching { java.io.File(book.localFilePath).delete() }
        }
        database.sourceLibraryDao().deleteBook(book)
    }

    suspend fun importFile(
        uri: Uri,
        isPrimarySource: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport = importEngine.importFromUri(uri, isPrimarySource, authorName, editionName, investigator)

    // User Data
    fun observeFavorites(): Flow<List<FavoriteEntity>> = database.userDataDao().observeFavorites()
    fun observeIsFavorite(type: String, id: Long): Flow<Boolean> = database.userDataDao().observeIsFavorite(type, id)
    suspend fun toggleFavorite(type: String, id: Long, title: String, subtitle: String, currentlyFav: Boolean) {
        if (currentlyFav) {
            database.userDataDao().removeFavorite(type, id)
        } else {
            database.userDataDao().upsertFavorite(
                FavoriteEntity(
                    itemType = type,
                    itemId = id,
                    title = title,
                    subtitle = subtitle,
                    createdAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun observeNotes(): Flow<List<UserNoteEntity>> = database.userDataDao().observeNotes()
    suspend fun saveNote(itemType: String, itemId: Long, title: String, content: String) {
        database.userDataDao().upsertNote(
            UserNoteEntity(
                itemType = itemType,
                itemId = itemId,
                title = title,
                content = content,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }
    suspend fun deleteNote(note: UserNoteEntity) = database.userDataDao().deleteNote(note)
    fun observeDownloads(): Flow<List<DownloadEntity>> = database.userDataDao().observeDownloads()

    suspend fun checkDatabaseHealth(): DatabaseHealthReport = withContext(Dispatchers.IO) {
        val (ok, detail) = database.runIntegrityCheck()
        DatabaseHealthReport(
            isHealthy = ok,
            detail = detail,
            booksCount = database.sourceLibraryDao().countBooks(),
            eventsCount = database.eventDao().countEvents()
        )
    }

    /**
     * محرك البحث الشامل المحلي دون إنترنت مع تطبيع النص العربي وربط كل نتيجة بمصدرها.
     */
    suspend fun searchOffline(
        rawQuery: String,
        filter: SearchContentFilter = SearchContentFilter.ALL
    ): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val normalized = ArabicNormalizer.normalize(rawQuery)
        val ftsQuery = ArabicNormalizer.toFtsQuery(rawQuery)
        if (normalized.isBlank() || ftsQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResultItem>()

        if (filter == SearchContentFilter.ALL || filter == SearchContentFilter.EVENTS ||
            filter == SearchContentFilter.BATTLES || filter == SearchContentFilter.EXPEDITIONS
        ) {
            runCatching { database.eventDao().searchEventsFts(ftsQuery) }.getOrDefault(emptyList())
                .filter { e ->
                    when (filter) {
                        SearchContentFilter.BATTLES -> e.eventType == "GHAZWAH"
                        SearchContentFilter.EXPEDITIONS -> e.eventType == "SARIYYAH"
                        else -> true
                    }
                }
                .forEach { e ->
                    results.add(
                        SearchResultItem(
                            id = e.id,
                            contentType = when (e.eventType) {
                                "GHAZWAH" -> SearchContentFilter.BATTLES
                                "SARIYYAH" -> SearchContentFilter.EXPEDITIONS
                                else -> SearchContentFilter.EVENTS
                            },
                            title = e.title,
                            snippet = e.shortSummary,
                            sourceCitation = "سجل الأحداث (${e.verificationStatus})",
                            stageCode = e.stageCode
                        )
                    )
                }
        }

        if (filter == SearchContentFilter.ALL || filter == SearchContentFilter.PEOPLE) {
            database.peopleDao().searchPeople(normalized).forEach { p ->
                results.add(
                    SearchResultItem(
                        id = p.id,
                        contentType = SearchContentFilter.PEOPLE,
                        title = p.name,
                        snippet = p.roleInSeerah.ifBlank { p.historicalBio },
                        sourceCitation = p.primarySources.ifBlank { "موسوعة الشخصيات — بانتظار استيراد المرجع" }
                    )
                )
            }
        }

        if (filter == SearchContentFilter.ALL || filter == SearchContentFilter.PLACES) {
            database.atlasGeoDao().searchPlaces(normalized).forEach { pl ->
                results.add(
                    SearchResultItem(
                        id = pl.id,
                        contentType = SearchContentFilter.PLACES,
                        title = pl.name,
                        snippet = pl.description.ifBlank { pl.historicalSignificance },
                        sourceCitation = "الأطلس التاريخي (${GeographicCertainty.fromCode(pl.certaintyLevel).arabicLabel})"
                    )
                )
            }
        }

        if (filter == SearchContentFilter.ALL || filter == SearchContentFilter.BOOKS) {
            runCatching { database.sourceLibraryDao().searchPagesFts(ftsQuery) }.getOrDefault(emptyList())
                .forEach { page ->
                    val book = database.sourceLibraryDao().getBookById(page.bookId)
                    results.add(
                        SearchResultItem(
                            id = page.bookId,
                            contentType = SearchContentFilter.BOOKS,
                            title = book?.title ?: "كتاب مستورد",
                            snippet = page.rawText.take(180),
                            sourceCitation = "${book?.title ?: "كتاب"} — ج${page.volumeNumber} ص${page.pageNumber}"
                        )
                    )
                }
        }

        if (filter == SearchContentFilter.ALL || filter == SearchContentFilter.VERSES) {
            database.quranHadithDao().searchVerses(normalized).forEach { v ->
                results.add(
                    SearchResultItem(
                        id = v.id,
                        contentType = SearchContentFilter.VERSES,
                        title = "سورة ${v.surahName} — آية ${v.ayahNumber}",
                        snippet = v.uthmaniText,
                        sourceCitation = v.tafsirSource.ifBlank { "القرآن الكريم" }
                    )
                )
            }
        }

        if (filter == SearchContentFilter.ALL || filter == SearchContentFilter.HADITHS) {
            database.quranHadithDao().searchHadiths(normalized).forEach { h ->
                results.add(
                    SearchResultItem(
                        id = h.id,
                        contentType = SearchContentFilter.HADITHS,
                        title = "${h.sourceName} ${if (h.hadithNumber.isNotBlank()) "(${h.hadithNumber})" else ""}",
                        snippet = h.hadithText.take(180),
                        sourceCitation = "${h.sourceName} — الراوي: ${h.narrator}"
                    )
                )
            }
        }

        results
    }
}
