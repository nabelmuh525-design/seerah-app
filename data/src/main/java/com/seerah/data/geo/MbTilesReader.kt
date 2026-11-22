package com.seerah.data.geo

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.Closeable
import java.io.File
import kotlin.math.*

/**
 * قارئ محلي لملفات MBTiles (SQLite) مع دعم TMS/XYZ والتخزين المؤقت (LRU Cache)
 * لحساب البلاطات المرئية فقط دون تحميل كامل الخريطة إلى الذاكرة.
 */
class MbTilesReader(private val file: File) : Closeable {
    private var db: SQLiteDatabase? = null
    private var tileScheme: String = "tms"

    private val bitmapCache = object : LruCache<String, Bitmap>(64) {}

    fun open(): Boolean {
        if (!file.exists() || !file.canRead()) return false
        return try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val meta = readMetadata()
            tileScheme = meta["scheme"]?.lowercase() ?: "tms"
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

    fun getTile(zoom: Int, x: Int, yXyz: Int): ByteArray? {
        val database = db ?: return null
        val rowY = if (tileScheme == "xyz") yXyz else xyzToTmsY(zoom, yXyz)
        return try {
            database.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1",
                arrayOf(zoom.toString(), x.toString(), rowY.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getBlob(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getTileBitmapCached(zoom: Int, x: Int, yXyz: Int): Bitmap? {
        val key = "$zoom/$x/$yXyz"
        bitmapCache.get(key)?.let { return it }
        val bytes = getTile(zoom, x, yXyz) ?: return null
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        bitmapCache.put(key, bmp)
        return bmp
    }

    override fun close() {
        bitmapCache.evictAll()
        try { db?.close() } catch (_: Exception) {}
        db = null
    }

    companion object {
        fun xyzToTmsY(zoom: Int, yXyz: Int): Int = ((1 shl zoom) - 1) - yXyz
        fun tmsToXyzY(zoom: Int, yTms: Int): Int = ((1 shl zoom) - 1) - yTms

        fun latLonToTileXY(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
            val n = (1 shl zoom).toDouble()
            val x = floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, (1 shl zoom) - 1)
            val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
            val y = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
                .coerceIn(0, (1 shl zoom) - 1)
            return x to y
        }
    }
}
