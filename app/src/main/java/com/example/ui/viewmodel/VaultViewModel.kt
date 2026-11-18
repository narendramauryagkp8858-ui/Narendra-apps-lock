package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NarendraAppLockApplication
import com.example.data.local.SecurityEventDao
import com.example.data.local.SecurityEventEntity
import com.example.data.local.VaultItemEntity
import com.example.data.model.SecurityEventType
import com.example.data.model.VaultMediaType
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val eventDao: SecurityEventDao
) : ViewModel() {

    val vaultItems: StateFlow<List<VaultItemEntity>> = vaultRepository.vaultItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _activePhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val activePhotoBitmap: StateFlow<Bitmap?> = _activePhotoBitmap.asStateFlow()

    private val _activeVideoFile = MutableStateFlow<File?>(null)
    val activeVideoFile: StateFlow<File?> = _activeVideoFile.asStateFlow()

    private val thumbCache = ConcurrentHashMap<Long, Bitmap>()
    private val intruderCache = ConcurrentHashMap<String, Bitmap>()

    fun importMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            var successCount = 0
            uris.forEach { uri ->
                val result = vaultRepository.importMediaUri(uri)
                if (result.isSuccess) {
                    successCount++
                    val entity = result.getOrNull()
                    if (entity != null) {
                        eventDao.insert(
                            SecurityEventEntity(
                                eventType = SecurityEventType.VAULT_ADDED,
                                title = "Encrypted ${entity.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                detail = "${entity.displayName} secured with AES-256-GCM"
                            )
                        )
                    }
                }
            }
            _isBusy.value = false
            _statusMessage.value = if (successCount > 0) {
                "$successCount file(s) encrypted inside Private Vault"
            } else {
                "Failed to encrypt selected media"
            }
        }
    }

    suspend fun loadThumbnail(item: VaultItemEntity): Bitmap? {
        thumbCache[item.id]?.let { return it }
        return vaultRepository.decryptThumbnailBitmap(item)?.also { bmp ->
            thumbCache[item.id] = bmp
        }
    }

    suspend fun loadIntruderPhoto(path: String): Bitmap? {
        intruderCache[path]?.let { return it }
        return vaultRepository.decryptIntruderPhoto(path)?.also { bmp ->
            intruderCache[path] = bmp
        }
    }

    fun openVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            _isBusy.value = true
            if (item.mediaType == VaultMediaType.PHOTO) {
                _activeVideoFile.value = null
                _activePhotoBitmap.value = vaultRepository.decryptPhotoBitmap(item)
            } else {
                _activePhotoBitmap.value = null
                _activeVideoFile.value = vaultRepository.decryptToTempPlaybackFile(item)
            }
            _isBusy.value = false
        }
    }

    fun closeViewer() {
        _activePhotoBitmap.value = null
        _activeVideoFile.value = null
        vaultRepository.clearPlaybackCache()
    }

    fun exportItem(item: VaultItemEntity, destinationUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = vaultRepository.exportToUri(item, destinationUri)
            _isBusy.value = false
            if (result.isSuccess) {
                eventDao.insert(
                    SecurityEventEntity(
                        eventType = SecurityEventType.VAULT_EXPORTED,
                        title = "Vault Item Exported",
                        detail = "Restored ${item.displayName} to user-selected location"
                    )
                )
                _statusMessage.value = "Exported ${item.displayName}"
            } else {
                _statusMessage.value = "Export failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteItem(item: VaultItemEntity) {
        viewModelScope.launch {
            thumbCache.remove(item.id)
            vaultRepository.deleteVaultItem(item)
            _statusMessage.value = "Deleted ${item.displayName} permanently"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        vaultRepository.clearPlaybackCache()
        super.onCleared()
    }

    class Factory(private val app: NarendraAppLockApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VaultViewModel(
                vaultRepository = app.vaultRepository,
                eventDao = app.database.securityEventDao()
            ) as T
        }
    }
}
