package com.seerah.data.importer

import java.io.InputStream
import java.util.zip.ZipInputStream

data class EpubChapter(
    val order: Int,
    val title: String,
    val plainText: String
)

/**
 * محلل EPUB حقيقي يقرأ META-INF/container.xml ثم ملف OPF (manifest + spine)
 * لاستخراج الفصول بالترتيب الأصلي للكتاب مع الاحتفاظ بعناوين الفصول.
 */
object EpubOpfParser {

    fun parseChaptersFromZip(zipStream: InputStream): List<EpubChapter> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(zipStream).use { zis ->
            var entry = zis.nextEntry
            var count = 0
            var totalBytes = 0L
            while (entry != null) {
                count++
                ZipSecurityValidator.validateQuota(count, totalBytes)
                if (!entry.isDirectory) {
                    val lower = entry.name.lowercase()
                    if (lower.endsWith(".xml") || lower.endsWith(".opf") || lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) {
                        val bytes = zis.readBytes()
                        totalBytes += bytes.size
                        ZipSecurityValidator.validateQuota(count, totalBytes)
                        entries[entry.name] = bytes.toString(Charsets.UTF_8)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // 1. Locate OPF path from META-INF/container.xml
        val containerXml = entries["META-INF/container.xml"].orEmpty()
        val opfPath = Regex("full-path\\s*=\\s*\"([^\"]+)\"").find(containerXml)?.groupValues?.get(1)
            ?: entries.keys.firstOrNull { it.endsWith(".opf", ignoreCase = true) }

        val orderedPaths = if (opfPath != null && entries.containsKey(opfPath)) {
            resolveSpineOrder(opfPath, entries[opfPath].orEmpty())
        } else {
            entries.keys.filter { it.endsWith(".xhtml", true) || it.endsWith(".html", true) }.sorted()
        }

        val chapters = mutableListOf<EpubChapter>()
        orderedPaths.forEachIndexed { idx, path ->
            val rawHtml = entries[path] ?: return@forEachIndexed
            val title = extractHeading(rawHtml) ?: path.substringAfterLast('/')
            val plain = stripHtml(rawHtml)
            if (plain.isNotBlank()) {
                chapters.add(EpubChapter(order = idx + 1, title = title, plainText = plain))
            }
        }
        return chapters
    }

    internal fun resolveSpineOrder(opfPath: String, opfXml: String): List<String> {
        val baseDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
        val manifest = mutableMapOf<String, String>()
        val itemRegex = Regex("<item\\b[^>]*?>", RegexOption.IGNORE_CASE)
        val idRegex = Regex("\\bid\\s*=\\s*\"([^\"]+)\"")
        val hrefRegex = Regex("\\bhref\\s*=\\s*\"([^\"]+)\"")
        itemRegex.findAll(opfXml).forEach { m ->
            val tag = m.value
            val id = idRegex.find(tag)?.groupValues?.get(1)
            val href = hrefRegex.find(tag)?.groupValues?.get(1)
            if (id != null && href != null) manifest[id] = baseDir + href
        }

        val spine = mutableListOf<String>()
        val itemRefRegex = Regex("<itemref\\b[^>]*?idref\\s*=\\s*\"([^\"]+)\"[^>]*?>", RegexOption.IGNORE_CASE)
        itemRefRegex.findAll(opfXml).forEach { m ->
            val idref = m.groupValues[1]
            manifest[idref]?.let { spine.add(it) }
        }
        return spine
    }

    private fun extractHeading(html: String): String? {
        val h = Regex("<(?:h1|h2|title)[^>]*>([\\s\\S]*?)</(?:h1|h2|title)>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
        return h?.let { stripHtml(it) }?.takeIf { it.isNotBlank() }
    }

    private fun stripHtml(html: String): String = html
        .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
