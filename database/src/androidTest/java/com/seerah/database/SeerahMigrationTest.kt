package com.seerah.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeerahMigrationTest {

    private val testDbName = "migration-test-seerah"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SeerahDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_remediatesLegacyStatusesAndPreservesUserData() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL("INSERT INTO events (id, title, normalized_title, stage_code, event_type, chronological_order, short_summary, detailed_analysis, cause_text, preparation_text, outcome_text, verification_status) VALUES (1, 'غزوة بدر', 'غزوه بدر', 'BATTLES_AND_EXPEDITIONS', 'GHAZWAH', 1, 'ملخص الحدث', '', '', '', '', 'VERIFIED')")
            execSQL("INSERT INTO narrations (id, event_id, source_name, author_name, book_name, volume, page, chapter, edition_name, investigator, isnad, narration_text, normalized_text, scholarly_commentary, grade_status, grade_reference) VALUES (1, 1, 'صحيح البخاري', '', '', '', '', '', '', '', '', 'ملخص الحدث', 'ملخص الحدث', '', 'SAHIH', '')")
            execSQL("INSERT INTO favorites (id, item_type, item_id, title, subtitle, created_at_epoch_ms) VALUES (1, 'EVENT', 1, 'غزوة بدر', '', 1000)")
            execSQL("INSERT INTO user_notes (id, item_type, item_id, title, content, updated_at_epoch_ms) VALUES (1, 'EVENT', 1, 'ملاحظتي', 'محتوى محفوظ', 1000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, SeerahDatabase.MIGRATION_1_2)

        db.query("SELECT verification_status FROM events WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("NEEDS_COMPLETION", c.getString(0))
        }
        db.query("SELECT grade_status FROM narrations WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("HISTORICAL_UNGRADED", c.getString(0))
        }
        db.query("SELECT COUNT(*) FROM favorites").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM user_notes").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.close()
    }
}
