package com.seerah.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seerah.database.dao.*
import com.seerah.database.entity.*

@Database(
    entities = [
        AuthorEntity::class,
        SourceEntity::class,
        BookEntity::class,
        EditionEntity::class,
        SourcePageEntity::class,
        SourcePageFtsEntity::class,
        EventEntity::class,
        EventFtsEntity::class,
        EventDateEntity::class,
        EventSourceEntity::class,
        NarrationEntity::class,
        HadithEntity::class,
        HadithGradeEntity::class,
        PersonEntity::class,
        PersonEventEntity::class,
        PlaceEntity::class,
        PlaceCoordinateEntity::class,
        PlaceSourceEntity::class,
        GeographicLayerEntity::class,
        HistoricalRouteEntity::class,
        RoutePointEntity::class,
        BattlePhaseEntity::class,
        BattleMapFeatureEntity::class,
        QuranVerseEntity::class,
        EventVerseEntity::class,
        FavoriteEntity::class,
        UserNoteEntity::class,
        DownloadEntity::class,
        // v3.0 Governance, Relationships, Bookmarks & Progress tables (Migration 1 -> 2)
        VolumeEntity::class,
        SourcePassageEntity::class,
        NarrationVariantEntity::class,
        PersonRelationshipEntity::class,
        TribeEntity::class,
        BookmarkEntity::class,
        ReadingProgressEntity::class,
        ResearchNoteEntity::class,
        ContentVersionEntity::class,
        AuditLogEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class SeerahDatabase : RoomDatabase() {
    abstract fun sourceLibraryDao(): SourceLibraryDao
    abstract fun eventDao(): EventDao
    abstract fun peopleDao(): PeopleDao
    abstract fun atlasGeoDao(): AtlasGeoDao
    abstract fun quranHadithDao(): QuranHadithDao
    abstract fun userDataDao(): UserDataDao
    abstract fun qualityAuditDao(): QualityAuditDao

    fun runIntegrityCheck(): Pair<Boolean, String> {
        return try {
            val cursor = openHelper.readableDatabase.query("PRAGMA integrity_check")
            cursor.use {
                if (it.moveToFirst()) {
                    val result = it.getString(0)
                    (result.equals("ok", ignoreCase = true)) to result
                } else {
                    false to "تعذر قراءة نتيجة فحص السلامة"
                }
            }
        } catch (e: Exception) {
            false to (e.message ?: "خطأ غير معروف أثناء فحص قاعدة البيانات")
        }
    }

    companion object {
        const val DATABASE_NAME = "seerah_encyclopedia.db"

        /**
         * ترحيل آمن من الإصدار 1 إلى الإصدار 2 دون حذف أي بيانات مستخدم
         * (المفضلة، الملاحظات، والكتب المستوردة تبقى محفوظة بالكامل).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `volumes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `book_id` INTEGER NOT NULL,
                        `volume_number` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `printed_page_start` INTEGER,
                        `printed_page_end` INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_volumes_book_id_volume_number` ON `volumes` (`book_id`, `volume_number`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `source_passages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `book_id` INTEGER NOT NULL,
                        `source_page_id` INTEGER,
                        `printed_page_label` TEXT NOT NULL,
                        `digital_page_number` INTEGER,
                        `passage_text` TEXT NOT NULL,
                        `review_status` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_passages_book_id` ON `source_passages` (`book_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_passages_source_page_id` ON `source_passages` (`source_page_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `narration_variants` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `narration_id` INTEGER NOT NULL,
                        `source_name` TEXT NOT NULL,
                        `variant_text` TEXT NOT NULL,
                        `difference_note` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_narration_variants_narration_id` ON `narration_variants` (`narration_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `person_relationships` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `person_id` INTEGER NOT NULL,
                        `related_person_id` INTEGER NOT NULL,
                        `relation_type` TEXT NOT NULL,
                        `certainty_status` TEXT NOT NULL,
                        `source_citation` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_person_relationships_person_id` ON `person_relationships` (`person_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_person_relationships_related_person_id` ON `person_relationships` (`related_person_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tribes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `lineage` TEXT NOT NULL,
                        `region` TEXT NOT NULL,
                        `source_citation` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tribes_name` ON `tribes` (`name`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `item_type` TEXT NOT NULL,
                        `item_id` INTEGER NOT NULL,
                        `page_number` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_item_type_item_id` ON `bookmarks` (`item_type`, `item_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reading_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `item_type` TEXT NOT NULL,
                        `item_id` INTEGER NOT NULL,
                        `last_page` INTEGER NOT NULL,
                        `scroll_fraction` REAL NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reading_progress_item_type_item_id` ON `reading_progress` (`item_type`, `item_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `research_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `target_type` TEXT NOT NULL,
                        `target_id` INTEGER NOT NULL,
                        `reviewer_name` TEXT NOT NULL,
                        `review_status` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `reviewed_at_epoch_ms` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_notes_target_type_target_id` ON `research_notes` (`target_type`, `target_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `content_versions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `pack_key` TEXT NOT NULL,
                        `version_tag` TEXT NOT NULL,
                        `sha256_hash` TEXT NOT NULL,
                        `installed_at_epoch_ms` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_content_versions_pack_key` ON `content_versions` (`pack_key`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `audit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `action` TEXT NOT NULL,
                        `target` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL
                    )
                """.trimIndent())

                // تصحيح بيانات الإصدارات السابقة (v2.1.2 وما قبلها) دون حذف بيانات المستخدم
                db.execSQL("UPDATE events SET verification_status = 'NEEDS_COMPLETION' WHERE verification_status = 'VERIFIED'")
                db.execSQL("""
                    UPDATE narrations
                    SET grade_status = 'HISTORICAL_UNGRADED'
                    WHERE event_id IS NOT NULL
                      AND EXISTS (
                        SELECT 1 FROM events
                        WHERE events.id = narrations.event_id
                          AND events.short_summary = narrations.narration_text
                      )
                """.trimIndent())

                // تحديث طبقات الخريطة للترقيات من v2 (INSERT OR IGNORE للمفاتيح الجديدة ومعالجة القديمة)
                db.execSQL("DELETE FROM geographic_layers WHERE layer_key = 'events'")
                db.execSQL("UPDATE geographic_layers SET display_name = 'المناطق التاريخية' WHERE layer_key = 'terrain'")
                val v3Layers = listOf(
                    Triple("cities_villages", "المدن والقرى", 1),
                    Triple("historical_sites", "المواقع التاريخية", 2),
                    Triple("mountains", "الجبال والتضاريس", 3),
                    Triple("valleys", "الأودية والشعاب", 4),
                    Triple("wells", "الآبار ومصادر المياه", 5),
                    Triple("roads", "طرق القوافل", 6),
                    Triple("hijra_routes", "طرق الهجرة", 7),
                    Triple("battles", "مسارات الغزوات", 8),
                    Triple("camps", "مواقع المعسكرات", 9),
                    Triple("terrain", "المناطق التاريخية", 10),
                    Triple("military_roads", "الطرق العسكرية", 11),
                    Triple("people_locations", "مواقع الأحداث المرتبطة بالشخصيات", 12)
                )
                v3Layers.forEach { (key, name, z) ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO geographic_layers (layer_key, display_name, is_enabled_by_default, z_index, geojson_path, mbtiles_path) VALUES (?, ?, 1, ?, '', '')",
                        arrayOf(key, name, z)
                    )
                }
            }
        }

        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `events` ADD COLUMN `stable_id` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `people` ADD COLUMN `stable_id` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `places` ADD COLUMN `stable_id` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `quran_verses` ADD COLUMN `stable_id` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `hadiths` ADD COLUMN `stable_id` TEXT NOT NULL DEFAULT ''")
            }
        }

        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `hadith_grades` ADD COLUMN `stable_id` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `hadith_grades` ADD COLUMN `is_human_review` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `hadith_grades` ADD COLUMN `review_state` TEXT NOT NULL DEFAULT 'UNDER_REVIEW'")
                db.execSQL("ALTER TABLE `hadith_grades` ADD COLUMN `reviewed_at_epoch_ms` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        fun build(context: Context): SeerahDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SeerahDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys=ON;")
                    }
                })
                .build()
        }
    }
}
