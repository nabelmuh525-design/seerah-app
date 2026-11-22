package com.seerah.data.importer

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

data class EpubChapter(
    val order: Int,
    val title: String,
    val plainText: String
)

/**
 * محلل EPUB محصّن (v3.0.0-alpha.4.2):
 * - يحسب حجم جميع مدخلات ZIP بما فيها الصور والملفات الثنائية ضد ZIP Bomb.
 * - يمنع Zip Slip على كل مدخل قبل القراءة.
 * - يعطّل DTD وXXE وXInclude وACCESS_EXTERNAL_DTD/SCHEMA إلزاميًا (يرفض التحليل عند فشل الحماية).
 * - يفك ترميز %XX دون تحويل '+' إلى مسافة، ويزيل #fragment و?query.
 */
object EpubOpfParser {

    fun parseChaptersFromZip(zipStream: InputStream): List<EpubChapter> {
        val entries = mutableMapOf<String, String>()
        val virtualRoot = File("/epub_virtual_root")

        ZipInputStream(zipStream).use { zis ->
            var entry = zis.nextEntry
            var count = 0
            var totalBytes = 0L
            while (entry != null) {
                count++
                ZipSecurityValidator.validateQuota(count, totalBytes)
                ZipSecurityValidator.resolveSafeEntryFile(virtualRoot, entry.name)

                if (!entry.isDirectory) {
                    val remaining = ZipSecurityValidator.MAX_TOTAL_UNCOMPRESSED_BYTES - totalBytes
                    val lower = entry.name.lowercase()
                    val isTextXml = lower.endsWith(".xml") || lower.endsWith(".opf") ||
                        lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")

                    if (isTextXml) {
                        val bytes = readBounded(zis, remaining)
                        totalBytes += bytes.size
                        ZipSecurityValidator.validateQuota(count, totalBytes)
                        entries[normalizePath(entry.name)] = bytes.toString(Charsets.UTF_8)
                    } else {
                        // احتساب حجم الصور والملفات الثنائية أيضًا لمنع تهريب ZIP Bomb
                        val skippedBytes = drainAndCountBounded(zis, remaining)
                        totalBytes += skippedBytes
                        ZipSecurityValidator.validateQuota(count, totalBytes)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val containerXml = entries["META-INF/container.xml"]
        val opfPath = if (containerXml != null) {
            parseContainerOpfPath(containerXml)
        } else {
            entries.keys.firstOrNull { it.endsWith(".opf", ignoreCase = true) }
        }

        val orderedPaths = if (opfPath != null && entries.containsKey(normalizePath(opfPath))) {
            resolveSpineOrderXml(normalizePath(opfPath), entries[normalizePath(opfPath)].orEmpty())
        } else {
            entries.keys.filter { it.endsWith(".xhtml", true) || it.endsWith(".html", true) }.sorted()
        }

        val chapters = mutableListOf<EpubChapter>()
        orderedPaths.forEachIndexed { idx, path ->
            val rawHtml = entries[normalizePath(path)] ?: return@forEachIndexed
            val title = extractHeading(rawHtml) ?: path.substringAfterLast('/')
            val plain = stripHtml(rawHtml)
            if (plain.isNotBlank()) {
                chapters.add(EpubChapter(order = idx + 1, title = title, plainText = plain))
            }
        }
        return chapters
    }

    private fun readBounded(input: InputStream, maxBytes: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var written = 0L
        var n = input.read(buf)
        while (n != -1) {
            written += n
            if (written > maxBytes) {
                throw SecurityException("تجاوز حجم الملف داخل EPUB الحد الأقصى المسموح به (حماية ZIP Bomb).")
            }
            out.write(buf, 0, n)
            n = input.read(buf)
        }
        return out.toByteArray()
    }

    private fun drainAndCountBounded(input: InputStream, maxBytes: Long): Long {
        val buf = ByteArray(8192)
        var total = 0L
        var n = input.read(buf)
        while (n != -1) {
            total += n
            if (total > maxBytes) {
                throw SecurityException("تجاوز حجم الملفات الثنائية/الصور داخل EPUB الحد الأقصى (حماية ZIP Bomb).")
            }
            n = input.read(buf)
        }
        return total
    }

    private fun secureXmlBuilder(): javax.xml.parsers.DocumentBuilder {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            runCatching { factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, systemId ->
                throw SecurityException("تم منع الوصول إلى كيان XML خارجي: $systemId")
            }
            return builder
        } catch (e: Exception) {
            throw SecurityException("تعذر تفعيل إعدادات حماية XML/XXE الإلزامية: ${e.message}", e)
        }
    }

    internal fun parseContainerOpfPath(containerXml: String): String? {
        val doc = secureXmlBuilder().parse(ByteArrayInputStream(containerXml.toByteArray(Charsets.UTF_8)))
        val nodes = doc.getElementsByTagName("rootfile")
        val raw = if (nodes.length > 0) (nodes.item(0) as? Element)?.getAttribute("full-path") else null
        return raw?.takeIf { it.isNotBlank() }?.let { decodePercentPath(stripQueryAndFragment(it)) }
    }

    internal fun resolveSpineOrderXml(opfPath: String, opfXml: String): List<String> {
        val baseDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") else ""
        val doc = secureXmlBuilder().parse(ByteArrayInputStream(opfXml.toByteArray(Charsets.UTF_8)))

        val manifest = mutableMapOf<String, String>()
        val items = doc.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val el = items.item(i) as? Element ?: continue
            val id = el.getAttribute("id")
            val href = el.getAttribute("href")
            if (id.isNotBlank() && href.isNotBlank()) {
                val cleaned = decodePercentPath(stripQueryAndFragment(href))
                val resolved = resolveRelativeStrict(baseDir, cleaned)
                manifest[id] = resolved
            }
        }

        val spine = mutableListOf<String>()
        val refs = doc.getElementsByTagName("itemref")
        for (i in 0 until refs.length) {
            val el = refs.item(i) as? Element ?: continue
            val idref = el.getAttribute("idref")
            manifest[idref]?.let { spine.add(it) }
        }
        return spine
    }

    internal fun stripQueryAndFragment(href: String): String =
        href.substringBefore("#").substringBefore("?")

    /**
     * فك ترميز %XX دون تحويل علامة '+' إلى مسافة (يحافظ على أسماء الملفات مثل chapter+1.xhtml).
     */
    internal fun decodePercentPath(path: String): String {
        val out = ByteArrayOutputStream(path.length)
        var i = 0
        val bytes = path.toByteArray(Charsets.UTF_8)
        while (i < bytes.size) {
            val b = bytes[i]
            if (b == '%'.code.toByte() && i + 2 < bytes.size) {
                val hex = String(bytes, i + 1, 2, Charsets.US_ASCII)
                val parsed = hex.toIntOrNull(16)
                if (parsed != null) {
                    out.write(parsed)
                    i += 3
                    continue
                }
            }
            out.write(b.toInt())
            i++
        }
        return out.toString("UTF-8")
    }

    private fun resolveRelativeStrict(baseDir: String, href: String): String {
        val combined = if (baseDir.isBlank()) href else "$baseDir/$href"
        val parts = mutableListOf<String>()
        combined.replace("\\", "/").split("/").forEach { seg ->
            when (seg) {
                "", "." -> {}
                ".." -> {
                    if (parts.isEmpty()) throw SecurityException("مسار EPUB يتجاوز جذر الأرشيف: $href")
                    parts.removeAt(parts.lastIndex)
                }
                else -> parts.add(seg)
            }
        }
        return parts.joinToString("/")
    }

    private fun normalizePath(path: String): String = resolveRelativeStrict("", path)

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