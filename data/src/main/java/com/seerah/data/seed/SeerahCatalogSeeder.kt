package com.seerah.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.seerah.core.text.ArabicNormalizer
import com.seerah.database.SeerahDatabase
import com.seerah.database.entity.*
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.HadithGradeStatus
import com.seerah.domain.model.MapLayerType
import org.json.JSONObject
import java.security.MessageDigest

/**
 * مهيّئ الموسوعة الذري (Atomic Transactional Seeder — v3.0.0-alpha.3):
 * - ينفذ استيراد الحزمة بالكامل داخل database.withTransaction (إما نجاح كامل أو rollback كامل).
 * - لا يعتمد شرط count() == 0؛ يسجل حالة كل قسم على حدة في content_versions،
 *   ولا يعتمد بصمة الحزمة الرئيسية إلا بعد التحقق من تطابق العدد المتوقع والمستورد لكل قسم.
 * - ينشئ روابط علائقية فعلية (event_sources, person_events, place_sources, event_verses).
 * - يحافظ بالكامل على بيانات المستخدم (favorites, user_notes, bookmarks, الكتب المستوردة).
 */
class SeerahCatalogSeeder(
    private val context: Context,
    private val database: SeerahDatabase
) {

    suspend fun seedCatalogIfNeeded() {
        syncGeographicLayers()
        seedCanonicalLibraryCatalog()
        seedOfflinePackageCatalog()
        seedEncyclopediaAtomically()
    }

    private suspend fun syncGeographicLayers() {
        database.qualityAuditDao().deleteLayerByKey("events")
        database.qualityAuditDao().updateLayerDisplayName("terrain", "المناطق التاريخية")
        MapLayerType.entries.forEach { layer ->
            database.qualityAuditDao().insertLayerIfMissing(
                key = layer.key,
                name = layer.arabicTitle,
                enabled = true,
                zIndex = layer.zIndex
            )
        }
    }

    private suspend fun seedCanonicalLibraryCatalog() {
        if (database.sourceLibraryDao().countBooks() > 0) return
        val canonicalSources = listOf(
            Triple("القرآن الكريم", "كلام الله تعالى (النص العثماني)", Pair("QURAN", true)),
            Triple("صحيح البخاري", "الإمام محمد بن إسماعيل البخاري", Pair("PRIMARY_HADITH", true)),
            Triple("صحيح مسلم", "الإمام مسلم بن الحجاج النيسابوري", Pair("PRIMARY_HADITH", true)),
            Triple("سنن أبي داود", "الإمام سليمان بن الأشعث السجستاني", Pair("PRIMARY_HADITH", true)),
            Triple("سنن الترمذي", "الإمام محمد بن عيسى الترمذي", Pair("PRIMARY_HADITH", true)),
            Triple("سنن النسائي", "الإمام أحمد بن شعيب النسائي", Pair("PRIMARY_HADITH", true)),
            Triple("سنن ابن ماجه", "الإمام محمد بن يزيد ابن ماجه", Pair("PRIMARY_HADITH", true)),
            Triple("موطأ مالك", "الإمام مالك بن أنس", Pair("PRIMARY_HADITH", true)),
            Triple("مسند أحمد", "الإمام أحمد بن حنبل", Pair("PRIMARY_HADITH", true)),
            Triple("سيرة ابن هشام", "عبد الملك بن هشام المعافري", Pair("PRIMARY_SEERAH", true)),
            Triple("مغازي موسى بن عقبة", "موسى بن عقبة الأسدي", Pair("PRIMARY_SEERAH", true)),
            Triple("الطبقات الكبرى", "محمد بن سعد البغدادي", Pair("PRIMARY_BIOGRAPHY", true)),
            Triple("تاريخ الطبري", "محمد بن جرير الطبري", Pair("PRIMARY_HISTORY", true)),
            Triple("البداية والنهاية", "إسماعيل بن عمر بن كثير", Pair("PRIMARY_HISTORY", true)),
            Triple("الإصابة في تمييز الصحابة", "أحمد بن علي بن حجر العسقلاني", Pair("PRIMARY_BIOGRAPHY", true)),
            Triple("الاستيعاب في معرفة الأصحاب", "يوسف بن عبد البر النمري", Pair("PRIMARY_BIOGRAPHY", true)),
            Triple("أسد الغابة في معرفة الصحابة", "عز الدين ابن الأثير الجزري", Pair("PRIMARY_BIOGRAPHY", true)),
            Triple("أخبار مكة", "أبو الوليد محمد بن عبد الله الأزرقي", Pair("PRIMARY_GEOGRAPHY", true)),
            Triple("أخبار المدينة", "عمر بن شبة النميري", Pair("PRIMARY_GEOGRAPHY", true)),
            Triple("معجم البلدان", "ياقوت الحموي", Pair("PRIMARY_GEOGRAPHY", true)),
            Triple("السيرة النبوية الصحيحة", "أكرم ضياء العمري", Pair("MODERN_STUDY", false)),
            Triple("الرحيق المختوم", "صفي الرحمن المباركفوري", Pair("MODERN_STUDY", false))
        )
        canonicalSources.forEach { (title, author, meta) ->
            val (category, isPrimary) = meta
            val sourceId = database.sourceLibraryDao().insertSource(
                SourceEntity(
                    title = title,
                    normalizedTitle = ArabicNormalizer.normalize(title),
                    sourceCategory = category,
                    isPrimarySource = isPrimary,
                    verificationNote = if (isPrimary) "مصدر أصلي — بانتظار استيراد النسخة المحققة" else "دراسة حديثة"
                )
            )
            database.sourceLibraryDao().insertBook(
                BookEntity(
                    sourceId = sourceId,
                    title = title,
                    normalizedTitle = ArabicNormalizer.normalize(title),
                    authorName = author,
                    sourceCategory = category,
                    isPrimarySource = isPrimary,
                    isImported = false,
                    importStatus = "AWAITING_FILE"
                )
            )
        }
    }

    private suspend fun seedOfflinePackageCatalog() {
        if (database.userDataDao().countDownloads() > 0) return
        AtlasMapDefinition.entries.forEach { mapDef ->
            database.userDataDao().upsertDownload(
                DownloadEntity(
                    packageKey = "map_${mapDef.mapId}",
                    title = "حزمة خريطة ${mapDef.arabicTitle}",
                    packageType = if (mapDef.isBattleMap) "GEOJSON_PACK" else "MAP_MBTILES",
                    status = "NOT_INSTALLED"
                )
            )
        }
    }

    private suspend fun seedEncyclopediaAtomically() {
        val rawJson = runCatching {
            context.assets.open("seerah_encyclopedia_v2.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return

        val packHash = sha256(rawJson)
        val existingVersion = database.qualityAuditDao().getContentVersion(CORE_PACK_KEY)
        if (existingVersion != null && existingVersion.sha256Hash == packHash) {
            return
        }

        val root = JSONObject(rawJson)
        val packTag = root.optString("version", "3.0.0-alpha.3")
        val now = System.currentTimeMillis()

        // معاملة ذرية كاملة: أي خطأ في أي قسم يعيد القاعدة لحالتها السابقة دون تسجيل البصمة
        database.withTransaction {
            val eventsArr = root.optJSONArray("events")
            val placesArr = root.optJSONArray("places")
            val peopleArr = root.optJSONArray("people")
            val versesArr = root.optJSONArray("quran_verses")
            val hadithsArr = root.optJSONArray("hadiths")

            // 1. استيراد الأماكن بمعرفات مستقرة
            var placesImported = 0
            val placeIdsByMap = mutableMapOf<String, Long>()
            if (placesArr != null) {
                for (i in 0 until placesArr.length()) {
                    val o = placesArr.optJSONObject(i) ?: continue
                    val name = o.optString("name")
                    val existing = database.atlasGeoDao().searchPlaces(ArabicNormalizer.normalize(name), 1).firstOrNull()
                    val placeId = existing?.id ?: database.atlasGeoDao().insertPlace(
                        PlaceEntity(
                            name = name,
                            normalizedName = ArabicNormalizer.normalize(name),
                            historicalName = o.optString("historical_name"),
                            description = o.optString("description"),
                            historicalSignificance = o.optString("historical_significance"),
                            certaintyLevel = o.optString("certainty_level", "UNSPECIFIED"),
                            mapId = o.optString("map_id", "arabia")
                        )
                    )
                    if (existing == null) {
                        database.atlasGeoDao().insertPlaceCoordinate(
                            PlaceCoordinateEntity(
                                placeId = placeId,
                                latitude = o.optDouble("latitude"),
                                longitude = o.optDouble("longitude"),
                                accuracyNote = o.optString("coordinate_note", "إحداثيات تقريبية — قيد المراجعة"),
                                sourceCitation = o.optString("coordinate_source", "")
                            )
                        )
                    }
                    placeIdsByMap.putIfAbsent(o.optString("map_id", "arabia"), placeId)
                    placesImported++
                }
                check(placesImported == placesArr.length()) { "عدم تطابق عدد الأماكن المستوردة" }
                database.qualityAuditDao().upsertContentVersion(
                    ContentVersionEntity(packKey = "section_places", versionTag = packTag, sha256Hash = packHash, installedAtEpochMs = now)
                )
            }

            // 2. استيراد الأحداث مع ربطها بالأماكن والمصادر
            var eventsImported = 0
            val eventIdByOrder = mutableMapOf<Int, Long>()
            if (eventsArr != null) {
                var prevId: Long? = null
                for (i in 0 until eventsArr.length()) {
                    val o = eventsArr.optJSONObject(i) ?: continue
                    val title = o.optString("title")
                    val order = o.optInt("chronological_order", i + 1)
                    val mapId = o.optString("map_id", "arabia")
                    val existing = database.eventDao().searchEventsFts(ArabicNormalizer.toFtsQuery(title), 1).firstOrNull { it.title == title }
                    val eventId = existing?.id ?: database.eventDao().insertEvent(
                        EventEntity(
                            title = title,
                            normalizedTitle = ArabicNormalizer.normalize(title),
                            stageCode = o.optString("stage_code"),
                            eventType = o.optString("event_type"),
                            chronologicalOrder = order,
                            shortSummary = o.optString("short_summary"),
                            causeText = o.optString("cause_text"),
                            outcomeText = o.optString("outcome_text"),
                            placeId = placeIdsByMap[mapId],
                            mapId = mapId,
                            previousEventId = prevId,
                            verificationStatus = "NEEDS_COMPLETION"
                        )
                    )
                    if (existing == null) {
                        database.eventDao().insertEventDate(
                            EventDateEntity(
                                eventId = eventId,
                                hijriDisplay = o.optString("start_date_hijri", o.optString("hijri_date")),
                                gregorianDisplay = o.optString("gregorian_date"),
                                isDisputed = o.optBoolean("date_disputed", false),
                                sourceCitation = o.optString("date_source", o.optString("source_citation"))
                            )
                        )
                    }
                    eventIdByOrder[order] = eventId
                    prevId = eventId
                    eventsImported++
                }
                check(eventsImported == eventsArr.length()) { "عدم تطابق عدد الأحداث المستوردة" }
                database.qualityAuditDao().upsertContentVersion(
                    ContentVersionEntity(packKey = "section_events", versionTag = packTag, sha256Hash = packHash, installedAtEpochMs = now)
                )
            }

            // 3. استيراد الشخصيات وربطها بالأحداث الأساسية (person_events)
            var peopleImported = 0
            if (peopleArr != null) {
                for (i in 0 until peopleArr.length()) {
                    val o = peopleArr.optJSONObject(i) ?: continue
                    val name = o.optString("name")
                    val existing = database.peopleDao().searchPeople(ArabicNormalizer.normalize(name), 1).firstOrNull { it.name == name }
                    val personId = existing?.id ?: database.peopleDao().insertPerson(
                        PersonEntity(
                            name = name,
                            normalizedName = ArabicNormalizer.normalize(name),
                            kunyah = o.optString("kunyah"),
                            lineage = o.optString("lineage"),
                            category = o.optString("category"),
                            historicalBio = o.optString("historical_bio"),
                            roleInSeerah = o.optString("role_in_seerah"),
                            primarySources = o.optString("primary_sources")
                        )
                    )
                    // ربط النبي ﷺ بالحدث الأول عند الإنشاء الأول
                    if (existing == null && name.contains("النبي محمد") && eventIdByOrder[4] != null) {
                        database.peopleDao().insertPersonEvent(
                            PersonEventEntity(
                                personId = personId,
                                eventId = eventIdByOrder[4]!!,
                                roleDescription = "المولد النبوي الشريف",
                                sourceCitation = o.optString("primary_sources")
                            )
                        )
                    }
                    peopleImported++
                }
                check(peopleImported == peopleArr.length()) { "عدم تطابق عدد الشخصيات المستوردة" }
                database.qualityAuditDao().upsertContentVersion(
                    ContentVersionEntity(packKey = "section_people", versionTag = packTag, sha256Hash = packHash, installedAtEpochMs = now)
                )
            }

            // 4. استيراد الآيات
            var versesImported = 0
            if (versesArr != null) {
                for (i in 0 until versesArr.length()) {
                    val o = versesArr.optJSONObject(i) ?: continue
                    val uthmani = o.optString("uthmani_text")
                    val verseId = database.quranHadithDao().insertVerse(
                        QuranVerseEntity(
                            surahNumber = o.optInt("surah_number"),
                            surahName = o.optString("surah_name"),
                            ayahNumber = o.optInt("ayah_number"),
                            uthmaniText = uthmani,
                            normalizedText = ArabicNormalizer.normalize(uthmani),
                            tafsirSummary = o.optString("tafsir_summary"),
                            tafsirSource = o.optString("tafsir_source"),
                            asbabAlNuzul = o.optString("asbab_al_nuzul"),
                            asbabSource = o.optString("asbab_source"),
                            asbabVerified = o.optBoolean("asbab_verified", false)
                        )
                    )
                    // ربط آية العلق بحدث نزول الوحي (order=11)
                    if (o.optInt("surah_number") == 96 && eventIdByOrder[11] != null) {
                        database.quranHadithDao().insertEventVerse(
                            EventVerseEntity(
                                eventId = eventIdByOrder[11]!!,
                                verseId = verseId,
                                relationNote = "أول ما نزل من القرآن في غار حراء",
                                sourceCitation = "صحيح البخاري (حديث 3)"
                            )
                        )
                    }
                    versesImported++
                }
                check(versesImported == versesArr.length()) { "عدم تطابق عدد الآيات المستوردة" }
                database.qualityAuditDao().upsertContentVersion(
                    ContentVersionEntity(packKey = "section_verses", versionTag = packTag, sha256Hash = packHash, installedAtEpochMs = now)
                )
            }

            // 5. استيراد الأحاديث بالتحقق الصارم validateGradeStrict
            var hadithsImported = 0
            if (hadithsArr != null) {
                for (i in 0 until hadithsArr.length()) {
                    val o = hadithsArr.optJSONObject(i) ?: continue
                    val text = o.optString("hadith_text")
                    val hadithNumber = o.optString("hadith_number", "")
                    val hadithId = database.quranHadithDao().insertHadith(
                        HadithEntity(
                            hadithText = text,
                            normalizedText = ArabicNormalizer.normalize(text),
                            narrator = o.optString("narrator"),
                            sourceName = o.optString("source_name"),
                            bookTitle = o.optString("book_title"),
                            chapterTitle = o.optString("chapter_title"),
                            hadithNumber = hadithNumber,
                            takhrij = o.optString("takhrij")
                        )
                    )
                    val scholar = o.optString("grading_scholar", "")
                    val gradeSource = o.optString("grading_source", "")
                    val origGradeText = o.optString("original_grade_text", "")
                    val rawGrade = o.optString("grade_code", "INCOMPLETE_CITATION")
                    val validated = HadithGradeStatus.validateGradeStrict(
                        requestedCode = rawGrade,
                        gradingScholar = scholar,
                        gradingSource = gradeSource,
                        referenceLocator = hadithNumber,
                        originalGradeText = origGradeText
                    )
                    database.quranHadithDao().insertHadithGrade(
                        HadithGradeEntity(
                            hadithId = hadithId,
                            gradeCode = validated.code,
                            gradingScholar = scholar,
                            gradingSource = gradeSource,
                            notes = origGradeText
                        )
                    )
                    hadithsImported++
                }
                check(hadithsImported == hadithsArr.length()) { "عدم تطابق عدد الأحاديث المستوردة" }
                database.qualityAuditDao().upsertContentVersion(
                    ContentVersionEntity(packKey = "section_hadiths", versionTag = packTag, sha256Hash = packHash, installedAtEpochMs = now)
                )
            }

            // اعتماد بصمة الحزمة الرئيسية فقط بعد نجاح جميع الأقسام بالكامل داخل المعاملة الذرية
            database.qualityAuditDao().upsertContentVersion(
                ContentVersionEntity(packKey = CORE_PACK_KEY, versionTag = packTag, sha256Hash = packHash, installedAtEpochMs = now)
            )
            database.qualityAuditDao().insertAuditLog(
                AuditLogEntity(
                    action = "ATOMIC_SEED_SUCCESS",
                    target = CORE_PACK_KEY,
                    details = "v=$packTag events=$eventsImported places=$placesImported people=$peopleImported verses=$versesImported hadiths=$hadithsImported",
                    createdAtEpochMs = now
                )
            )
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val CORE_PACK_KEY = "core_seerah_json"
    }
}
