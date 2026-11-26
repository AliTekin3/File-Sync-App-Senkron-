package com.example.senkron.data.model

enum class SyncStatus {
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED,
    EXCLUDED
}

data class SyncedFile(
    val id: String = "",
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val driveFileId: String? = null
)
