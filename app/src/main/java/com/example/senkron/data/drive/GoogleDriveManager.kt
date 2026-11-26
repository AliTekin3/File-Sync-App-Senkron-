package com.example.senkron.data.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.senkron.data.model.SyncedFile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GoogleDriveManager(private val context: Context) {

    companion object {
        const val DEFAULT_DRIVE_FOLDER_NAME = "Senkron_Cloud_Backup"
        val REQUIRED_SCOPES = arrayOf(
            Scope(DriveScopes.DRIVE_FILE)
        )
    }

    fun getGoogleSignInOptions(): GoogleSignInOptions {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        for (scope in REQUIRED_SCOPES) {
            gso.requestScopes(scope)
        }
        return gso.build()
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, *REQUIRED_SCOPES)) {
            account
        } else {
            null
        }
    }

    fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Senkron")
            .build()
    }

    /**
     * Google Drive üzerinde özelleştirilebilir senkronizasyon klasörünü alır veya oluşturur.
     */
    suspend fun getOrCreateBackupFolder(
        driveService: Drive,
        folderName: String = DEFAULT_DRIVE_FOLDER_NAME
    ): String? = withContext(Dispatchers.IO) {
        try {
            val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and trashed = false"
            val result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()

            val filesList = result.files
            if (!filesList.isNullOrEmpty()) {
                return@withContext filesList[0].id
            }

            // Klasör yoksa yeni oluştur
            val folderMetadata = DriveFile().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
            }

            val createdFolder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()

            createdFolder.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Tek bir dosyayı hedef Google Drive klasörüne yükler.
     */
    suspend fun uploadFile(
        account: GoogleSignInAccount,
        syncedFile: SyncedFile,
        folderName: String = DEFAULT_DRIVE_FOLDER_NAME
    ): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            val parentFolderId = getOrCreateBackupFolder(driveService, folderName) ?: return@withContext null
            val localFile = File(syncedFile.path)

            if (!localFile.exists()) return@withContext null

            // Drive'da aynı isimde dosya var mı kontrol et
            val query = "'$parentFolderId' in parents and name = '${syncedFile.name}' and trashed = false"
            val result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()

            val existingFiles = result.files

            val mediaContent = FileContent(syncedFile.mimeType, localFile)

            if (!existingFiles.isNullOrEmpty()) {
                // Var olan dosyayı güncelle
                val existingFileId = existingFiles[0].id
                val updatedFile = driveService.files().update(existingFileId, null, mediaContent)
                    .setFields("id")
                    .execute()
                return@withContext updatedFile.id
            } else {
                // Yeni dosya olarak yükle
                val fileMetadata = DriveFile().apply {
                    name = syncedFile.name
                    parents = listOf(parentFolderId)
                }

                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                return@withContext uploadedFile.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Kullanıcıyı doğrudan Google Drive uygulamasına veya web klasörüne yönlendirir.
     */
    fun openGoogleDriveFolder(account: GoogleSignInAccount) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://drive.google.com/drive/my-drive")
                setPackage("com.google.android.apps.docs")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Google Drive uygulaması yoksa tarayıcıda aç
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://drive.google.com/drive/my-drive")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }
}
