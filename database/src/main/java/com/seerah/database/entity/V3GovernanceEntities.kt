package com.seerah.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "volumes", indices = [Index(value = ["book_id", "volume_number"])])
data class VolumeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "volume_number") val volumeNumber: Int,
    val title: String = "",
    @ColumnInfo(name = "printed_page_start") val printedPageStart: Int? = null,
    @ColumnInfo(name = "printed_page_end") val printedPageEnd: Int? = null
)

@Entity(tableName = "source_passages", indices = [Index(value = ["book_id"]), Index(value = ["source_page_id"])])
data class SourcePassageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "source_page_id") val sourcePageId: Long? = null,
    @ColumnInfo(name = "printed_page_label") val printedPageLabel: String = "",
    @ColumnInfo(name = "digital_page_number") val digitalPageNumber: Int? = null,
    @ColumnInfo(name = "passage_text") val passageText: String,
    @ColumnInfo(name = "review_status") val reviewStatus: String = "NEEDS_COMPLETION"
)

@Entity(tableName = "narration_variants", indices = [Index(value = ["narration_id"])])
data class NarrationVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "narration_id") val narrationId: Long,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "variant_text") val variantText: String,
    @ColumnInfo(name = "difference_note") val differenceNote: String = ""
)

@Entity(tableName = "person_relationships", indices = [Index(value = ["person_id"]), Index(value = ["related_person_id"])])
data class PersonRelationshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "person_id") val personId: Long,
    @ColumnInfo(name = "related_person_id") val relatedPersonId: Long,
    @ColumnInfo(name = "relation_type") val relationType: String, // FATHER, MOTHER, SPOUSE, CHILD, SIBLING, COMPANION
    @ColumnInfo(name = "certainty_status") val certaintyStatus: String = "PRIMARY_DOCUMENTED",
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(tableName = "tribes", indices = [Index(value = ["name"], unique = true)])
data class TribeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lineage: String = "",
    val region: String = "",
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(tableName = "bookmarks", indices = [Index(value = ["item_type", "item_id"])])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "page_number") val pageNumber: Int = 1,
    val label: String = "",
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long
)

@Entity(tableName = "reading_progress", indices = [Index(value = ["item_type", "item_id"], unique = true)])
data class ReadingProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "last_page") val lastPage: Int = 1,
    @ColumnInfo(name = "scroll_fraction") val scrollFraction: Float = 0f,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long
)

@Entity(tableName = "research_notes", indices = [Index(value = ["target_type", "target_id"])])
data class ResearchNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "target_type") val targetType: String,
    @ColumnInfo(name = "target_id") val targetId: Long,
    @ColumnInfo(name = "reviewer_name") val reviewerName: String = "",
    @ColumnInfo(name = "review_status") val reviewStatus: String = "UNDER_REVIEW",
    val note: String,
    @ColumnInfo(name = "reviewed_at_epoch_ms") val reviewedAtEpochMs: Long
)

@Entity(tableName = "content_versions", indices = [Index(value = ["pack_key"], unique = true)])
data class ContentVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pack_key") val packKey: String,
    @ColumnInfo(name = "version_tag") val versionTag: String,
    @ColumnInfo(name = "sha256_hash") val sha256Hash: String = "",
    @ColumnInfo(name = "installed_at_epoch_ms") val installedAtEpochMs: Long
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val target: String,
    val details: String = "",
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long
)
