package com.seerah.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hadiths",
    indices = [Index(value = ["event_id"]), Index(value = ["source_name"])]
)
data class HadithEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stable_id", defaultValue = "") val stableId: String = "",
    @ColumnInfo(name = "event_id") val eventId: Long? = null,
    @ColumnInfo(name = "hadith_text") val hadithText: String,
    @ColumnInfo(name = "normalized_text") val normalizedText: String,
    val narrator: String,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "book_title") val bookTitle: String = "",
    @ColumnInfo(name = "chapter_title") val chapterTitle: String = "",
    @ColumnInfo(name = "hadith_number") val hadithNumber: String = "",
    val takhrij: String = "",
    val isnad: String = "",
    @ColumnInfo(name = "edition_info") val editionInfo: String = ""
)

@Entity(
    tableName = "hadith_grades",
    foreignKeys = [
        ForeignKey(
            entity = HadithEntity::class,
            parentColumns = ["id"],
            childColumns = ["hadith_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["hadith_id"])]
)
data class HadithGradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stable_id", defaultValue = "") val stableId: String = "",
    @ColumnInfo(name = "hadith_id") val hadithId: Long,
    @ColumnInfo(name = "grade_code") val gradeCode: String,
    @ColumnInfo(name = "grading_scholar") val gradingScholar: String,
    @ColumnInfo(name = "grading_source") val gradingSource: String,
    val notes: String = "",
    @ColumnInfo(name = "is_human_review", defaultValue = "0") val isHumanReview: Boolean = false,
    @ColumnInfo(name = "review_state", defaultValue = "UNDER_REVIEW") val reviewState: String = "UNDER_REVIEW",
    @ColumnInfo(name = "reviewed_at_epoch_ms", defaultValue = "0") val reviewedAtEpochMs: Long = 0L
)

@Entity(
    tableName = "quran_verses",
    indices = [Index(value = ["surah_number", "ayah_number"], unique = true)]
)
data class QuranVerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stable_id", defaultValue = "") val stableId: String = "",
    @ColumnInfo(name = "surah_number") val surahNumber: Int,
    @ColumnInfo(name = "surah_name") val surahName: String,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "uthmani_text") val uthmaniText: String,
    @ColumnInfo(name = "normalized_text") val normalizedText: String,
    @ColumnInfo(name = "tafsir_summary") val tafsirSummary: String = "",
    @ColumnInfo(name = "tafsir_source") val tafsirSource: String = "",
    @ColumnInfo(name = "asbab_al_nuzul") val asbabAlNuzul: String = "",
    @ColumnInfo(name = "asbab_source") val asbabSource: String = "",
    @ColumnInfo(name = "asbab_verified") val asbabVerified: Boolean = false
)

@Entity(
    tableName = "event_verses",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuranVerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verse_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"]), Index(value = ["verse_id"])]
)
data class EventVerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "verse_id") val verseId: Long,
    @ColumnInfo(name = "relation_note") val relationNote: String = "",
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)
