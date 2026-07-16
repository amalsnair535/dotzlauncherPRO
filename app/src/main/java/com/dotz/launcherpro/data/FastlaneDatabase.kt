package com.dotz.launcherpro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FastlaneEvent::class], version = 1, exportSchema = false)
@TypeConverters(FastlaneConverters::class)
abstract class FastlaneDatabase : RoomDatabase() {
    abstract fun fastlaneDao(): FastlaneDao

    companion object {
        @Volatile
        private var INSTANCE: FastlaneDatabase? = null

        fun getDatabase(context: Context): FastlaneDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FastlaneDatabase::class.java,
                    "fastlane_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
