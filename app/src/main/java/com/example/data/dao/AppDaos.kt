package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Note
import com.example.data.entity.SavedFile
import com.example.data.entity.ResumeProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query ORDER BY timestamp DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)
}

@Dao
interface SavedFileDao {
    @Query("SELECT * FROM saved_files ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<SavedFile>>

    @Query("SELECT * FROM saved_files WHERE name LIKE :query ORDER BY timestamp DESC")
    fun searchFiles(query: String): Flow<List<SavedFile>>

    @Query("SELECT * FROM saved_files WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteFiles(): Flow<List<SavedFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: SavedFile)

    @Update
    suspend fun updateFile(file: SavedFile)

    @Delete
    suspend fun deleteFile(file: SavedFile)
}

@Dao
interface ResumeProfileDao {
    @Query("SELECT * FROM resume_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: Int = 1): ResumeProfile?

    @Query("SELECT * FROM resume_profiles WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: Int = 1): Flow<ResumeProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ResumeProfile)
}
