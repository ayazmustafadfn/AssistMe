package com.artsistem.assistme.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun toRepeatType(value: String): RepeatType = RepeatType.valueOf(value)

    @TypeConverter
    fun fromRepeatType(type: RepeatType): String = type.name
}

@Database(
    entities = [Reminder::class, ReminderGroup::class, ReminderHistory::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun groupDao(): GroupDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2: gruplar tablosu eklenir ve reminders'a groupId sütunu eklenir.
         * Mevcut hatırlatmalar korunur (groupId null = "Grupsuz").
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `groups` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`name` TEXT NOT NULL, " +
                        "`colorArgb` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE reminders ADD COLUMN groupId INTEGER")
            }
        }

        /**
         * v2 -> v3: öncelik bayrağı, tekrar bitiş kontrolü ve tamamlananlar
         * geçmişi tablosu eklenir. Mevcut hatırlatmalar korunur.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN flagged INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeatEndMillis INTEGER")
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeatCount INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminder_history` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`reminderId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`groupId` INTEGER, " +
                        "`completedAtMillis` INTEGER NOT NULL, " +
                        "`kind` TEXT NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "assistme.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}
