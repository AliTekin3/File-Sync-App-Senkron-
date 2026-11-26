package com.example.senkron

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.senkron.data.drive.GoogleDriveManager
import com.example.senkron.ui.SyncDashboardScreen
import com.example.senkron.ui.SyncViewModel
import com.example.senkron.ui.theme.SenkronTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    private val viewModel: SyncViewModel by viewModels()
    private lateinit var driveManager: GoogleDriveManager

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.onSignedIn(account, this)
                Toast.makeText(this, "Google Hesabı Bağlandı!", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                e.printStackTrace()
                Toast.makeText(this, "Giriş Hatası: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.scanFiles(this)
        } else {
            Toast.makeText(this, "Dosyaları senkronize etmek için depolama izni gereklidir.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        driveManager = GoogleDriveManager(this)

        requestRequiredPermissions()

        setContent {
            SenkronTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.checkAccountAndScan(this@MainActivity)
                    }

                    SyncDashboardScreen(
                        uiState = uiState,
                        onSignInClick = { launchGoogleSignIn() },
                        onSignOutClick = { signOutGoogle() },
                        onWifiOnlyChanged = { enabled ->
                            viewModel.setWifiOnly(this@MainActivity, enabled)
                        },
                        onFolderChanged = { folderName ->
                            viewModel.updateTargetFolder(folderName)
                        },
                        onOpenDriveClick = {
                            viewModel.openGoogleDrive(this@MainActivity)
                        },
                        onStartSyncClick = {
                            viewModel.startManualSync(this@MainActivity)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.scanFiles(this)
    }

    private fun launchGoogleSignIn() {
        val gso = driveManager.getGoogleSignInOptions()
        val client = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(client.signInIntent)
    }

    private fun signOutGoogle() {
        val gso = driveManager.getGoogleSignInOptions()
        val client = GoogleSignIn.getClient(this, gso)
        client.signOut().addOnCompleteListener {
            viewModel.onSignedIn(null, this)
            Toast.makeText(this, "Oturum Kapatıldı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}
