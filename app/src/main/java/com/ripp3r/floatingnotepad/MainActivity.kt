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
            var hasOverlay by remember { mutableStateOf(PermissionHelper.canDrawOverlays(this)) }
            var hasStorage by remember { mutableStateOf(PermissionHelper.hasStoragePermission(this)) }
            
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(500)
                    hasOverlay = PermissionHelper.canDrawOverlays(this@MainActivity)
                    hasStorage = PermissionHelper.hasStoragePermission(this@MainActivity)
                }
            }
            
            FloatingNotepadTheme(darkTheme = isDarkMode) {
                DashboardScreen(
                    context = this,
                    isDarkMode = isDarkMode,
                    onThemeToggle = {
                        isDarkMode = !isDarkMode
                        ThemeManager.setDarkMode(this, isDarkMode)
                    },
                    onStartService = { startFloatingService() },
                    onStopService = { stopFloatingService() },
                    hasOverlayPermission = hasOverlay,
                    onRequestOverlayPermission = { PermissionHelper.requestOverlayPermission(this) },
                    hasStoragePermission = hasStorage,
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
    context: ComponentActivity,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Floating Notepad",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Quick access notepad overlay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Theme Toggle
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode")
                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                }
            }
            
            // Overlay Permission
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
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
            
            // Bubble Size Slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var bubbleSize by remember { mutableStateOf(ThemeManager.getBubbleSize(context)) }
                    Text("Bubble Size: ${bubbleSize}dp")
                    Slider(
                        value = bubbleSize.toFloat(),
                        onValueChange = { 
                            bubbleSize = it.toInt()
                            ThemeManager.setBubbleSize(context, bubbleSize)
                        },
                        onValueChangeFinished = {
                            // Restart service to apply new bubble size
                            context.stopService(Intent(context, FloatingWindowService::class.java))
                            context.startService(Intent(context, FloatingWindowService::class.java))
                        },
                        valueRange = 48f..96f,
                        steps = 11
                    )
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
