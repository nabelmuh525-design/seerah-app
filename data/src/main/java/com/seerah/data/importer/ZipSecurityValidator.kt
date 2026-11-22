package com.seerah.data.importer

import java.io.File

/**
 * حارس أمني ضد هجمات Zip Slip و ZIP Bomb عند استيراد أرشيفات الكتب والخرائط.
 */
object ZipSecurityValidator {
    const val MAX_ZIP_ENTRIES = 500
    const val MAX_TOTAL_UNCOMPRESSED_BYTES = 150L * 1024L * 1024L // 150 MB

    fun resolveSafeEntryFile(targetDir: File, entryName: String): File {
        val canonicalDir = targetDir.canonicalPath
        val candidate = File(targetDir, entryName)
        val canonicalCandidate = candidate.canonicalPath
        if (!canonicalCandidate.startsWith(canonicalDir + File.separator)) {
            throw SecurityException("تم رفض ملف غير آمن (محاولة Zip Slip): $entryName")
        }
        return candidate
    }

    fun validateQuota(entryCount: Int, totalBytesWritten: Long) {
        if (entryCount > MAX_ZIP_ENTRIES) {
            throw SecurityException("تجاوز الأرشيف الحد الأقصى المسموح به للملفات ($MAX_ZIP_ENTRIES).")
        }
        if (totalBytesWritten > MAX_TOTAL_UNCOMPRESSED_BYTES) {
            throw SecurityException("تم إيقاف فك الضغط حمايةً من ZIP Bomb (تجاوز الحجم المسموح 150 ميجابايت).")
        }
    }
}
