package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioStreamer
import com.example.data.AppSettingsManager
import com.example.model.AssistantState
import com.example.service.AppUpdateManager
import com.example.service.LiveSessionManager
import com.example.ui.components.AnimatedWaveformView
import com.example.ui.components.CentralGlowingMicButton
import com.example.ui.components.LiveTranscriptPanel
import com.example.ui.components.MicPermissionBanner
import com.example.ui.components.NotesBottomSheet
import com.example.ui.components.QuickActionChips
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.theme.CyberBackground
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
fun FirdousMainScreen() {
    val context = LocalContext.current

    val settingsManager = remember { AppSettingsManager(context) }
    val updateManager = remember { AppUpdateManager(context) }
    val assistantName by settingsManager.assistantNameFlow.collectAsState()
    val selectedVoice by settingsManager.voiceFlow.collectAsState()
    val apiKey by settingsManager.apiKeyFlow.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }

    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasMicPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasMicPermission
    }

    val audioStreamer = remember { AudioStreamer(context) }
    val sessionManager = remember {
        LiveSessionManager(
            context = context,
            audioStreamer = audioStreamer,
            settingsManager = settingsManager
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            sessionManager.disconnectSession()
            audioStreamer.release()
        }
    }

    val state by sessionManager.state.collectAsState()
    val micAmplitude by audioStreamer.amplitude.collectAsState()
    val speakerAmplitude by audioStreamer.outputAmplitude.collectAsState()
    val messages by sessionManager.messages.collectAsState()
    val toolCalls by sessionManager.toolCalls.collectAsState()
    val errorMessage by sessionManager.errorMessage.collectAsState()

    val currentAmplitude = if (state == AssistantState.SPEAKING) speakerAmplitude else micAmplitude

    var textInput by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0C0F1D),
                            CyberBackground,
                            Color(0xFF06070E)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top HUD Header Bar
                TopHudHeaderBar(
                    state = state,
                    assistantName = assistantName,
                    voiceName = selectedVoice,
                    toolCount = toolCalls.size,
                    onOpenSettings = { showSettingsSheet = true },
                    onOpenNotes = { showNotesSheet = true }
                )

                // 2. Error / Missing API Key Notification Card
                if (errorMessage != null) {
                    ErrorNotificationCard(
                        message = errorMessage!!,
                        onOpenSettings = { showSettingsSheet = true }
                    )
                }

                // 3. Permission Banner
                if (!hasMicPermission) {
                    MicPermissionBanner(
                        onRequestPermission = {
                            multiplePermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.CALL_PHONE
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Mark-XXXIX Title Branding
                Text(
                    text = "$assistantName AI • MARK-XXXIX",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = "Created by Shawez Hacker",
                    color = NeonPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Central Glowing Arc Reactor Core
                CentralGlowingMicButton(
                    state = state,
                    amplitude = currentAmplitude,
                    onClick = {
                        if (!hasMicPermission) {
                            multiplePermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.CALL_PHONE
                                )
                            )
                        }
                        sessionManager.toggleSession()
                    }
                )

                // Status Label
                Text(
                    text = state.label,
                    color = when (state) {
                        AssistantState.LISTENING -> NeonCyan
                        AssistantState.SPEAKING -> NeonPink
                        AssistantState.CONNECTING -> Color(0xFFFFB800)
                        AssistantState.THINKING -> NeonPurple
                        else -> TextSecondary
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Real-Time Dynamic Waveform
                AnimatedWaveformView(
                    state = state,
                    amplitude = currentAmplitude
                )

                // 7. Quick Action Chips Row
                QuickActionChips(
                    onActionClick = { prompt ->
                        sessionManager.sendTextMessage(prompt)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 8. Live Transcript & Tool Execution Panel
                LiveTranscriptPanel(
                    messages = messages,
                    assistantName = assistantName,
                    modifier = Modifier.weight(1f)
                )

                // 9. Bottom Text Input Bar (Fallback)
                BottomChatInputBar(
                    textValue = textInput,
                    onTextChange = { textInput = it },
                    onSend = {
                        if (textInput.isNotBlank()) {
                            sessionManager.sendTextMessage(textInput)
                            textInput = ""
                        }
                    }
                )
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            settingsManager = settingsManager,
            updateManager = updateManager,
            sheetState = settingsSheetState,
            onRequestAllPermissions = {
                multiplePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.CALL_PHONE
                    )
                )
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Notes Bottom Sheet
    if (showNotesSheet) {
        NotesBottomSheet(
            settingsManager = settingsManager,
            sheetState = notesSheetState,
            onDismiss = { showNotesSheet = false }
        )
    }
}

@Composable
fun TopHudHeaderBar(
    state: AssistantState,
    assistantName: String,
    voiceName: String,
    toolCount: Int,
    onOpenSettings: () -> Unit,
    onOpenNotes: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status & Voice Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CyberSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        color = when (state) {
                            AssistantState.LISTENING -> Color(0xFF00FF66)
                            AssistantState.SPEAKING -> NeonPink
                            AssistantState.CONNECTING -> Color(0xFFFFB800)
                            AssistantState.THINKING -> NeonPurple
                            else -> TextMuted
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$voiceName (Female Voice)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Action Icons (Notes, Tool count, Settings)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (toolCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2845))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$toolCount Actions",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            IconButton(
                onClick = onOpenNotes,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Saved Notes",
                    tint = NeonPurple,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BottomChatInputBar(
    textValue: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Type a command or research topic...",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0xFF242A42),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = CyberSurfaceVariant,
                unfocusedContainerColor = CyberSurfaceVariant
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (textValue.isNotBlank()) NeonCyan else Color(0xFF22283E))
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send Command",
                tint = if (textValue.isNotBlank()) Color.Black else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ErrorNotificationCard(
    message: String,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpenSettings)
            .testTag("error_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF331418)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF4D4D),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    color = Color(0xFFFFD6D6),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Text(
                text = "SETTINGS ⚙️",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
