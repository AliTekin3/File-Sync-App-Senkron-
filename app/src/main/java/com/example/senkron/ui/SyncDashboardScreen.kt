package com.example.senkron.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.senkron.data.model.SyncStatus
import com.example.senkron.data.model.SyncedFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDashboardScreen(
    uiState: SyncUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onFolderChanged: (String) -> Unit,
    onOpenDriveClick: () -> Unit,
    onStartSyncClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Senkron - Bulut Yedekleme",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Google Hesabı Kartı
            AccountCard(
                uiState = uiState,
                onSignInClick = onSignInClick,
                onSignOutClick = onSignOutClick
            )

            // Akıllı Belge Bilgilendirme Kartı
            PhotoExclusionNoticeCard()

            // İstatistik Kartı
            StatsRow(uiState = uiState)

            // Google Drive Hedef Klasör Kartı
            TargetFolderCard(
                uiState = uiState,
                onFolderChanged = onFolderChanged,
                onOpenDriveClick = onOpenDriveClick
            )

            // Ayarlar ve Manuel Senkronizasyon Butonu
            SettingsAndActionCard(
                uiState = uiState,
                onWifiOnlyChanged = onWifiOnlyChanged,
                onStartSyncClick = onStartSyncClick
            )

            // Dosya Listesi Başlığı
            Text(
                text = "Cihazdaki Belgeler ve Dosyalar (${uiState.syncableFiles.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Dosya Listesi
            if (uiState.syncableFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Senkronize edilecek dosya bulunamadı veya izin bekleniyor.",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.syncableFiles, key = { it.id }) { file ->
                        FileItemRow(file = file)
                    }
                }
            }
        }
    }
}

@Composable
fun TargetFolderCard(
    uiState: SyncUiState,
    onFolderChanged: (String) -> Unit,
    onOpenDriveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Google Drive Hedef Klasörü",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.targetFolderName,
                    onValueChange = onFolderChanged,
                    label = { Text("Drive Klasör Adı") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.signedInAccount != null) {
                    OutlinedButton(onClick = onOpenDriveClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Drive'da Aç", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(
    uiState: SyncUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val account = uiState.signedInAccount
                    if (account != null) {
                        Text(
                            text = account.displayName ?: "Google Hesabı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = account.email ?: "",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "Google Drive Bağlantısı Yok",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Dosyaları senkronize etmek için giriş yapın",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (uiState.signedInAccount == null) {
                Button(onClick = onSignInClick) {
                    Text("Giriş Yap")
                }
            } else {
                OutlinedButton(onClick = onSignOutClick) {
                    Text("Çıkış")
                }
            }
        }
    }
}

@Composable
fun PhotoExclusionNoticeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Akıllı Belge Filtresi Aktif",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 14.sp
                )
                Text(
                    text = "Yalnızca PDF, Word, Excel, Notlar, ZIP/RAR, APK ve WhatsApp belgeleri/sticker'ları yedeklenir. Fotoğraf ve gereksiz sistem dosyaları atlanır.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun StatsRow(uiState: SyncUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Toplam Dosya",
            value = uiState.syncableFiles.size.toString(),
            color = MaterialTheme.colorScheme.primaryContainer
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Yüklenenler",
            value = uiState.syncableFiles.count { it.syncStatus == SyncStatus.SYNCED }.toString(),
            color = Color(0xFFE8F5E9)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Hariç Fotoğraf",
            value = uiState.excludedPhotosCount.toString(),
            color = Color(0xFFFFF3E0)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun SettingsAndActionCard(
    uiState: SyncUiState,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onStartSyncClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Yalnızca Wi-Fi ile Senkronize Et")
                }
                Switch(
                    checked = uiState.wifiOnly,
                    onCheckedChange = onWifiOnlyChanged
                )
            }

            Text(
                text = "Son Senkronizasyon: ${uiState.lastSyncTime}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Button(
                onClick = onStartSyncClick,
                enabled = uiState.signedInAccount != null && !uiState.isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.syncStatusMessage)
                } else {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Şimdi Google Drive'a Senkronize Et")
                }
            }
        }
    }
}

@Composable
fun FileItemRow(file: SyncedFile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatFileSize(file.size)} • ${formatDate(file.lastModified)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Durum rozeti
            StatusBadge(status = file.syncStatus)
        }
    }
}

@Composable
fun StatusBadge(status: SyncStatus) {
    val (bgColor, textColor, label, icon) = when (status) {
        SyncStatus.SYNCED -> Quadruple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "YÜKLENDİ",
            Icons.Default.CloudDone
        )
        SyncStatus.UPLOADING -> Quadruple(
            Color(0xFFE3F2FD),
            Color(0xFF1565C0),
            "YÜKLENİYOR",
            Icons.Default.CloudUpload
        )
        SyncStatus.FAILED -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "HATA",
            Icons.Default.ErrorOutline
        )
        else -> Quadruple(
            Color(0xFFF5F5F5),
            Color(0xFF616161),
            "BEKLİYOR",
            Icons.Default.CloudSync
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    val formatted = String.format(Locale.getDefault(), "%.1f", size / Math.pow(1024.0, digitGroups.toDouble()))
    return "$formatted ${units[digitGroups.coerceAtMost(units.size - 1)]}"
}

private fun formatDate(timestamp: Long): String {
    val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return df.format(Date(timestamp))
}
