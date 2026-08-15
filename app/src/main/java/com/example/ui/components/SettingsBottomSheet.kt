package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppSettingsManager
import com.example.data.AssistantVoice
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settingsManager: AppSettingsManager,
    sheetState: SheetState,
    onRequestAllPermissions: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var apiKeyText by remember { mutableStateOf(settingsManager.getApiKey()) }
    var assistantNameText by remember { mutableStateOf(settingsManager.getAssistantName()) }
    var selectedVoice by remember { mutableStateOf(settingsManager.getSelectedVoice()) }
    var showApiKey by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF333A52))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Settings",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MAYA AI SETTINGS",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Mark-XXXIX Mobile Assistant Configuration",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // 1. Gemini API Key Input
            Text(
                text = "GEMINI API KEY (REQUIRED FOR REALTIME VOICE)",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = apiKeyText,
                onValueChange = {
                    apiKeyText = it
                    saveStatus = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Paste Gemini API Key (AIzaSy...)", color = TextMuted, fontSize = 13.sp)
                },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle API Key visibility",
                            tint = NeonCyan
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0xFF2B334E),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberSurfaceVariant,
                    unfocusedContainerColor = CyberSurfaceVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Assistant Persona Name
            Text(
                text = "ASSISTANT NAME",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = assistantNameText,
                onValueChange = {
                    assistantNameText = it
                    saveStatus = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Maya / Firdous", color = TextMuted, fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0xFF2B334E),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberSurfaceVariant,
                    unfocusedContainerColor = CyberSurfaceVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Permissions Status & One-Tap Grant Card
            Text(
                text = "PHONE PERMISSIONS (AUTOMATIC)",
                color = NeonPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    PermissionStatusRow("Microphone (Voice)", hasMic, Icons.Default.Mic)
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow("Camera (Live Vision & Torch)", hasCamera, Icons.Default.CameraAlt)
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow("Phone Calls (Direct Dialing)", hasCall, Icons.Default.Call)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRequestAllPermissions,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("App Info", color = NeonCyan, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Realtime Female Voice Selection
            Text(
                text = "FEMALE VOICE SELECTION (GEMINI LIVE 2.5)",
                color = NeonPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSettingsManager.AVAILABLE_FEMALE_VOICES.forEach { voice ->
                    VoiceSelectionCard(
                        voice = voice,
                        isSelected = voice.id == selectedVoice,
                        onSelect = {
                            selectedVoice = voice.id
                            saveStatus = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (saveStatus != null) {
                Text(
                    text = saveStatus!!,
                    color = Color(0xFF00FF66),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Save Button
            Button(
                onClick = {
                    settingsManager.setApiKey(apiKeyText)
                    settingsManager.setAssistantName(assistantNameText.ifBlank { "Maya" })
                    settingsManager.setSelectedVoice(selectedVoice)
                    saveStatus = "Settings Saved Successfully! Ready to connect."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SAVE CONFIGURATION",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    isGranted: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isGranted) Color(0xFF00FF66) else Color(0xFFFFB800), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = TextPrimary, fontSize = 12.5.sp)
        }
        Text(
            text = if (isGranted) "Granted ✅" else "Required ⚠️",
            color = if (isGranted) Color(0xFF00FF66) else Color(0xFFFFB800),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VoiceSelectionCard(
    voice: AssistantVoice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonCyan else Color(0xFF22283E),
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF16213B) else CyberSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0xFF101422)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = voice.name,
                    tint = if (isSelected) NeonCyan else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = voice.name,
                        color = if (isSelected) NeonCyan else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${voice.gender})",
                        color = NeonPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = voice.description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
