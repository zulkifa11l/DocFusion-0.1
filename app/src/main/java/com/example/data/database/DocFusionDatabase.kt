package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.NoteDao
import com.example.data.dao.SavedFileDao
import com.example.data.dao.ResumeProfileDao
import com.example.data.entity.Note
import com.example.data.entity.SavedFile
import com.example.data.entity.ResumeProfile

@Database(
    entities = [Note::class, SavedFile::class, ResumeProfile::class],
    version = 1,
    exportSchema = false
)
abstract class DocFusionDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun savedFileDao(): SavedFileDao
    abstract fun resumeDao(): ResumeProfileDao

    companion object {
        @Volatile
        private var INSTANCE: DocFusionDatabase? = null

        fun getDatabase(context: Context): DocFusionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocFusionDatabase::class.java,
                    "docfusion_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
