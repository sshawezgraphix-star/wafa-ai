package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary

data class QuickAction(
    val label: String,
    val prompt: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickAction("Research", "Maya, please research the latest AI breakthroughs today", Icons.Default.Search, NeonCyan),
        QuickAction("Call", "Maya, make a phone call", Icons.Default.Call, Color(0xFF00FF66)),
        QuickAction("WhatsApp", "Maya, send a WhatsApp message", Icons.Default.Chat, Color(0xFF25D366)),
        QuickAction("Torch", "Maya, turn on the flashlight", Icons.Default.FlashlightOn, Color(0xFFFFB800)),
        QuickAction("Battery", "Maya, what is my phone battery percentage?", Icons.Default.BatteryChargingFull, NeonCyan),
        QuickAction("Alarm", "Maya, set an alarm for 7:00 AM", Icons.Default.Alarm, NeonPink),
        QuickAction("Music", "Maya, play favorite hits on Spotify", Icons.Default.MusicNote, NeonPurple),
        QuickAction("Apps", "Maya, open YouTube app", Icons.Default.Apps, Color(0xFFFF3366))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            ActionChipItem(
                action = action,
                onClick = { onActionClick(action.prompt) }
            )
        }
    }
}

@Composable
fun ActionChipItem(
    action: QuickAction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, action.tint.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = action.tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = action.label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
