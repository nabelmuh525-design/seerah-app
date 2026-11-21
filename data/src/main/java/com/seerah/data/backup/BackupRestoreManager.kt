package com.seerah.data.backup

import android.content.Context
import android.net.Uri
import com.seerah.database.SeerahDatabase
import com.seerah.database.entity.FavoriteEntity
import com.seerah.database.entity.UserNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * مدير النسخ الاحتياطي والاستعادة لبيانات المستخدم (المفضلة والملاحظات) بصيغة JSON محلية.
 */
class BackupRestoreManager(
    private val context: Context,
    private val database: SeerahDatabase
) {
    suspend fun exportUserDataToUri(targetUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val favorites = database.userDataDao().observeFavorites().first()
            val notes = database.userDataDao().observeNotes().first()

            val root = JSONObject().apply {
                put("app", "SEERAH")
                put("schema_version", 1)
                put("exported_at", System.currentTimeMillis())
                put("favorites", JSONArray().apply {
                    favorites.forEach { fav ->
                        put(JSONObject().apply {
                            put("item_type", fav.itemType)
                            put("item_id", fav.itemId)
                            put("title", fav.title)
                            put("subtitle", fav.subtitle)
                            put("created_at", fav.createdAtEpochMs)
                        })
                    }
                })
                put("notes", JSONArray().apply {
                    notes.forEach { note ->
                        put(JSONObject().apply {
                            put("item_type", note.itemType)
                            put("item_id", note.itemId)
                            put("title", note.title)
                            put("content", note.content)
                            put("updated_at", note.updatedAtEpochMs)
                        })
                    }
                })
            }

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("تعذر فتح مسار حفظ النسخة الاحتياطية")

            "تم تصدير ${favorites.size} من المفضلة و${notes.size} ملاحظة بنجاح."
        }
    }

    suspend fun importUserDataFromUri(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonStr = context.contentResolver.openInputStream(sourceUri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.readText()
                ?: error("تعذر قراءة ملف النسخة الاحتياطية")

            val root = JSONObject(jsonStr)
            var favCount = 0
            var noteCount = 0

            root.optJSONArray("favorites")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    database.userDataDao().upsertFavorite(
                        FavoriteEntity(
                            itemType = o.optString("item_type"),
                            itemId = o.optLong("item_id"),
                            title = o.optString("title"),
                            subtitle = o.optString("subtitle"),
                            createdAtEpochMs = o.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                    favCount++
                }
            }

            root.optJSONArray("notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    database.userDataDao().upsertNote(
                        UserNoteEntity(
                            itemType = o.optString("item_type"),
                            itemId = o.optLong("item_id"),
                            title = o.optString("title"),
                            content = o.optString("content"),
                            updatedAtEpochMs = o.optLong("updated_at", System.currentTimeMillis())
                        )
                    )
                    noteCount++
                }
            }

            "تمت استعادة $favCount من المفضلة و$noteCount ملاحظة بنجاح دون حذف البيانات الحالية."
        }
    }
}
