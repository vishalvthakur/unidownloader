package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val filePath: String,
    val fileSize: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaType: String, // "video", "image", "audio"
    val platform: String   // "youtube", "instagram", "whatsapp", "other"
)
