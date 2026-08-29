package com.memorylane.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Project::class, PhotoAnalysisCache::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun photoAnalysisDao(): PhotoAnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Adds the new photo_analysis_cache table without touching the
        // existing projects table, so upgrading the app doesn't lose
        // anyone's saved projects.
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `photo_analysis_cache` (
                        `uri` TEXT NOT NULL PRIMARY KEY,
                        `tags` TEXT NOT NULL,
                        `sharpness` REAL NOT NULL,
                        `hash` INTEGER NOT NULL,
                        `fileLastModified` INTEGER NOT NULL,
                        `analyzedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Adds EXIF date/GPS columns for the Timeline and Map views.
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photo_analysis_cache ADD COLUMN dateTaken INTEGER")
                db.execSQL("ALTER TABLE photo_analysis_cache ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE photo_analysis_cache ADD COLUMN longitude REAL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memory_lane_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
