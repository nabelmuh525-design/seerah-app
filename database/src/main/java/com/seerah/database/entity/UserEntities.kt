package com.seerah.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["item_type", "item_id"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_type") val itemType: String, // EVENT, PERSON, PLACE, HADITH, VERSE, BOOK
    @ColumnInfo(name = "item_id") val itemId: Long,
    val title: String,
    val subtitle: String = "",
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long
)

@Entity(
    tableName = "user_notes",
    indices = [Index(value = ["item_type", "item_id"])]
)
data class UserNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "item_id") val itemId: Long,
    val title: String,
    val content: String,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long
)

@Entity(
    tableName = "downloads",
    indices = [Index(value = ["package_key"], unique = true)]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_key") val packageKey: String,
    val title: String,
    @ColumnInfo(name = "package_type") val packageType: String, // BOOK_PACK, MAP_MBTILES, GEOJSON_PACK, QURAN_PACK, HADITH_PACK
    @ColumnInfo(name = "version_tag") val versionTag: String = "1.0",
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0L,
    val status: String = "NOT_INSTALLED", // NOT_INSTALLED, READY_LOCAL, IMPORTED
    @ColumnInfo(name = "local_path") val localPath: String = "",
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long = 0L
)
