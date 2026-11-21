package com.seerah.data.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.seerah.data.importer.BookImportEngine
import com.seerah.database.SeerahDatabase

/**
 * عامل خلفي عبر WorkManager لاستيراد الكتب الكبيرة وإعادة الفهرسة دون تجميد الواجهة.
 */
class LibraryImportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_FILE_URI) ?: return Result.failure()
        val isPrimary = inputData.getBoolean(KEY_IS_PRIMARY, true)
        val author = inputData.getString(KEY_AUTHOR) ?: "مؤلف غير محدد — يحتاج مراجعة"
        val edition = inputData.getString(KEY_EDITION) ?: ""
        val investigator = inputData.getString(KEY_INVESTIGATOR) ?: ""

        val db = SeerahDatabase.build(applicationContext)
        return try {
            val engine = BookImportEngine(applicationContext, db)
            val report = engine.importFromUri(
                uri = Uri.parse(uriString),
                isPrimarySource = isPrimary,
                authorName = author,
                editionName = edition,
                investigator = investigator
            )
            if (report.success) {
                Result.success(workDataOf(KEY_RESULT_MESSAGE to report.message))
            } else {
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to report.message))
            }
        } finally {
            db.close()
        }
    }

    companion object {
        const val KEY_FILE_URI = "file_uri"
        const val KEY_IS_PRIMARY = "is_primary"
        const val KEY_AUTHOR = "author"
        const val KEY_EDITION = "edition"
        const val KEY_INVESTIGATOR = "investigator"
        const val KEY_RESULT_MESSAGE = "result_message"
    }
}
