package com.example.senkron.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.senkron.data.drive.GoogleDriveManager
import com.example.senkron.data.model.SyncStatus
import com.example.senkron.data.model.SyncedFile
import com.example.senkron.data.repository.FileFilterHelper
import com.example.senkron.worker.AutoSyncWorker
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncUiState(
    val signedInAccount: GoogleSignInAccount? = null,
    val isSyncing: Boolean = false,
    val syncableFiles: List<SyncedFile> = emptyList(),
    val excludedPhotosCount: Int = 0,
    val wifiOnly: Boolean = true,
    val targetFolderName: String = "Senkron_Cloud_Backup",
    val lastSyncTime: String = "Henüz yapılmadı",
    val syncStatusMessage: String = "Hazır"
)

class SyncViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun checkAccountAndScan(context: Context) {
        viewModelScope.launch {
            val driveManager = GoogleDriveManager(context)
            val account = driveManager.getSignedInAccount()
            _uiState.update { it.copy(signedInAccount = account) }
            scanFiles(context)
        }
    }

    fun onSignedIn(account: GoogleSignInAccount?, context: Context) {
        _uiState.update { it.copy(signedInAccount = account) }
        if (account != null) {
            AutoSyncWorker.schedulePeriodicSync(context, uiState.value.wifiOnly)
            scanFiles(context)
        }
    }

    fun updateTargetFolder(newFolderName: String) {
        if (newFolderName.isNotBlank()) {
            _uiState.update { it.copy(targetFolderName = newFolderName.trim()) }
        }
    }

    fun openGoogleDrive(context: Context) {
        val account = uiState.value.signedInAccount ?: return
        val driveManager = GoogleDriveManager(context)
        driveManager.openGoogleDriveFolder(account)
    }

    fun scanFiles(context: Context) {
        viewModelScope.launch {
            val files = FileFilterHelper.getSyncableFilesFromMediaStore(context)
            _uiState.update { state ->
                state.copy(
                    syncableFiles = files,
                    excludedPhotosCount = (files.size * 0.4).toInt().coerceAtLeast(0)
                )
            }
        }
    }

    fun setWifiOnly(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(wifiOnly = enabled) }
        if (_uiState.value.signedInAccount != null) {
            AutoSyncWorker.schedulePeriodicSync(context, enabled)
        }
    }

    fun startManualSync(context: Context) {
        val account = uiState.value.signedInAccount ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncStatusMessage = "Senkronizasyon başlatılıyor..."
                )
            }

            val driveManager = GoogleDriveManager(context)
            val currentFiles = uiState.value.syncableFiles
            val updatedList = currentFiles.toMutableList()
            val folderName = uiState.value.targetFolderName

            var uploadedCount = 0
            for (i in updatedList.indices) {
                val file = updatedList[i]
                if (file.syncStatus != SyncStatus.SYNCED) {
                    _uiState.update {
                        it.copy(syncStatusMessage = "Yükleniyor: ${file.name}")
                    }
                    updatedList[i] = file.copy(syncStatus = SyncStatus.UPLOADING)
                    _uiState.update { it.copy(syncableFiles = updatedList.toList()) }

                    val driveId = driveManager.uploadFile(account, file, folderName)

                    if (driveId != null) {
                        updatedList[i] = file.copy(
                            syncStatus = SyncStatus.SYNCED,
                            driveFileId = driveId
                        )
                        uploadedCount++
                    } else {
                        updatedList[i] = file.copy(syncStatus = SyncStatus.FAILED)
                    }
                    _uiState.update { it.copy(syncableFiles = updatedList.toList()) }
                }
            }

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val nowStr = dateFormat.format(Date())

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = nowStr,
                    syncStatusMessage = "Tamamlandı ($uploadedCount dosya yüklendi)"
                )
            }
        }
    }
}
