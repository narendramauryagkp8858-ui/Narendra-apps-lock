package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.VaultItemEntity
import com.example.data.model.VaultMediaType
import com.example.ui.components.GlassSecurityCard
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShield
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

private enum class VaultTab(val label: String) {
    ALL("All Media"),
    PHOTOS("Photos"),
    VIDEOS("Videos")
}

@Composable
fun VaultScreen(
    items: List<VaultItemEntity>,
    isBusy: Boolean,
    statusMessage: String?,
    activePhotoBitmap: Bitmap?,
    activeVideoFile: File?,
    loadThumbnail: suspend (VaultItemEntity) -> Bitmap?,
    onImportUris: (List<Uri>) -> Unit,
    onOpenItem: (VaultItemEntity) -> Unit,
    onCloseViewer: () -> Unit,
    onExportItem: (VaultItemEntity, Uri) -> Unit,
    onDeleteItem: (VaultItemEntity) -> Unit,
    onClearMessage: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(VaultTab.ALL) }
    var inspectingItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var pendingExportItem by remember { mutableStateOf<VaultItemEntity?>(null) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                onImportUris(uris)
            }
        }
    )

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { destUri ->
            val target = pendingExportItem
            pendingExportItem = null
            if (destUri != null && target != null) {
                onExportItem(target, destUri)
            }
        }
    )

    val filtered = remember(items, selectedTab) {
        when (selectedTab) {
            VaultTab.ALL -> items
            VaultTab.PHOTOS -> items.filter { it.mediaType == VaultMediaType.PHOTO }
            VaultTab.VIDEOS -> items.filter { it.mediaType == VaultMediaType.VIDEO }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp)) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Private Vault",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "AES-256-GCM Hardware Keystore Encrypted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecureEmerald
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ElevatedNavy)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.EnhancedEncryption,
                                contentDescription = null,
                                tint = CyanShield,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${items.size} items",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalPurple,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("vault_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("vault_add_photos_button")
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photos")
                    }
                    OutlinedButton(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("vault_add_videos_button")
                    ) {
                        Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Videos")
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassSecurityCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = CyanShield.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyanShield,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearMessage) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isBusy) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanShield)
                }
            }

            if (filtered.isEmpty() && !isBusy) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSecurityCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EnhancedEncryption,
                                contentDescription = null,
                                tint = CyanShield,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Your Encrypted Vault is Empty",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Use Android Photo Picker above to import private photos and videos. Files are encrypted locally with AES-256-GCM inside app-private storage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        VaultThumbnailTile(
                            item = item,
                            loadThumbnail = loadThumbnail,
                            onClick = {
                                inspectingItem = item
                                onOpenItem(item)
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                mediaPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            containerColor = RoyalPurple,
            contentColor = TextPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("vault_fab_add")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Encrypt media into vault")
        }
    }

    val currentItem = inspectingItem
    if (currentItem != null && (activePhotoBitmap != null || activeVideoFile != null)) {
        VaultMediaViewerDialog(
            item = currentItem,
            photoBitmap = activePhotoBitmap,
            videoFile = activeVideoFile,
            onExport = {
                pendingExportItem = currentItem
                exportDocumentLauncher.launch(currentItem.displayName)
            },
            onDelete = {
                onDeleteItem(currentItem)
                inspectingItem = null
                onCloseViewer()
            },
            onDismiss = {
                inspectingItem = null
                onCloseViewer()
            }
        )
    }
}

@Composable
private fun VaultThumbnailTile(
    item: VaultItemEntity,
    loadThumbnail: suspend (VaultItemEntity) -> Bitmap?,
    onClick: () -> Unit
) {
    var thumb by remember(item.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id) {
        thumb = loadThumbnail(item)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(ElevatedNavy)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("vault_item_${item.id}")
    ) {
        val bmp = thumb
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (item.mediaType == VaultMediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Image,
                    contentDescription = item.displayName,
                    tint = CyanShield,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        if (item.mediaType == VaultMediaType.VIDEO) {
            Icon(
                imageVector = Icons.Rounded.PlayCircleFilled,
                contentDescription = "Encrypted Video",
                tint = TextPrimary.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ObsidianBlack.copy(alpha = 0.72f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VaultMediaViewerDialog(
    item: VaultItemEntity,
    photoBitmap: Bitmap?,
    videoFile: File?,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Decrypted in memory • ${item.sizeBytes / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecureEmerald
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("close_vault_viewer")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close viewer", tint = TextPrimary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (videoFile != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setVideoPath(videoFile.absolutePath)
                                start()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}
