package com.seerah.data.seed

import android.content.Context
import com.seerah.core.text.ArabicNormalizer
import com.seerah.database.SeerahDatabase
import com.seerah.database.entity.*
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.MapLayerType
import org.json.JSONObject

/**
 * مهيّئ الموسوعة الكاملة الموثّقة لتطبيق سِيرَة v2.1.0.
 * يقرأ حزمة السيرة النبوية الشاملة (46 حدثًا عبر 20 مرحلة، 17 موقعًا، 22 شخصية،
 * والآيات والأحاديث المرتبطة) من assets/seerah_encyclopedia_v2.json دون المساس
 * ببيانات المستخدم أو المفضلة أو الكتب المستوردة.
 */
class SeerahCatalogSeeder(
    private val context: Context,
    private val database: SeerahDatabase
) {

    suspend fun seedCatalogIfNeeded() {
        seedGeographicLayers()
        seedCanonicalLibraryCatalog()
        seedOfflinePackageCatalog()
        seedFullEncyclopediaFromAssets()
    }

    private suspend fun seedGeographicLayers() {
        if (database.atlasGeoDao().countLayers() > 0) return
        MapLayerType.entries.forEach { layer ->
            database.atlasGeoDao().insertLayer(
                GeographicLayerEntity(
                    layerKey = layer.key,
                    displayName = layer.arabicTitle,
                    isEnabledByDefault = true,
                    zIndex = layer.zIndex
                )
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
                    verificationNote = if (isPrimary) "مصدر أصلي معتمد" else "دراسة حديثة معتمدة"
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
                    status = "READY_LOCAL"
                )
            )
        }
    }

    private suspend fun seedFullEncyclopediaFromAssets() {
        // إذا كانت القاعدة تحتوي بالفعل على الموسوعة الشاملة (40+ حدثًا) نتوقف دون تكرار
        if (database.eventDao().countEvents() >= 40) return

        val rawJson = runCatching {
            context.assets.open("seerah_encyclopedia_v2.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return

        val root = JSONObject(rawJson)

        // 1. الأحداث الـ 46 عبر الـ 20 مرحلة
        root.optJSONArray("events")?.let { arr ->
            var prevId: Long? = null
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val title = o.optString("title")
                val summary = o.optString("short_summary")
                val source = o.optString("source_citation")
                val id = database.eventDao().insertEvent(
                    EventEntity(
                        title = title,
                        normalizedTitle = ArabicNormalizer.normalize(title),
                        stageCode = o.optString("stage_code"),
                        eventType = o.optString("event_type"),
                        chronologicalOrder = o.optInt("chronological_order", i + 1),
                        shortSummary = summary,
                        causeText = o.optString("cause_text"),
                        outcomeText = o.optString("outcome_text"),
                        mapId = o.optString("map_id"),
                        previousEventId = prevId,
                        verificationStatus = "VERIFIED"
                    )
                )
                database.eventDao().insertEventDate(
                    EventDateEntity(
                        eventId = id,
                        hijriDisplay = o.optString("hijri_date"),
                        gregorianDisplay = o.optString("gregorian_date"),
                        isDisputed = o.optBoolean("date_disputed", false),
                        sourceCitation = source
                    )
                )
                database.eventDao().insertNarration(
                    NarrationEntity(
                        eventId = id,
                        sourceName = source,
                        narrationText = summary,
                        normalizedText = ArabicNormalizer.normalize(summary),
                        gradeStatus = if (source.contains("صحيح")) "SAHIH" else "HISTORICAL_UNGRADED",
                        gradeReference = source
                    )
                )
                prevId = id
            }
        }

        // 2. الأماكن التاريخية الـ 17
        root.optJSONArray("places")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name")
                val placeId = database.atlasGeoDao().insertPlace(
                    PlaceEntity(
                        name = name,
                        normalizedName = ArabicNormalizer.normalize(name),
                        historicalName = o.optString("historical_name"),
                        description = o.optString("description"),
                        historicalSignificance = o.optString("historical_significance"),
                        certaintyLevel = o.optString("certainty_level", "CONFIRMED"),
                        mapId = o.optString("map_id", "arabia")
                    )
                )
                database.atlasGeoDao().insertPlaceCoordinate(
                    PlaceCoordinateEntity(
                        placeId = placeId,
                        latitude = o.optDouble("latitude"),
                        longitude = o.optDouble("longitude"),
                        accuracyNote = "إحداثيات مرجعية موثقة",
                        sourceCitation = o.optString("source_citation")
                    )
                )
            }
        }

        // 3. الشخصيات الـ 22 عبر التصنيفات التسعة
        root.optJSONArray("people")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name")
                database.peopleDao().insertPerson(
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
            }
        }

        // 4. الآيات القرآنية المرتبطة بالسيرة
        root.optJSONArray("quran_verses")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val uthmani = o.optString("uthmani_text")
                database.quranHadithDao().insertVerse(
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
                        asbabVerified = o.optBoolean("asbab_verified", true)
                    )
                )
            }
        }

        // 5. الأحاديث الصحيحة المرتبطة بالسيرة
        root.optJSONArray("hadiths")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val text = o.optString("hadith_text")
                val hadithId = database.quranHadithDao().insertHadith(
                    HadithEntity(
                        hadithText = text,
                        normalizedText = ArabicNormalizer.normalize(text),
                        narrator = o.optString("narrator"),
                        sourceName = o.optString("source_name"),
                        bookTitle = o.optString("book_title"),
                        chapterTitle = o.optString("chapter_title"),
                        hadithNumber = o.optString("hadith_number"),
                        takhrij = o.optString("takhrij")
                    )
                )
                database.quranHadithDao().insertHadithGrade(
                    HadithGradeEntity(
                        hadithId = hadithId,
                        gradeCode = o.optString("grade_code", "SAHIH"),
                        gradingScholar = o.optString("grading_scholar"),
                        gradingSource = o.optString("grading_source")
                    )
                )
            }
        }
    }
}
