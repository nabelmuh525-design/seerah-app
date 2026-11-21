package com.seerah.data.seed

import com.seerah.core.text.ArabicNormalizer
import com.seerah.database.SeerahDatabase
import com.seerah.database.entity.BookEntity
import com.seerah.database.entity.DownloadEntity
import com.seerah.database.entity.GeographicLayerEntity
import com.seerah.database.entity.SourceEntity
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.MapLayerType

/**
 * مهيّئ البنية المرجعية الأولية لقاعدة البيانات.
 * يلتزم بالضوابط العلمية الصارمة:
 * - يسجّل فقط عناوين المراجع المستهدفة الـ 22 كـ "بانتظار استيراد الملف من المستخدم" (isImported = false).
 * - يسجّل طبقات الأطلس الـ 10 وحزم الخرائط الـ 11 كـ "غير مثبتة" (NOT_INSTALLED).
 * - لا يخترع أي أحداث أو روايات أو إحداثيات أو أحكام حديثية.
 */
class SeerahCatalogSeeder(private val database: SeerahDatabase) {

    suspend fun seedCatalogIfNeeded() {
        seedGeographicLayers()
        seedCanonicalLibraryCatalog()
        seedOfflinePackageCatalog()
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
                    verificationNote = if (isPrimary) "مصدر أصلي — بانتظار استيراد النسخة المحققة" else "دراسة حديثة — تُفصل عن المصادر الأصلية"
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
                    title = "حزمة خريطة ${mapDef.arabicTitle} (MBTiles / GeoJSON)",
                    packageType = if (mapDef.isBattleMap) "GEOJSON_PACK" else "MAP_MBTILES",
                    status = "NOT_INSTALLED"
                )
            )
        }
    }
}
