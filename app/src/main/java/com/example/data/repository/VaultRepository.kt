package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.VaultItemDao
import com.example.data.local.VaultItemEntity
import com.example.data.model.VaultMediaType
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val vaultItemDao: VaultItemDao,
    private val cryptoManager: CryptoManager
) {
    private val vaultDir: File by lazy { File(context.filesDir, "vault_media").apply { mkdirs() } }
    private val thumbDir: File by lazy { File(context.filesDir, "vault_thumbs").apply { mkdirs() } }
    private val playbackCacheDir: File by lazy { File(context.cacheDir, "playback_tmp").apply { mkdirs() } }

    val vaultItemsFlow: Flow<List<VaultItemEntity>> = vaultItemDao.observeAll()
    val vaultCountFlow: Flow<Int> = vaultItemDao.observeCount()

    suspend fun importMediaUri(uri: Uri): Result<VaultItemEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            val mediaType = if (mimeType.startsWith("video/")) VaultMediaType.VIDEO else VaultMediaType.PHOTO
            val displayName = queryDisplayName(uri) ?: "${mediaType.name.lowercase()}_${System.currentTimeMillis()}"
            val uuid = UUID.randomUUID().toString()

            val encryptedFile = File(vaultDir, "$uuid.enc")
            val totalBytes = resolver.openInputStream(uri)?.use { input ->
                encryptedFile.outputStream().use { output ->
                    cryptoManager.encryptStream(input, output)
                }
            } ?: error("Unable to open selected media stream")

            val encryptedThumbPath = createEncryptedThumbnail(uri, mediaType, uuid)

            val entity = VaultItemEntity(
                displayName = displayName,
                mediaType = mediaType,
                encryptedFilePath = encryptedFile.absolutePath,
                encryptedThumbPath = encryptedThumbPath,
                mimeType = mimeType,
                sizeBytes = totalBytes,
                createdAt = System.currentTimeMillis()
            )
            val id = vaultItemDao.insert(entity)
            entity.copy(id = id)
        }
    }

    suspend fun decryptThumbnailBitmap(item: VaultItemEntity): Bitmap? = withContext(Dispatchers.IO) {
        val thumbPath = item.encryptedThumbPath ?: return@withContext null
        val file = File(thumbPath)
        if (!file.exists()) return@withContext null
        runCatching {
            val plainBytes = file.inputStream().use { cryptoManager.decryptStreamToBytes(it) }
            BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size)
        }.getOrNull()
    }

    suspend fun decryptPhotoBitmap(item: VaultItemEntity): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(item.encryptedFilePath)
        if (!file.exists()) return@withContext null
        runCatching {
            val plainBytes = file.inputStream().use { cryptoManager.decryptStreamToBytes(it) }
            BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size)
        }.getOrNull()
    }

    suspend fun decryptToTempPlaybackFile(item: VaultItemEntity): File? = withContext(Dispatchers.IO) {
        val file = File(item.encryptedFilePath)
        if (!file.exists()) return@withContext null
        runCatching {
            clearPlaybackCache()
            val ext = if (item.mediaType == VaultMediaType.VIDEO) "mp4" else "jpg"
            val tempFile = File(playbackCacheDir, "tmp_${item.id}_${System.currentTimeMillis()}.$ext")
            file.inputStream().use { input ->
                tempFile.outputStream().use { output ->
                    cryptoManager.decryptStream(input, output)
                }
            }
            tempFile
        }.getOrNull()
    }

    suspend fun exportToUri(item: VaultItemEntity, destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(item.encryptedFilePath)
            require(file.exists()) { "Encrypted vault file missing" }
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                file.inputStream().use { input ->
                    cryptoManager.decryptStream(input, output)
                }
            } ?: error("Cannot write to destination URI")
        }
    }

    suspend fun deleteVaultItem(item: VaultItemEntity) = withContext(Dispatchers.IO) {
        runCatching { File(item.encryptedFilePath).delete() }
        item.encryptedThumbPath?.let { path -> runCatching { File(path).delete() } }
        vaultItemDao.deleteById(item.id)
    }

    fun clearPlaybackCache() {
        playbackCacheDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun decryptIntruderPhoto(encryptedPath: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(encryptedPath)
        if (!file.exists()) return@withContext null
        runCatching {
            val plain = file.inputStream().use { cryptoManager.decryptStreamToBytes(it) }
            BitmapFactory.decodeByteArray(plain, 0, plain.size)
        }.getOrNull()
    }

    private fun createEncryptedThumbnail(uri: Uri, mediaType: VaultMediaType, uuid: String): String? {
        return runCatching {
            val thumbBitmap: Bitmap? = if (mediaType == VaultMediaType.PHOTO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    retriever.release()
                }
            }

            if (thumbBitmap == null) return null
            val scaled = scaleCenterThumbnail(thumbBitmap, 320)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 78, baos)
            if (scaled !== thumbBitmap) scaled.recycle()
            thumbBitmap.recycle()

            val thumbFile = File(thumbDir, "${uuid}_thumb.enc")
            baos.toByteArray().inputStream().use { input ->
                thumbFile.outputStream().use { output ->
                    cryptoManager.encryptStream(input, output)
                }
            }
            thumbFile.absolutePath
        }.getOrNull()
    }

    private fun scaleCenterThumbnail(src: Bitmap, maxDim: Int): Bitmap {
        val ratio = minOf(maxDim.toFloat() / src.width, maxDim.toFloat() / src.height)
        if (ratio >= 1f) return src
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }
}
