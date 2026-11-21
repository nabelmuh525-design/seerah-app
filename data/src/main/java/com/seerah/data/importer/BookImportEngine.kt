package com.seerah.data.importer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.seerah.core.text.ArabicNormalizer
import com.seerah.database.SeerahDatabase
import com.seerah.database.entity.*
import com.seerah.domain.model.ImportFormat
import com.seerah.domain.model.ImportReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * محرك استيراد وفهرسة الكتب والمصادر وحزم البيانات الجغرافية دون إنترنت.
 * يدعم: PDF, TXT, EPUB, XML, JSON/GeoJSON, CSV, SQLite/MBTiles, ZIP.
 * يتحقق من التكرار عبر SHA-256، ويضع وسم المراجعة الآلية على ملفات PDF المصوّرة.
 */
class BookImportEngine(
    private val context: Context,
    private val database: SeerahDatabase
) {

    suspend fun importFromUri(
        uri: Uri,
        isPrimarySource: Boolean = true,
        authorName: String = "مؤلف غير محدد — يحتاج مراجعة",
        editionName: String = "",
        investigator: String = ""
    ): ImportReport = withContext(Dispatchers.IO) {
        val fileName = resolveDisplayName(uri) ?: "imported_source_${System.currentTimeMillis()}"
        val format = ImportFormat.fromFileName(fileName)
            ?: return@withContext ImportReport(
                success = false,
                fileName = fileName,
                format = "UNKNOWN",
                message = "صيغة الملف غير مدعومة. الصيغ المدعومة: PDF, TXT, EPUB, XML, JSON, CSV, SQLite, ZIP."
            )

        val storageDir = File(context.filesDir, "imported_library").apply { mkdirs() }
        val localFile = File(storageDir, "${System.currentTimeMillis()}_${fileName.replace(" ", "_")}")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output -> input.copyTo(output) }
            } ?: return@withContext ImportReport(
                success = false,
                fileName = fileName,
                format = format.label,
                message = "تعذر قراءة الملف المختار من الهاتف."
            )

            val hash = sha256(localFile)
            val existing = database.sourceLibraryDao().findBookByHash(hash)
            if (existing != null) {
                localFile.delete()
                return@withContext ImportReport(
                    success = true,
                    fileName = fileName,
                    format = format.label,
                    isDuplicate = true,
                    message = "الملف مستورد مسبقًا في المكتبة باسم: ${existing.title} (تم منع التكرار)."
                )
            }

            when (format) {
                ImportFormat.TXT -> importPlainTextBook(localFile, fileName, hash, isPrimarySource, authorName, editionName, investigator)
                ImportFormat.EPUB -> importEpubBook(localFile, fileName, hash, isPrimarySource, authorName, editionName, investigator)
                ImportFormat.XML -> importXmlBook(localFile, fileName, hash, isPrimarySource, authorName, editionName, investigator)
                ImportFormat.CSV -> importCsvBook(localFile, fileName, hash, isPrimarySource, authorName, editionName, investigator)
                ImportFormat.JSON -> importJsonOrGeoJson(localFile, fileName, hash, isPrimarySource, authorName, editionName, investigator)
                ImportFormat.PDF -> importPdfBook(localFile, fileName, hash, isPrimarySource, authorName, editionName, investigator)
                ImportFormat.SQLITE -> importSqliteOrMbTiles(localFile, fileName, hash, isPrimarySource, authorName)
                ImportFormat.ZIP -> importZipBundle(localFile, fileName, isPrimarySource, authorName, editionName, investigator)
            }
        } catch (e: Exception) {
            ImportReport(
                success = false,
                fileName = fileName,
                format = format.label,
                message = "حدث خطأ أثناء الاستيراد: ${e.message ?: "خطأ غير معروف"}"
            )
        }
    }

    private suspend fun importPlainTextBook(
        file: File,
        fileName: String,
        hash: String,
        isPrimary: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport {
        val text = file.readText(Charsets.UTF_8)
        val chunks = chunkIntoPages(text, charsPerPage = 2200)
        return persistBookWithPages(
            title = fileName.substringBeforeLast('.'),
            authorName = authorName,
            format = "TXT",
            filePath = file.absolutePath,
            hash = hash,
            isPrimary = isPrimary,
            isOcr = false,
            editionName = editionName,
            investigator = investigator,
            pages = chunks
        )
    }

    private suspend fun importEpubBook(
        file: File,
        fileName: String,
        hash: String,
        isPrimary: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport {
        val extractedPages = mutableListOf<Pair<String, String>>()
        ZipInputStream(file.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val lower = entry.name.lowercase()
                if (!entry.isDirectory && (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm"))) {
                    val rawHtml = BufferedReader(InputStreamReader(zis, Charsets.UTF_8)).readText()
                    val plain = rawHtml
                        .replace(Regex("<script[\s\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                        .replace(Regex("<style[\s\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                        .replace(Regex("<[^>]+>"), " ")
                        .replace(Regex("\s+"), " ")
                        .trim()
                    if (plain.isNotBlank()) {
                        extractedPages.add(entry.name.substringAfterLast('/') to plain)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return persistBookWithPages(
            title = fileName.substringBeforeLast('.'),
            authorName = authorName,
            format = "EPUB",
            filePath = file.absolutePath,
            hash = hash,
            isPrimary = isPrimary,
            isOcr = false,
            editionName = editionName,
            investigator = investigator,
            pages = extractedPages.ifEmpty { listOf("محتوى الكتاب" to "لم يُعثر على نصوص مقروءة داخل ملف EPUB.") }
        )
    }

    private suspend fun importXmlBook(
        file: File,
        fileName: String,
        hash: String,
        isPrimary: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport {
        val pages = mutableListOf<Pair<String, String>>()
        val parser = Xml.newPullParser()
        file.inputStream().use { stream ->
            parser.setInput(stream, "UTF-8")
            var eventType = parser.eventType
            val buffer = StringBuilder()
            var currentTag = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name ?: ""
                    XmlPullParser.TEXT -> {
                        val txt = parser.text?.trim().orEmpty()
                        if (txt.isNotBlank()) {
                            buffer.append(txt).append("
")
                            if (buffer.length >= 2200) {
                                pages.add(currentTag to buffer.toString().trim())
                                buffer.clear()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            if (buffer.isNotBlank()) {
                pages.add(currentTag to buffer.toString().trim())
            }
        }
        return persistBookWithPages(
            title = fileName.substringBeforeLast('.'),
            authorName = authorName,
            format = "XML",
            filePath = file.absolutePath,
            hash = hash,
            isPrimary = isPrimary,
            isOcr = false,
            editionName = editionName,
            investigator = investigator,
            pages = pages
        )
    }

    private suspend fun importCsvBook(
        file: File,
        fileName: String,
        hash: String,
        isPrimary: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport {
        val lines = file.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        val pages = lines.chunked(30).mapIndexed { idx, block ->
            "مجموعة سجلات ${idx + 1}" to block.joinToString("
")
        }
        return persistBookWithPages(
            title = fileName.substringBeforeLast('.'),
            authorName = authorName,
            format = "CSV",
            filePath = file.absolutePath,
            hash = hash,
            isPrimary = isPrimary,
            isOcr = false,
            editionName = editionName,
            investigator = investigator,
            pages = pages
        )
    }

    private suspend fun importPdfBook(
        file: File,
        fileName: String,
        hash: String,
        isPrimary: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport {
        // فحص وجود نصوص مدمجة داخل PDF؛ وإن كان الملف مصوّرًا يُوسَم للمراجعة الآلية (OCR)
        val rawAscii = file.readBytes().toString(Charsets.ISO_8859_1)
        val hasTextOperators = rawAscii.contains("BT") && rawAscii.contains("Tj")
        val extractedNotice = if (hasTextOperators) {
            "تم حفظ ملف PDF في المكتبة المحلية. لاستخراج النص العربي الكامل بدقة عالية يُنصح بتفعيل وحدة OCR أو استيراد نسخة نصية (EPUB/JSON/TXT)."
        } else {
            "ملف PDF مصوّر (Scanned): تم وسمه تلقائيًا بأن النص يحتاج استخراجًا آليًا (OCR) ومراجعة علمية قبل الاعتماد."
        }
        return persistBookWithPages(
            title = fileName.substringBeforeLast('.'),
            authorName = authorName,
            format = "PDF",
            filePath = file.absolutePath,
            hash = hash,
            isPrimary = isPrimary,
            isOcr = true,
            editionName = editionName,
            investigator = investigator,
            pages = listOf("صفحة الفهرسة الأولية" to extractedNotice)
        )
    }

    private suspend fun importJsonOrGeoJson(
        file: File,
        fileName: String,
        hash: String,
        isPrimary: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport {
        val raw = file.readText(Charsets.UTF_8).trim()
        if (raw.startsWith("{")) {
            val root = JSONObject(raw)
            // 1. GeoJSON Map Pack
            if (root.optString("type") == "FeatureCollection") {
                val mapId = root.optString("map_id", fileName.substringBeforeLast('.').lowercase())
                val features = com.seerah.data.geo.GeoJsonParser.parseFeatureCollection(raw)
                features.forEach { f ->
                    database.atlasGeoDao().insertBattleFeature(
                        BattleMapFeatureEntity(
                            battleMapId = mapId,
                            phaseId = null,
                            layerKey = f.layerKey,
                            featureType = f.featureType,
                            label = f.label,
                            geojsonGeometry = raw,
                            certaintyLevel = f.certainty.code,
                            sourceCitation = f.sourceCitation
                        )
                    )
                }
                database.userDataDao().upsertDownload(
                    DownloadEntity(
                        packageKey = "geojson_$mapId",
                        title = "حزمة خريطة GeoJSON: $fileName",
                        packageType = "GEOJSON_PACK",
                        sizeBytes = file.length(),
                        status = "IMPORTED",
                        localPath = file.absolutePath,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
                return ImportReport(
                    success = true,
                    fileName = fileName,
                    format = "GeoJSON",
                    importedRecordsCount = features.size,
                    message = "تم استيراد ${features.size} معلمًا جغرافيًا إلى الأطلس التاريخي ($mapId)."
                )
            }

            // 2. Structured Seerah Pack JSON (events, people, places, narrations, hadiths, verses, pages)
            var count = 0
            root.optJSONArray("events")?.let { arr -> count += importStructuredEvents(arr) }
            root.optJSONArray("people")?.let { arr -> count += importStructuredPeople(arr) }
            root.optJSONArray("places")?.let { arr -> count += importStructuredPlaces(arr) }
            root.optJSONArray("narrations")?.let { arr -> count += importStructuredNarrations(arr) }
            root.optJSONArray("hadiths")?.let { arr -> count += importStructuredHadiths(arr) }
            root.optJSONArray("quran_verses")?.let { arr -> count += importStructuredVerses(arr) }
            if (count > 0) {
                return ImportReport(
                    success = true,
                    fileName = fileName,
                    format = "JSON",
                    importedRecordsCount = count,
                    message = "تم استيراد $count سجلًا موثقًا إلى قاعدة بيانات سِيرَة بنجاح."
                )
            }
        }

        // Fallback: store JSON text as searchable book pages
        val chunks = chunkIntoPages(raw, charsPerPage = 2400)
        return persistBookWithPages(
            title = fileName.substringBeforeLast('.'),
            authorName = authorName,
            format = "JSON",
            filePath = file.absolutePath,
            hash = hash,
            isPrimary = isPrimary,
            isOcr = false,
            editionName = editionName,
            investigator = investigator,
            pages = chunks
        )
    }

    private suspend fun importSqliteOrMbTiles(
        file: File,
        fileName: String,
        hash: String,
        isPrimary: Boolean,
        authorName: String
    ): ImportReport {
        val lower = fileName.lowercase()
        if (lower.endsWith(".mbtiles")) {
            database.userDataDao().upsertDownload(
                DownloadEntity(
                    packageKey = "mbtiles_${fileName.substringBeforeLast('.')}",
                    title = "حزمة خريطة MBTiles: $fileName",
                    packageType = "MAP_MBTILES",
                    sizeBytes = file.length(),
                    status = "IMPORTED",
                    localPath = file.absolutePath,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
            return ImportReport(
                success = true,
                fileName = fileName,
                format = "MBTiles",
                importedRecordsCount = 1,
                message = "تم تسجيل حزمة الخريطة المحلية MBTiles بنجاح للاستخدام دون إنترنت."
            )
        }

        val pages = mutableListOf<Pair<String, String>>()
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { extDb ->
            extDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { tables ->
                while (tables.moveToNext()) {
                    val tableName = tables.getString(0)
                    if (tableName.startsWith("sqlite_") || tableName == "android_metadata") continue
                    extDb.rawQuery("SELECT * FROM "$tableName" LIMIT 200", null).use { rows ->
                        val colCount = rows.columnCount
                        while (rows.moveToNext()) {
                            val line = (0 until colCount).mapNotNull { idx ->
                                rows.getString(idx)?.takeIf { it.isNotBlank() }
                            }.joinToString(" | ")
                            if (line.isNotBlank()) pages.add(tableName to line)
                        }
                    }
                }
            }
        }
        return persistBookWithPages(
            title = fileName.substringBeforeLast('.'),
            authorName = authorName,
            format = "SQLITE",
            filePath = file.absolutePath,
            hash = hash,
            isPrimary = isPrimary,
            isOcr = false,
            editionName = "",
            investigator = "",
            pages = pages
        )
    }

    private suspend fun importZipBundle(
        file: File,
        fileName: String,
        isPrimary: Boolean,
        authorName: String,
        editionName: String,
        investigator: String
    ): ImportReport {
        val extractDir = File(context.filesDir, "imported_library/zip_${System.currentTimeMillis()}").apply { mkdirs() }
        var totalRecords = 0
        var totalPages = 0
        ZipInputStream(file.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name.substringAfterLast('/')
                    val outFile = File(extractDir, entryName)
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    val fmt = ImportFormat.fromFileName(entryName)
                    if (fmt == ImportFormat.TXT) {
                        val rep = importPlainTextBook(outFile, entryName, sha256(outFile), isPrimary, authorName, editionName, investigator)
                        totalPages += rep.importedPagesCount
                    } else if (fmt == ImportFormat.JSON) {
                        val rep = importJsonOrGeoJson(outFile, entryName, sha256(outFile), isPrimary, authorName, editionName, investigator)
                        totalRecords += rep.importedRecordsCount
                        totalPages += rep.importedPagesCount
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return ImportReport(
            success = true,
            fileName = fileName,
            format = "ZIP",
            importedPagesCount = totalPages,
            importedRecordsCount = totalRecords,
            message = "تم فك حزمة ZIP واستيراد $totalPages صفحة و$totalRecords سجلًا بنجاح."
        )
    }

    private suspend fun persistBookWithPages(
        title: String,
        authorName: String,
        format: String,
        filePath: String,
        hash: String,
        isPrimary: Boolean,
        isOcr: Boolean,
        editionName: String,
        investigator: String,
        pages: List<Pair<String, String>>
    ): ImportReport {
        val bookId = database.sourceLibraryDao().insertBook(
            BookEntity(
                title = title,
                normalizedTitle = ArabicNormalizer.normalize(title),
                authorName = authorName,
                sourceCategory = if (isPrimary) "PRIMARY_SEERAH" else "MODERN_STUDY",
                isPrimarySource = isPrimary,
                fileFormat = format,
                localFilePath = filePath,
                fileHash = hash,
                isImported = true,
                isOcrExtracted = isOcr,
                needsReview = isOcr,
                pageCount = pages.size,
                importedAtEpochMs = System.currentTimeMillis(),
                importStatus = "IMPORTED"
            )
        )
        if (editionName.isNotBlank() || investigator.isNotBlank()) {
            database.sourceLibraryDao().insertEdition(
                EditionEntity(
                    bookId = bookId,
                    editionName = editionName.ifBlank { "طبعة مستوردة" },
                    investigator = investigator
                )
            )
        }
        val pageEntities = pages.mapIndexed { idx, (chapter, content) ->
            SourcePageEntity(
                bookId = bookId,
                volumeNumber = 1,
                pageNumber = idx + 1,
                chapterTitle = chapter,
                rawText = content,
                normalizedText = ArabicNormalizer.normalize(content),
                isOcrGenerated = isOcr
            )
        }
        database.sourceLibraryDao().insertPages(pageEntities)
        return ImportReport(
            success = true,
            fileName = title,
            format = format,
            isOcrFlagged = isOcr,
            importedPagesCount = pageEntities.size,
            message = if (isOcr) {
                "تم استيراد «$title» (${pageEntities.size} صفحة) مع وسم المراجعة الآلية (OCR)."
            } else {
                "تم استيراد «$title» وفهرسة ${pageEntities.size} صفحة في محرك البحث المحلي."
            }
        )
    }

    private suspend fun importStructuredEvents(arr: JSONArray): Int {
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue
            val eventId = database.eventDao().insertEvent(
                EventEntity(
                    title = title,
                    normalizedTitle = ArabicNormalizer.normalize(title),
                    stageCode = o.optString("stage_code", "LINEAGE_AND_BIRTH"),
                    eventType = o.optString("event_type", "GENERAL"),
                    chronologicalOrder = o.optInt("chronological_order", i + 1),
                    shortSummary = o.optString("short_summary", ""),
                    detailedAnalysis = o.optString("detailed_analysis", ""),
                    causeText = o.optString("cause_text", ""),
                    preparationText = o.optString("preparation_text", ""),
                    outcomeText = o.optString("outcome_text", ""),
                    mapId = o.optString("map_id").takeIf { it.isNotBlank() },
                    verificationStatus = o.optString("verification_status", "UNVERIFIED")
                )
            )
            val dateStr = o.optString("hijri_date")
            if (dateStr.isNotBlank()) {
                database.eventDao().insertEventDate(
                    EventDateEntity(
                        eventId = eventId,
                        hijriDisplay = dateStr,
                        gregorianDisplay = o.optString("gregorian_date", ""),
                        isDisputed = o.optBoolean("date_disputed", false),
                        sourceCitation = o.optString("source_citation", "")
                    )
                )
            }
            count++
        }
        return count
    }

    private suspend fun importStructuredPeople(arr: JSONArray): Int {
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").takeIf { it.isNotBlank() } ?: continue
            database.peopleDao().insertPerson(
                PersonEntity(
                    name = name,
                    normalizedName = ArabicNormalizer.normalize(name),
                    kunyah = o.optString("kunyah", ""),
                    lineage = o.optString("lineage", ""),
                    category = o.optString("category", "OTHER"),
                    historicalBio = o.optString("historical_bio", ""),
                    roleInSeerah = o.optString("role_in_seerah", ""),
                    primarySources = o.optString("primary_sources", "")
                )
            )
            count++
        }
        return count
    }

    private suspend fun importStructuredPlaces(arr: JSONArray): Int {
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").takeIf { it.isNotBlank() } ?: continue
            val placeId = database.atlasGeoDao().insertPlace(
                PlaceEntity(
                    name = name,
                    normalizedName = ArabicNormalizer.normalize(name),
                    historicalName = o.optString("historical_name", name),
                    description = o.optString("description", ""),
                    historicalSignificance = o.optString("historical_significance", ""),
                    certaintyLevel = o.optString("certainty_level", "UNVERIFIED"),
                    mapId = o.optString("map_id", "arabia")
                )
            )
            if (o.has("latitude") && o.has("longitude")) {
                database.atlasGeoDao().insertPlaceCoordinate(
                    PlaceCoordinateEntity(
                        placeId = placeId,
                        latitude = o.optDouble("latitude"),
                        longitude = o.optDouble("longitude"),
                        accuracyNote = o.optString("accuracy_note", ""),
                        sourceCitation = o.optString("source_citation", "")
                    )
                )
            }
            count++
        }
        return count
    }

    private suspend fun importStructuredNarrations(arr: JSONArray): Int {
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val text = o.optString("narration_text").takeIf { it.isNotBlank() } ?: continue
            database.eventDao().insertNarration(
                NarrationEntity(
                    eventId = if (o.has("event_id")) o.optLong("event_id") else null,
                    sourceName = o.optString("source_name", "مصدر غير محدد"),
                    authorName = o.optString("author_name", ""),
                    bookName = o.optString("book_name", ""),
                    volume = o.optString("volume", ""),
                    page = o.optString("page", ""),
                    chapter = o.optString("chapter", ""),
                    editionName = o.optString("edition_name", ""),
                    investigator = o.optString("investigator", ""),
                    isnad = o.optString("isnad", ""),
                    narrationText = text,
                    normalizedText = ArabicNormalizer.normalize(text),
                    scholarlyCommentary = o.optString("scholarly_commentary", ""),
                    gradeStatus = o.optString("grade_status", "UNVERIFIED"),
                    gradeReference = o.optString("grade_reference", "")
                )
            )
            count++
        }
        return count
    }

    private suspend fun importStructuredHadiths(arr: JSONArray): Int {
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val text = o.optString("hadith_text").takeIf { it.isNotBlank() } ?: continue
            val hadithId = database.quranHadithDao().insertHadith(
                HadithEntity(
                    eventId = if (o.has("event_id")) o.optLong("event_id") else null,
                    hadithText = text,
                    normalizedText = ArabicNormalizer.normalize(text),
                    narrator = o.optString("narrator", "غير مذكور"),
                    sourceName = o.optString("source_name", "مصدر غير محدد"),
                    bookTitle = o.optString("book_title", ""),
                    chapterTitle = o.optString("chapter_title", ""),
                    hadithNumber = o.optString("hadith_number", ""),
                    takhrij = o.optString("takhrij", ""),
                    isnad = o.optString("isnad", ""),
                    editionInfo = o.optString("edition_info", "")
                )
            )
            val gradeCode = o.optString("grade_code")
            val gradeScholar = o.optString("grading_scholar")
            if (gradeCode.isNotBlank() && gradeScholar.isNotBlank()) {
                database.quranHadithDao().insertHadithGrade(
                    HadithGradeEntity(
                        hadithId = hadithId,
                        gradeCode = gradeCode,
                        gradingScholar = gradeScholar,
                        gradingSource = o.optString("grading_source", "")
                    )
                )
            }
            count++
        }
        return count
    }

    private suspend fun importStructuredVerses(arr: JSONArray): Int {
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val uthmani = o.optString("uthmani_text").takeIf { it.isNotBlank() } ?: continue
            database.quranHadithDao().insertVerse(
                QuranVerseEntity(
                    surahNumber = o.optInt("surah_number", 1),
                    surahName = o.optString("surah_name", ""),
                    ayahNumber = o.optInt("ayah_number", 1),
                    uthmaniText = uthmani,
                    normalizedText = ArabicNormalizer.normalize(uthmani),
                    tafsirSummary = o.optString("tafsir_summary", ""),
                    tafsirSource = o.optString("tafsir_source", ""),
                    asbabAlNuzul = o.optString("asbab_al_nuzul", ""),
                    asbabSource = o.optString("asbab_source", ""),
                    asbabVerified = o.optBoolean("asbab_verified", false)
                )
            )
            count++
        }
        return count
    }

    private fun chunkIntoPages(text: String, charsPerPage: Int): List<Pair<String, String>> {
        if (text.isBlank()) return emptyList()
        val pages = mutableListOf<Pair<String, String>>()
        var start = 0
        var pageNum = 1
        while (start < text.length) {
            val end = (start + charsPerPage).coerceAtMost(text.length)
            pages.add("صفحة $pageNum" to text.substring(start, end).trim())
            start = end
            pageNum++
        }
        return pages
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        } ?: uri.lastPathSegment
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var read = input.read(buf)
            while (read > 0) {
                digest.update(buf, 0, read)
                read = input.read(buf)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
