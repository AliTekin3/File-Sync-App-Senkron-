package com.example.senkron

import com.example.senkron.data.repository.FileFilterHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileFilterTest {

    @Test
    fun testValidDocumentsAreAccepted() {
        assertTrue(FileFilterHelper.isSyncableDocument("document.pdf", "application/pdf"))
        assertTrue(FileFilterHelper.isSyncableDocument("report.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertTrue(FileFilterHelper.isSyncableDocument("sheet.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        assertTrue(FileFilterHelper.isSyncableDocument("notes.txt", "text/plain"))
        assertTrue(FileFilterHelper.isSyncableDocument("archive.zip", "application/zip"))
        assertTrue(FileFilterHelper.isSyncableDocument("backup.rar", "application/x-rar-compressed"))
        assertTrue(FileFilterHelper.isSyncableDocument("app.apk", "application/vnd.android.package-archive"))
        assertTrue(FileFilterHelper.isSyncableDocument("sticker.webp", "image/webp"))
        assertTrue(FileFilterHelper.isSyncableDocument("voice_note.opus", "audio/opus"))
    }

    @Test
    fun testPhotosAndJunkFilesAreRejected() {
        // Fotoğraflar reddedilmeli
        assertFalse(FileFilterHelper.isSyncableDocument("sample.jpg", "image/jpeg"))
        assertFalse(FileFilterHelper.isSyncableDocument("photo.PNG", "image/png"))
        assertFalse(FileFilterHelper.isSyncableDocument("image.heic", "image/heic"))

        // Sistem ve çöp geçici dosyalar reddedilmeli
        assertFalse(FileFilterHelper.isSyncableDocument("cache.tmp", "application/octet-stream"))
        assertFalse(FileFilterHelper.isSyncableDocument("data.dat", "application/octet-stream"))
        assertFalse(FileFilterHelper.isSyncableDocument("app.db", "application/x-sqlite3"))
        assertFalse(FileFilterHelper.isSyncableDocument(".hidden_file.txt", "text/plain"))
    }
}
