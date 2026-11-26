package com.example.senkron.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.example.senkron.data.model.SyncStatus
import com.example.senkron.data.model.SyncedFile
import java.io.File
import java.util.Locale

object FileFilterHelper {

    // Fotoğraf uzantıları (Kesinlikle Hariç Tutulacaklar)
    private val PHOTO_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "heic", "heif",
        "gif", "bmp", "tiff", "tif", "raw", "dng", "svg", "ico"
    )

    // Yalnızca İzin Verilen Yararlı Belge ve Dosya Uzantıları (Beyaz Liste)
    private val ALLOWED_DOCUMENT_EXTENSIONS = setOf(
        // Belgeler ve Ofis Dosyaları (PDF, Word, Excel, PowerPoint)
        "pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx",
        // Not Defterleri ve Metin Dosyaları
        "txt", "rtf", "md",
        // Arşiv ve Sıkıştırılmış Dosyalar (ZIP, RAR, 7Z vb.)
        "zip", "rar", "7z", "tar", "gz",
        // Kurulum Dosyaları (APK)
        "apk",
        // WhatsApp Sticker'ları ve Ses/Medya Belgeleri
        "webp", "opus", "m4a", "ogg"
    )

    fun isPhoto(fileName: String, mimeType: String?): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
        if (PHOTO_EXTENSIONS.contains(extension)) {
            return true
        }
        // Standart fotoğrafları hariç tut (WhatsApp sticker webp hariç)
        if (extension != "webp" && mimeType?.startsWith("image/", ignoreCase = true) == true) {
            return true
        }
        return false
    }

    /**
     * Dosyanın yedeklenecek yararlı bir belge/not/arşiv/APK/WhatsApp belgesi olup olmadığını kontrol eder.
     */
    fun isSyncableDocument(fileName: String, mimeType: String?): Boolean {
        // Gizli veya geçici sistem dosyalarını atla
        if (fileName.startsWith(".")) return false

        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
        if (extension.isEmpty()) return false

        // Eğer resimse (ör. JPG, PNG, HEIC) hariç tut
        if (isPhoto(fileName, mimeType)) return false

        // Yalnızca tanımlı yararlı uzantılara izin ver
        return ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)
    }

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.getDefault())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    /**
     * MediaStore kullanarak cihazdaki YALNIZCA yararlı belgeleri, notları, arşivleri ve WhatsApp dokümanlarını getirir.
     */
    fun getSyncableFilesFromMediaStore(context: Context): List<SyncedFile> {
        val syncableFiles = mutableListOf<SyncedFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val queryUri: Uri = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(
            queryUri,
            projection,
            null,
            null,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn).toString()
                val name = cursor.getString(nameColumn) ?: continue
                val path = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeColumn) ?: "application/octet-stream"
                val dateModified = cursor.getLong(dateColumn) * 1000L

                // Yalnızca filtreden geçen yararlı belgeleri ekle
                if (isSyncableDocument(name, mimeType) && path.isNotEmpty() && size > 0) {
                    syncableFiles.add(
                        SyncedFile(
                            id = id,
                            name = name,
                            path = path,
                            size = size,
                            mimeType = mimeType,
                            lastModified = dateModified,
                            syncStatus = SyncStatus.PENDING
                        )
                    )
                }
            }
        }

        return syncableFiles
    }
}
