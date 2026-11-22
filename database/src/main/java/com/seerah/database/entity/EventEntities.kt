package com.seerah.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["stage_code"]),
        Index(value = ["event_type"]),
        Index(value = ["chronological_order"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stable_id", defaultValue = "") val stableId: String = "",
    val title: String,
    @ColumnInfo(name = "normalized_title") val normalizedTitle: String,
    @ColumnInfo(name = "stage_code") val stageCode: String, // e.g. BIRTH, REVELATION, HIJRA, BATTLES...
    @ColumnInfo(name = "event_type") val eventType: String, // GENERAL, GHAZWAH, SARIYYAH, HIJRA_STAGE, TREATY
    @ColumnInfo(name = "chronological_order") val chronologicalOrder: Int,
    @ColumnInfo(name = "short_summary") val shortSummary: String,
    @ColumnInfo(name = "detailed_analysis") val detailedAnalysis: String = "",
    @ColumnInfo(name = "cause_text") val causeText: String = "",
    @ColumnInfo(name = "preparation_text") val preparationText: String = "",
    @ColumnInfo(name = "outcome_text") val outcomeText: String = "",
    @ColumnInfo(name = "place_id") val placeId: Long? = null,
    @ColumnInfo(name = "map_id") val mapId: String? = null,
    @ColumnInfo(name = "previous_event_id") val previousEventId: Long? = null,
    @ColumnInfo(name = "next_event_id") val nextEventId: Long? = null,
    @ColumnInfo(name = "verification_status") val verificationStatus: String = "UNVERIFIED" // VERIFIED, DISPUTED, UNVERIFIED
)

@Fts4(contentEntity = EventEntity::class)
@Entity(tableName = "events_fts")
data class EventFtsEntity(
    @ColumnInfo(name = "normalized_title") val normalizedTitle: String,
    @ColumnInfo(name = "short_summary") val shortSummary: String,
    @ColumnInfo(name = "detailed_analysis") val detailedAnalysis: String
)

@Entity(
    tableName = "event_dates",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"])]
)
data class EventDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "hijri_year") val hijriYear: Int? = null,
    @ColumnInfo(name = "hijri_display") val hijriDisplay: String,
    @ColumnInfo(name = "gregorian_display") val gregorianDisplay: String = "",
    @ColumnInfo(name = "is_disputed") val isDisputed: Boolean = false,
    @ColumnInfo(name = "source_citation") val sourceCitation: String = ""
)

@Entity(
    tableName = "event_sources",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"]), Index(value = ["source_id"])]
)
data class EventSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "source_id") val sourceId: Long,
    val volume: String = "",
    val page: String = "",
    val chapter: String = "",
    @ColumnInfo(name = "edition_info") val editionInfo: String = "",
    val investigator: String = "",
    @ColumnInfo(name = "citation_note") val citationNote: String = ""
)

@Entity(
    tableName = "narrations",
    indices = [Index(value = ["event_id"]), Index(value = ["source_id"])]
)
data class NarrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long? = null,
    @ColumnInfo(name = "source_id") val sourceId: Long? = null,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "author_name") val authorName: String = "",
    @ColumnInfo(name = "book_name") val bookName: String = "",
    val volume: String = "",
    val page: String = "",
    val chapter: String = "",
    @ColumnInfo(name = "edition_name") val editionName: String = "",
    val investigator: String = "",
    val isnad: String = "",
    @ColumnInfo(name = "narration_text") val narrationText: String,
    @ColumnInfo(name = "normalized_text") val normalizedText: String,
    @ColumnInfo(name = "scholarly_commentary") val scholarlyCommentary: String = "",
    @ColumnInfo(name = "grade_status") val gradeStatus: String = "UNVERIFIED", // SAHIH, HASAN, DAIF, DAIF_JIDDAN, MAWDU, DISPUTED, HISTORICAL_UNGRADED, UNVERIFIED
    @ColumnInfo(name = "grade_reference") val gradeReference: String = "" // مرجع الحكم العلمي فقط - لا يُولّد آلياً
)
