package com.seerah.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.seerah.core.text.ArabicNormalizer
import com.seerah.database.SeerahDatabase
import com.seerah.database.entity.*
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.HadithGradeStatus
import com.seerah.domain.model.MapLayerType
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * مزامِن الموسوعة الآمن (v3.0.1-alpha.5):
 * 1. يحمي المراجعات الحديثية البشرية (is_human_review = 1) ولا يحذفها أبدًا؛ وإذا تغيّر نص الحديث
 *    أو مصدره يضع المراجعة البشرية في حالة NEEDS_REVALIDATION بدل حذفها أو استبدالها.
 * 2. يقرأ جميع العلاقات (place_stable_id, person_events, event_verses) من بيانات الحزمة فقط
 *    (صفر قواعد مكتوبة يدويًا داخل الكود، وصفر تخمين لأول مكان في الخريطة).
 * 3. يتحقق من وجود جميع المعرفات المشار إليها في العلاقات قبل التنفيذ داخل withTransaction.
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
        if (existingVersion != null && existingVersion.sha256Hash == packHash) return

        val root = JSONObject(rawJson)
        val packTag = root.optString("version", FALLBACK_PACK_VERSION)
        val now = System.currentTimeMillis()

        val placesArr = requireNotNull(root.optJSONArray("places"))
        val eventsArr = requireNotNull(root.optJSONArray("events"))
        val peopleArr = requireNotNull(root.optJSONArray("people"))
        val versesArr = requireNotNull(root.optJSONArray("quran_verses"))
        val hadithsArr = requireNotNull(root.optJSONArray("hadiths"))
        val personEventsArr = root.optJSONArray("person_events") ?: JSONArray()
        val eventVersesArr = root.optJSONArray("event_verses") ?: JSONArray()

        val placeStableIds = collectUniqueStableIds("places", placesArr)
        val eventStableIds = collectUniqueStableIds("events", eventsArr)
        val personStableIds = collectUniqueStableIds("people", peopleArr)
        val verseStableIds = collectUniqueStableIds("quran_verses", versesArr)
        collectUniqueStableIds("hadiths", hadithsArr)

        // التحقق المسبق من سلامة جميع المعرفات المشار إليها في العلاقات
        for (i in 0 until personEventsArr.length()) {
            val r = personEventsArr.getJSONObject(i)
            check(r.optString("person_stable_id") in personStableIds) { "معرف شخصية غير موجود في person_events" }
            check(r.optString("event_stable_id") in eventStableIds) { "معرف حدث غير موجود في person_events" }
        }
        for (i in 0 until eventVersesArr.length()) {
            val r = eventVersesArr.getJSONObject(i)
            check(r.optString("event_stable_id") in eventStableIds) { "معرف حدث غير موجود في event_verses" }
            check(r.optString("verse_stable_id") in verseStableIds) { "معرف آية غير موجود في event_verses" }
        }

        database.withTransaction {
            val placeDbIdByStable = mutableMapOf<String, Long>()
            for (i in 0 until placesArr.length()) {
                val o = placesArr.getJSONObject(i)
                val stableId = o.getString("stable_id")
                val name = o.getString("name")
                val existing = database.atlasGeoDao().findPlaceByStableId(stableId)
                    ?: database.atlasGeoDao().findPlaceByName(name)
                val placeId = if (existing != null) {
                    database.atlasGeoDao().updatePlace(
                        existing.copy(
                            stableId = stableId,
                            name = name,
                            normalizedName = ArabicNormalizer.normalize(name),
                            historicalName = o.optString("historical_name"),
                            description = o.optString("description"),
                            historicalSignificance = o.optString("historical_significance"),
                            certaintyLevel = o.optString("certainty_level", "UNSPECIFIED"),
                            mapId = o.optString("map_id", "arabia")
                        )
                    )
                    existing.id
                } else {
                    database.atlasGeoDao().insertPlace(
                        PlaceEntity(
                            stableId = stableId,
                            name = name,
                            normalizedName = ArabicNormalizer.normalize(name),
                            historicalName = o.optString("historical_name"),
                            description = o.optString("description"),
                            historicalSignificance = o.optString("historical_significance"),
                            certaintyLevel = o.optString("certainty_level", "UNSPECIFIED"),
                            mapId = o.optString("map_id", "arabia")
                        )
                    )
                }
                database.atlasGeoDao().deleteCoordinatesForPlace(placeId)
                database.atlasGeoDao().insertPlaceCoordinate(
                    PlaceCoordinateEntity(
                        placeId = placeId,
                        latitude = o.optDouble("latitude"),
                        longitude = o.optDouble("longitude"),
                        accuracyNote = o.optString("coordinate_note", "إحداثيات تقريبية — قيد المراجعة"),
                        sourceCitation = o.optString("coordinate_source", "")
                    )
                )
                placeDbIdByStable[stableId] = placeId
            }

            // الأحداث: ربط بالمكان فقط عبر place_stable_id الصريح (صفر تخمين!)
            val eventDbIdByStable = mutableMapOf<String, Long>()
            var prevId: Long? = null
            for (i in 0 until eventsArr.length()) {
                val o = eventsArr.getJSONObject(i)
                val stableId = o.getString("stable_id")
                val title = o.getString("title")
                val placeStableId = o.optString("place_stable_id", "")
                val resolvedPlaceId = if (placeStableId.isNotBlank()) {
                    check(placeStableId in placeStableIds) { "place_stable_id غير موجود: $placeStableId" }
                    placeDbIdByStable[placeStableId]
                } else null

                val existing = database.eventDao().findEventByStableId(stableId)
                    ?: database.eventDao().findEventByTitle(title)
                val eventId = if (existing != null) {
                    database.eventDao().updateEvent(
                        existing.copy(
                            stableId = stableId,
                            title = title,
                            normalizedTitle = ArabicNormalizer.normalize(title),
                            stageCode = o.optString("stage_code"),
                            eventType = o.optString("event_type"),
                            chronologicalOrder = o.optInt("chronological_order", i + 1),
                            shortSummary = o.optString("short_summary"),
                            detailedAnalysis = o.optString("detailed_analysis", existing.detailedAnalysis),
                            causeText = o.optString("cause_text"),
                            outcomeText = o.optString("outcome_text"),
                            placeId = resolvedPlaceId,
                            mapId = o.optString("map_id", "arabia"),
                            previousEventId = prevId,
                            verificationStatus = o.optString("verification_status", "NEEDS_COMPLETION")
                        )
                    )
                    existing.id
                } else {
                    database.eventDao().insertEvent(
                        EventEntity(
                            stableId = stableId,
                            title = title,
                            normalizedTitle = ArabicNormalizer.normalize(title),
                            stageCode = o.optString("stage_code"),
                            eventType = o.optString("event_type"),
                            chronologicalOrder = o.optInt("chronological_order", i + 1),
                            shortSummary = o.optString("short_summary"),
                            detailedAnalysis = o.optString("detailed_analysis", ""),
                            causeText = o.optString("cause_text"),
                            outcomeText = o.optString("outcome_text"),
                            placeId = resolvedPlaceId,
                            mapId = o.optString("map_id", "arabia"),
                            previousEventId = prevId,
                            verificationStatus = o.optString("verification_status", "NEEDS_COMPLETION")
                        )
                    )
                }
                database.eventDao().deleteEventDates(eventId)
                database.eventDao().insertEventDate(
                    EventDateEntity(
                        eventId = eventId,
                        hijriDisplay = o.optString("start_date_hijri", o.optString("hijri_date")),
                        gregorianDisplay = o.optString("gregorian_date"),
                        isDisputed = o.optBoolean("date_disputed", false),
                        sourceCitation = o.optString("date_source", o.optString("source_citation"))
                    )
                )
                eventDbIdByStable[stableId] = eventId
                prevId = eventId
            }

            // الشخصيات
            val personDbIdByStable = mutableMapOf<String, Long>()
            for (i in 0 until peopleArr.length()) {
                val o = peopleArr.getJSONObject(i)
                val stableId = o.getString("stable_id")
                val name = o.getString("name")
                val existing = database.peopleDao().findPersonByStableId(stableId)
                    ?: database.peopleDao().findPersonByName(name)
                val personId = if (existing != null) {
                    database.peopleDao().updatePerson(
                        existing.copy(
                            stableId = stableId,
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
                    existing.id
                } else {
                    database.peopleDao().insertPerson(
                        PersonEntity(
                            stableId = stableId,
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
                }
                personDbIdByStable[stableId] = personId
            }

            // الآيات
            val verseDbIdByStable = mutableMapOf<String, Long>()
            for (i in 0 until versesArr.length()) {
                val o = versesArr.getJSONObject(i)
                val stableId = o.getString("stable_id")
                val surah = o.optInt("surah_number")
                val ayah = o.optInt("ayah_number")
                val uthmani = o.optString("uthmani_text")
                val existing = database.quranHadithDao().findVerseByStableId(stableId)
                    ?: database.quranHadithDao().findVerse(surah, ayah)
                val verseId = if (existing != null) {
                    database.quranHadithDao().updateVerse(
                        existing.copy(
                            stableId = stableId,
                            uthmaniText = uthmani,
                            normalizedText = ArabicNormalizer.normalize(uthmani),
                            tafsirSummary = o.optString("tafsir_summary"),
                            tafsirSource = o.optString("tafsir_source"),
                            asbabAlNuzul = o.optString("asbab_al_nuzul"),
                            asbabSource = o.optString("asbab_source"),
                            asbabVerified = o.optBoolean("asbab_verified", false)
                        )
                    )
                    existing.id
                } else {
                    database.quranHadithDao().insertVerse(
                        QuranVerseEntity(
                            stableId = stableId,
                            surahNumber = surah,
                            surahName = o.optString("surah_name"),
                            ayahNumber = ayah,
                            uthmaniText = uthmani,
                            normalizedText = ArabicNormalizer.normalize(uthmani),
                            tafsirSummary = o.optString("tafsir_summary"),
                            tafsirSource = o.optString("tafsir_source"),
                            asbabAlNuzul = o.optString("asbab_al_nuzul"),
                            asbabSource = o.optString("asbab_source"),
                            asbabVerified = o.optBoolean("asbab_verified", false)
                        )
                    )
                }
                verseDbIdByStable[stableId] = verseId
            }

            // العلاقات من بيانات الحزمة فقط (person_events و event_verses)
            for (i in 0 until personEventsArr.length()) {
                val r = personEventsArr.getJSONObject(i)
                val pid = personDbIdByStable.getValue(r.getString("person_stable_id"))
                val eid = eventDbIdByStable.getValue(r.getString("event_stable_id"))
                if (database.peopleDao().countPersonEvent(pid, eid) == 0) {
                    database.peopleDao().insertPersonEvent(
                        PersonEventEntity(
                            personId = pid,
                            eventId = eid,
                            roleDescription = r.optString("role_description"),
                            sourceCitation = r.optString("source_citation")
                        )
                    )
                }
            }
            for (i in 0 until eventVersesArr.length()) {
                val r = eventVersesArr.getJSONObject(i)
                val eid = eventDbIdByStable.getValue(r.getString("event_stable_id"))
                val vid = verseDbIdByStable.getValue(r.getString("verse_stable_id"))
                if (database.quranHadithDao().countEventVerse(eid, vid) == 0) {
                    database.quranHadithDao().insertEventVerse(
                        EventVerseEntity(
                            eventId = eid,
                            verseId = vid,
                            relationNote = r.optString("relation_note"),
                            sourceCitation = r.optString("source_citation")
                        )
                    )
                }
            }

            // الأحاديث مع حماية المراجعات البشرية (لا حذف للمراجعة البشرية؛ NEEDS_REVALIDATION عند تغيّر النص/المصدر)
            for (i in 0 until hadithsArr.length()) {
                val o = hadithsArr.getJSONObject(i)
                val stableId = o.getString("stable_id")
                val text = o.optString("hadith_text")
                val hadithNumber = o.optString("hadith_number", "")
                val sourceName = o.optString("source_name")
                val existing = database.quranHadithDao().findHadithByStableId(stableId)
                    ?: database.quranHadithDao().findHadith(sourceName, hadithNumber)
                val contentChanged = existing != null && (existing.hadithText != text || existing.sourceName != sourceName)

                val hadithId = if (existing != null) {
                    database.quranHadithDao().updateHadith(
                        existing.copy(
                            stableId = stableId,
                            hadithText = text,
                            normalizedText = ArabicNormalizer.normalize(text),
                            narrator = o.optString("narrator"),
                            sourceName = sourceName,
                            bookTitle = o.optString("book_title"),
                            chapterTitle = o.optString("chapter_title"),
                            takhrij = o.optString("takhrij")
                        )
                    )
                    existing.id
                } else {
                    database.quranHadithDao().insertHadith(
                        HadithEntity(
                            stableId = stableId,
                            hadithText = text,
                            normalizedText = ArabicNormalizer.normalize(text),
                            narrator = o.optString("narrator"),
                            sourceName = sourceName,
                            bookTitle = o.optString("book_title"),
                            chapterTitle = o.optString("chapter_title"),
                            hadithNumber = hadithNumber,
                            takhrij = o.optString("takhrij")
                        )
                    )
                }

                if (contentChanged) {
                    // الحفاظ على المراجعات البشرية مع وسمها بـ NEEDS_REVALIDATION
                    database.quranHadithDao().markHumanReviewsNeedsRevalidation(hadithId)
                }

                val existingHuman = database.quranHadithDao().getGradesForHadithSync(hadithId).any { it.isHumanReview }
                if (!existingHuman) {
                    val scholar = o.optString("grading_scholar", "")
                    val gradeSource = o.optString("grading_source", "")
                    val origGradeText = o.optString("original_grade_text", "")
                    val rawGrade = o.optString("grade_code", "INCOMPLETE_CITATION")
                    val validated = HadithGradeStatus.validateGradeStrict(
                        requestedCode = rawGrade,
                        gradingScholar = scholar,
                        gradingSource = gradeSource,
                        referenceLocator = hadithNumber,
                        originalGradeText = origGradeText,
                        isCitationVerifiedByReviewer = false
                    )
                    database.quranHadithDao().deleteNonHumanGradesForHadith(hadithId)
                    database.quranHadithDao().insertHadithGrade(
                        HadithGradeEntity(
                            stableId = "grade_$stableId",
                            hadithId = hadithId,
                            gradeCode = validated.code,
                            gradingScholar = scholar,
                            gradingSource = gradeSource,
                            notes = origGradeText,
                            isHumanReview = false,
                            reviewState = validated.code,
                            reviewedAtEpochMs = 0L
                        )
                    )
                }
            }

            database.qualityAuditDao().upsertContentVersion(
                ContentVersionEntity(packKey = CORE_PACK_KEY, versionTag = packTag, sha256Hash = packHash, installedAtEpochMs = now)
            )
            database.qualityAuditDao().insertAuditLog(
                AuditLogEntity(
                    action = "ATOMIC_SYNC_V43_SUCCESS",
                    target = CORE_PACK_KEY,
                    details = "v=$packTag pe_rels=${personEventsArr.length()} ev_rels=${eventVersesArr.length()}",
                    createdAtEpochMs = now
                )
            )
        }
    }

    private fun collectUniqueStableIds(section: String, arr: JSONArray): Set<String> {
        val seen = HashSet<String>()
        for (i in 0 until arr.length()) {
            val id = arr.getJSONObject(i).optString("stable_id").trim()
            check(id.isNotBlank()) { "معرف stable_id فارغ في قسم $section عند الفهرس $i" }
            check(seen.add(id)) { "معرف stable_id مكرر ($id) في قسم $section" }
        }
        return seen
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val CORE_PACK_KEY = "core_seerah_json"
        const val FALLBACK_PACK_VERSION = "3.0.1-alpha.5"
    }
}
