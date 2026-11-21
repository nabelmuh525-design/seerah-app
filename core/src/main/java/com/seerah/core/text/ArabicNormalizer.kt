package com.seerah.core.text

/**
 * معالج النصوص العربية للبحث الشامل دون إنترنت.
 * يوحّد الهمزات والألف المقصورة والياء والتاء المربوطة ويزيل التشكيل والتطويل
 * لرفع دقة البحث في الأحداث والشخصيات والأماكن والكتب والروايات.
 */
object ArabicNormalizer {
    private val diacriticsRegex = Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]")
    private val multiSpaceRegex = Regex("\s+")

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val stripped = diacriticsRegex.replace(input, "")
        val sb = StringBuilder(stripped.length)
        for (ch in stripped) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ى' -> sb.append('ي')
                'ة' -> sb.append('ه')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return multiSpaceRegex.replace(sb.toString().trim(), " ")
    }

    fun toFtsQuery(input: String): String {
        val normalized = normalize(input)
        if (normalized.isBlank()) return ""
        return normalized
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                val safe = token.replace(""", "").replace("*", "")
                "${safe}*"
            }
    }
}
