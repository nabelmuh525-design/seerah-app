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
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SeerahDatabase : RoomDatabase() {
    abstract fun sourceLibraryDao(): SourceLibraryDao
    abstract fun eventDao(): EventDao
    abstract fun peopleDao(): PeopleDao
    abstract fun atlasGeoDao(): AtlasGeoDao
    abstract fun quranHadithDao(): QuranHadithDao
    abstract fun userDataDao(): UserDataDao

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
         * سجل الترحيلات الآمنة لقاعدة البيانات.
         * يُحظر استخدام fallbackToDestructiveMigration لحماية بيانات المستخدم والكتب المستوردة.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            // الترحيلات المستقبلية تُضاف هنا دون حذف أي بيانات مستخدم
        )

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
