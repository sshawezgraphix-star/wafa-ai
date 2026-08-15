package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceMessage
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveTranscriptPanel(
    messages: List<VoiceMessage>,
    assistantName: String = "Maya",
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tap the Arc Reactor or speak to $assistantName",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try: \"Torch on karo\", \"Research AI trends\", \"WhatsApp pe message bhejo\"",
                    color = Color(0xFF4A5578),
                    fontSize = 11.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("transcript_panel"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed(), key = { it.id }) { msg ->
                TranscriptItemCard(msg, assistantName)
            }
        }
    }
}

@Composable
fun TranscriptItemCard(
    msg: VoiceMessage,
    assistantName: String
) {
    val isUser = msg.sender == VoiceMessage.Sender.USER
    val isSystem = msg.sender == VoiceMessage.Sender.SYSTEM
    val isResearch = msg.sender == VoiceMessage.Sender.RESEARCH
    val isTool = msg.sender == VoiceMessage.Sender.TOOL

    val cardBg = when {
        isUser -> CyberSurfaceVariant
        isSystem -> Color(0xFF131828)
        isResearch -> Color(0xFF10223A)
        isTool -> Color(0xFF1F1830)
        else -> CyberSurface
    }

    val icon = when {
        isUser -> Icons.Default.Person
        isSystem || isTool -> Icons.Default.Build
        isResearch -> Icons.Default.Search
        else -> Icons.Default.SmartToy
    }

    val tint = when {
        isUser -> NeonCyan
        isSystem -> NeonPurple
        isResearch -> Color(0xFF00FFCC)
        isTool -> Color(0xFFFFB800)
        else -> NeonPink
    }

    val label = when {
        isUser -> "You"
        isSystem -> "System Action"
        isResearch -> "Deep Research"
        isTool -> "Phone Automation"
        else -> assistantName
    }

    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = tint.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = tint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timeStr,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = msg.text,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
