package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.model.AssistantState
import com.example.service.LiveSessionManager
import com.example.ui.components.AnimatedWaveformView
import com.example.ui.components.CentralGlowingMicButton
import com.example.ui.components.LiveTranscriptPanel
import com.example.ui.components.MicPermissionBanner
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.BuildConfig

@Composable
fun FirdousMainScreen() {
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    val audioStreamer = remember { AudioStreamer(context) }
    val sessionManager = remember {
        LiveSessionManager(
            context = context,
            audioStreamer = audioStreamer,
            apiKey = BuildConfig.GEMINI_API_KEY
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
                            Color(0xFF0F1224),
                            CyberBackground,
                            Color(0xFF080912)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                TopHeaderBar(
                    state = state,
                    toolCount = toolCalls.size
                )

                // Error / Secret Banner
                if (errorMessage != null) {
                    ErrorNotificationCard(errorMessage!!)
                }

                // Permission Banner if required
                if (!hasMicPermission) {
                    MicPermissionBanner(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Central Assistant Status Label
                Text(
                    text = "FIRDOUS AI",
                    color = NeonCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )

                Text(
                    text = "Created by Shawez Hacker",
                    color = NeonPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Central Glowing Mic Button
                CentralGlowingMicButton(
                    state = state,
                    amplitude = currentAmplitude,
                    onClick = {
                        if (hasMicPermission) {
                            sessionManager.toggleSession()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                Text(
                    text = state.label,
                    color = when (state) {
                        AssistantState.LISTENING -> NeonCyan
                        AssistantState.SPEAKING -> NeonPink
                        AssistantState.CONNECTING -> Color(0xFFFFB800)
                        else -> TextSecondary
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Real-Time Animated Waveform
                AnimatedWaveformView(
                    state = state,
                    amplitude = currentAmplitude
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Transcripts and Function Tool Logs
                LiveTranscriptPanel(
                    messages = messages,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TopHeaderBar(
    state: AssistantState,
    toolCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = when (state) {
                            AssistantState.LISTENING -> Color(0xFF00FF66)
                            AssistantState.SPEAKING -> NeonPink
                            AssistantState.CONNECTING -> Color(0xFFFFB800)
                            else -> TextMuted
                        },
                        shape = RoundedCornerShape(5.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Voice-to-Voice PCM16",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (toolCount > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tools Executed",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$toolCount Browser Tools",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorNotificationCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("error_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF331418)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF4D4D),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = Color(0xFFFFD6D6),
                fontSize = 13.sp
            )
        }
    }
}
