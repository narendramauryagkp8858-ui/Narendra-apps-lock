package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SecurityEventType
import com.example.data.model.VaultMediaType

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isLocked: Boolean = true,
    val isHiddenShortcut: Boolean = false,
    val lockCount: Int = 0,
    val lastUnlockedAt: Long = 0L
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val displayName: String,
    val mediaType: VaultMediaType,
    val encryptedFilePath: String,
    val encryptedThumbPath: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_events")
data class SecurityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventType: SecurityEventType,
    val title: String,
    val detail: String,
    val targetPackage: String? = null,
    val intruderPhotoPath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
