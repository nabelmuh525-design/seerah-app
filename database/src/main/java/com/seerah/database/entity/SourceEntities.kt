package com.seerah.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "authors")
data class AuthorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "death_year_hijri") val deathYearHijri: Int? = null,
    val biography: String = ""
)

@Entity(
    tableName = "sources",
    indices = [Index(value = ["title"]), Index(value = ["source_category"])]
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "normalized_title") val normalizedTitle: String,
    @ColumnInfo(name = "author_id") val authorId: Long? = null,
    @ColumnInfo(name = "source_category") val sourceCategory: String, // PRIMARY_HADITH, PRIMARY_SEERAH, PRIMARY_HISTORY, PRIMARY_BIOGRAPHY, PRIMARY_GEOGRAPHY, MODERN_STUDY, QURAN
    val description: String = "",
    @ColumnInfo(name = "is_primary_source") val isPrimarySource: Boolean = true,
    @ColumnInfo(name = "verification_note") val verificationNote: String = ""
)

@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["source_id"]), Index(value = ["file_hash"], unique = true)]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source_id") val sourceId: Long? = null,
    val title: String,
    @ColumnInfo(name = "normalized_title") val normalizedTitle: String,
    @ColumnInfo(name = "author_name") val authorName: String,
    @ColumnInfo(name = "source_category") val sourceCategory: String,
    @ColumnInfo(name = "is_primary_source") val isPrimarySource: Boolean = true,
    @ColumnInfo(name = "file_format") val fileFormat: String = "", // PDF, TXT, EPUB, XML, JSON, CSV, SQLITE, ZIP
    @ColumnInfo(name = "local_file_path") val localFilePath: String = "",
    @ColumnInfo(name = "file_hash") val fileHash: String? = null,
    @ColumnInfo(name = "is_imported") val isImported: Boolean = false,
    @ColumnInfo(name = "is_ocr_extracted") val isOcrExtracted: Boolean = false,
    @ColumnInfo(name = "needs_review") val needsReview: Boolean = false,
    @ColumnInfo(name = "page_count") val pageCount: Int = 0,
    @ColumnInfo(name = "imported_at_epoch_ms") val importedAtEpochMs: Long? = null,
    @ColumnInfo(name = "import_status") val importStatus: String = "AWAITING_FILE" // AWAITING_FILE, IMPORTED, INDEXING, FAILED
)

@Entity(
    tableName = "editions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id"])]
)
data class EditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "edition_name") val editionName: String,
    val publisher: String = "",
    val investigator: String = "", // المحقق
    @ColumnInfo(name = "publication_year") val publicationYear: String = "",
    @ColumnInfo(name = "volume_count") val volumeCount: Int = 1
)

@Entity(
    tableName = "source_pages",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id", "volume_number", "page_number"])]
)
data class SourcePageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "edition_id") val editionId: Long? = null,
    @ColumnInfo(name = "volume_number") val volumeNumber: Int = 1,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "chapter_title") val chapterTitle: String = "",
    @ColumnInfo(name = "raw_text") val rawText: String,
    @ColumnInfo(name = "normalized_text") val normalizedText: String,
    @ColumnInfo(name = "is_ocr_generated") val isOcrGenerated: Boolean = false
)

@Fts4(contentEntity = SourcePageEntity::class)
@Entity(tableName = "source_pages_fts")
data class SourcePageFtsEntity(
    @ColumnInfo(name = "chapter_title") val chapterTitle: String,
    @ColumnInfo(name = "normalized_text") val normalizedText: String
)
