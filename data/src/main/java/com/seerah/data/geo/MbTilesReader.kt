package com.seerah.data.geo

import android.database.sqlite.SQLiteDatabase
import java.io.Closeable
import java.io.File

/**
 * قارئ محلي لملفات MBTiles (SQLite) لعرض الخرائط التضاريسية والتاريخية دون إنترنت.
 */
class MbTilesReader(private val file: File) : Closeable {
    private var db: SQLiteDatabase? = null

    fun open(): Boolean {
        if (!file.exists() || !file.canRead()) return false
        return try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readMetadata(): Map<String, String> {
        val database = db ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            database.rawQuery("SELECT name, value FROM metadata", null).use { cursor ->
                while (cursor.moveToNext()) {
                    map[cursor.getString(0)] = cursor.getString(1)
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    /**
     * يسترجع بايتات البلاطة (PNG/PBF) مع تحويل إحداثي Y من XYZ إلى TMS القياسي في MBTiles.
     */
    fun getTile(zoom: Int, x: Int, yXyz: Int): ByteArray? {
        val database = db ?: return null
        val tmsY = ((1 shl zoom) - 1) - yXyz
        return try {
            database.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1",
                arrayOf(zoom.toString(), x.toString(), tmsY.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getBlob(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun close() {
        try {
            db?.close()
        } catch (_: Exception) {
        }
        db = null
    }
}
