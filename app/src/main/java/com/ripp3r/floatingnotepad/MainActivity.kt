package com.ripp3r.floatingnotepad

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ripp3r.floatingnotepad.service.FloatingWindowService
import com.ripp3r.floatingnotepad.ui.theme.FloatingNotepadTheme
import com.ripp3r.floatingnotepad.utils.PermissionHelper
import com.ripp3r.floatingnotepad.utils.ThemeManager

class MainActivity : ComponentActivity() {
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(ThemeManager.isDarkMode(this)) }
            
            FloatingNotepadTheme(darkTheme = isDarkMode) {
                DashboardScreen(
                    isDarkMode = isDarkMode,
                    onThemeToggle = {
                        isDarkMode = !isDarkMode
                        ThemeManager.setDarkMode(this, isDarkMode)
                    },
                    onStartService = { startFloatingService() },
                    onStopService = { stopFloatingService() },
                    hasOverlayPermission = PermissionHelper.canDrawOverlays(this),
                    onRequestOverlayPermission = { PermissionHelper.requestOverlayPermission(this) },
                    hasStoragePermission = PermissionHelper.hasStoragePermission(this),
                    onRequestStoragePermission = {
                        val permissions = PermissionHelper.getStoragePermissions()
                        if (permissions.isNotEmpty()) {
                            storagePermissionLauncher.launch(permissions)
                        }
                    }
                )
            }
        }
    }
    
    private fun startFloatingService() {
        if (PermissionHelper.canDrawOverlays(this)) {
            startService(Intent(this, FloatingWindowService::class.java))
        }
    }
    
    private fun stopFloatingService() {
        stopService(Intent(this, FloatingWindowService::class.java))
    }
}

@Composable
fun DashboardScreen(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Floating Notepad",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Theme Toggle
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode")
                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                }
            }
            
            // Overlay Permission
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Overlay Permission")
                        Text(
                            text = if (hasOverlayPermission) "✓" else "✗",
                            color = if (hasOverlayPermission) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    if (!hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestOverlayPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
            
            // Storage Permission
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Storage Permission")
                        Text(
                            text = if (hasStoragePermission) "✓" else "✗",
                            color = if (hasStoragePermission) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    if (!hasStoragePermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestStoragePermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Service Controls
            Button(
                onClick = onStartService,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasOverlayPermission
            ) {
                Text("Start Floating Notepad")
            }
            
            Button(
                onClick = onStopService,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Floating Notepad")
            }
        }
    }
}
