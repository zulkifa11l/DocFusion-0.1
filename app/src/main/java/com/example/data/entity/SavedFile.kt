package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_files")
data class SavedFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String, // local filePath in context.filesDir
    val category: String, // PDF, Scan, OCR, Photo, DB, Resume
    val format: String, // PDF, JPG, PNG, DOCX, TXT
    val sizeString: String, // e.g. "124 KB"
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
