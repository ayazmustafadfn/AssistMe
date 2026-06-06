package com.artsistem.assistme.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun toRepeatType(value: String): RepeatType = RepeatType.valueOf(value)

    @TypeConverter
    fun fromRepeatType(type: RepeatType): String = type.name
}

@Database(entities = [Reminder::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "assistme.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
