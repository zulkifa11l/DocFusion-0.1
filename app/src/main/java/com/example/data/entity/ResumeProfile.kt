package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_profiles")
data class ResumeProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 1, // Only 1 active profile for easy single-screen editing
    val fullName: String = "",
    val title: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val website: String = "",
    val aboutMe: String = "",
    val experience: String = "", // Raw text block or serialized
    val education: String = "", // Raw text block or serialized
    val skills: String = "", // comma separated
    val projects: String = "",
    val templateId: String = "Modern Blue",
    val photoBytesPath: String? = null // local photo file path
)
