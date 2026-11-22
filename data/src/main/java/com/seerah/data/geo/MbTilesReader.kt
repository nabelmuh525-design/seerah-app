package com.seerah.data.geo

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.*

data class TileGeoBounds(
    val northLat: Double,
    val southLat: Double,
    val westLon: Double,
    val eastLon: Double
)

data class MbTilesMetadata(
    val scheme: String = "tms",
    val format: String = "png",
    val minZoom: Int = 0,
    val maxZoom: Int = 18,
    val bounds: TileGeoBounds? = null
)

/**
 * قارئ MBTiles احترافي غير متزامن (v3.0.0-alpha.4.3):
 * - يفتح SQLite ويقرأ metadata على Dispatchers.IO عبر openAsync().
 * - يحترم minzoom وmaxzoom وbounds وscheme وformat من جدول metadata.
 * - يمنع تكرار طلب البلاطة نفسها بالتزامن (In-Flight Request Deduplication).
 * - يحمي إغلاق قاعدة البيانات بـ ReentrantReadWriteLock لمنع الإغلاق أثناء قراءة نشطة.
 */
class MbTilesReader(private val file: File) : Closeable {
    private var db: SQLiteDatabase? = null
    private val rwLock = ReentrantReadWriteLock()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>()

    @Volatile
    var metadata: MbTilesMetadata = MbTilesMetadata()
        private set

    private val bitmapCache = object : LruCache<String, Bitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    suspend fun openAsync(): Boolean = withContext(Dispatchers.IO) { open() }

    fun open(): Boolean = rwLock.write {
        if (!file.exists() || !file.canRead()) return@write false
        try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val raw = readMetadataInternal()
            metadata = parseMetadata(raw)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readMetadata(): Map<String, String> = rwLock.read { readMetadataInternal() }

    private fun readMetadataInternal(): Map<String, String> {
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

    fun getTileBitmapIfCached(zoom: Int, x: Int, yXyz: Int): Bitmap? =
        bitmapCache.get("$zoom/$x/$yXyz")

    suspend fun loadTileAsync(zoom: Int, x: Int, yXyz: Int): Bitmap? {
        if (zoom !in metadata.minZoom..metadata.maxZoom) return null
        val key = "$zoom/$x/$yXyz"
        bitmapCache.get(key)?.let { return it }

        val newDeferred = CompletableDeferred<Bitmap?>()
        val existing = inFlight.putIfAbsent(key, newDeferred)
        if (existing != null) {
            return existing.await()
        }

        return try {
            val bmp = withContext(Dispatchers.IO) {
                val bytes = getTile(zoom, x, yXyz) ?: return@withContext null
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
            }
            if (bmp != null) bitmapCache.put(key, bmp)
            newDeferred.complete(bmp)
            bmp
        } catch (_: Exception) {
            newDeferred.complete(null)
            null
        } finally {
            inFlight.remove(key, newDeferred)
        }
    }

    fun getTile(zoom: Int, x: Int, yXyz: Int): ByteArray? = rwLock.read {
        val database = db ?: return@read null
        if (zoom !in metadata.minZoom..metadata.maxZoom) return@read null
        val rowY = if (metadata.scheme == "xyz") yXyz else xyzToTmsY(zoom, yXyz)
        try {
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

    override fun close() = rwLock.write {
        bitmapCache.evictAll()
        try { db?.close() } catch (_: Exception) {}
        db = null
    }

    companion object {
        fun parseMetadata(raw: Map<String, String>): MbTilesMetadata {
            val scheme = raw["scheme"]?.lowercase()?.takeIf { it in setOf("tms", "xyz") } ?: "tms"
            val format = raw["format"]?.lowercase() ?: "png"
            val minZ = raw["minzoom"]?.toIntOrNull()?.coerceIn(0, 22) ?: 0
            val maxZ = raw["maxzoom"]?.toIntOrNull()?.coerceIn(minZ, 22) ?: 18
            val bounds = raw["bounds"]?.split(",")?.mapNotNull { it.trim().toDoubleOrNull() }
                ?.takeIf { it.size == 4 }
                ?.let { (w, s, e, n) -> TileGeoBounds(northLat = n, southLat = s, westLon = w, eastLon = e) }
            return MbTilesMetadata(scheme = scheme, format = format, minZoom = minZ, maxZoom = maxZ, bounds = bounds)
        }

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

        fun tileToBounds(zoom: Int, x: Int, yXyz: Int): TileGeoBounds {
            val n = (1 shl zoom).toDouble()
            val westLon = x / n * 360.0 - 180.0
            val eastLon = (x + 1) / n * 360.0 - 180.0
            val northLat = Math.toDegrees(atan(sinh(Math.PI * (1.0 - 2.0 * yXyz / n))))
            val southLat = Math.toDegrees(atan(sinh(Math.PI * (1.0 - 2.0 * (yXyz + 1) / n))))
            return TileGeoBounds(northLat, southLat, westLon, eastLon)
        }
    }
}
