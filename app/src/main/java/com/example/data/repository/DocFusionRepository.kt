package com.example.data.repository

import android.content.Context
import com.example.data.database.DocFusionDatabase
import com.example.data.entity.Note
import com.example.data.entity.SavedFile
import com.example.data.entity.ResumeProfile
import kotlinx.coroutines.flow.Flow
import java.io.File

class DocFusionRepository(private val context: Context) {
    private val database = DocFusionDatabase.getDatabase(context)
    val noteDao = database.noteDao()
    val savedFileDao = database.savedFileDao()
    val resumeDao = database.resumeDao()

    // Notes
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes("%$query%")
    suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    // Saved Files (File Manager)
    val allFiles: Flow<List<SavedFile>> = savedFileDao.getAllFiles()
    fun searchFiles(query: String): Flow<List<SavedFile>> = savedFileDao.searchFiles("%$query%")
    val favoriteFiles: Flow<List<SavedFile>> = savedFileDao.getFavoriteFiles()
    
    suspend fun insertFile(file: SavedFile) = savedFileDao.insertFile(file)
    suspend fun updateFile(file: SavedFile) = savedFileDao.updateFile(file)
    suspend fun deleteFile(file: SavedFile) {
        // Also delete the physical file if it exists
        try {
            val physicalFile = File(file.path)
            if (physicalFile.exists()) {
                physicalFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        savedFileDao.deleteFile(file)
    }

    // Resume Profile
    fun getResumeProfileFlow(): Flow<ResumeProfile?> = resumeDao.getProfileFlow()
    suspend fun getResumeProfile(): ResumeProfile {
        return resumeDao.getProfile() ?: ResumeProfile().also {
            resumeDao.insertProfile(it)
        }
    }
    suspend fun saveResumeProfile(profile: ResumeProfile) = resumeDao.insertProfile(profile)
}
