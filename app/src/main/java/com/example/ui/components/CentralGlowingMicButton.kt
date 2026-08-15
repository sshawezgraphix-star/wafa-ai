package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StateConnecting
import com.example.ui.theme.StateIdle
import com.example.ui.theme.StateListening
import com.example.ui.theme.StateSpeaking

@Composable
fun CentralGlowingMicButton(
    state: AssistantState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = when (state) {
        AssistantState.IDLE -> StateIdle
        AssistantState.CONNECTING -> StateConnecting
        AssistantState.LISTENING -> StateListening
        AssistantState.THINKING -> NeonPurple
        AssistantState.SPEAKING -> StateSpeaking
        AssistantState.ERROR -> Color(0xFFFF3366)
    }

    // Dynamic rotation for Sci-Fi HUD feel
    val rotationAnim = remember { Animatable(0f) }
    val pulseAnim = remember { Animatable(1f) }

    LaunchedEffect(state) {
        if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING || state == AssistantState.CONNECTING || state == AssistantState.THINKING) {
            rotationAnim.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotationAnim.snapTo(0f)
        }
    }

    LaunchedEffect(state) {
        if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING || state == AssistantState.CONNECTING) {
            pulseAnim.animateTo(
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAnim.snapTo(1f)
        }
    }

    val dynamicScale = pulseAnim.value + (amplitude * 0.35f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(240.dp)
    ) {
        // Hologram Arc Reactor Energy Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.3f * dynamicScale

            // 1. Ambient Glow Field
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor.copy(alpha = 0.5f), Color.Transparent),
                    center = centerOffset,
                    radius = radius * 1.4f
                ),
                radius = radius * 1.4f,
                center = centerOffset
            )

            // 2. Outer Dash Circuit Ring
            drawCircle(
                color = baseColor.copy(alpha = 0.7f),
                radius = radius,
                center = centerOffset,
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f), rotationAnim.value)
                )
            )

            // 3. Middle Sci-Fi Sweep Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(NeonCyan, NeonPurple, NeonPink, NeonCyan),
                    center = centerOffset
                ),
                radius = radius * 0.88f,
                center = centerOffset,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 4. Inner Ring with audio reactivity
            drawCircle(
                color = baseColor.copy(alpha = 0.4f + amplitude * 0.5f),
                radius = radius * 0.75f,
                center = centerOffset,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), -rotationAnim.value)
                )
            )
        }

        // Center Arc Reactor Button Core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .scale(if (state == AssistantState.SPEAKING) 1.08f else 1f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.95f),
                            Color(0xFF0F1322)
                        )
                    )
                )
                .border(2.5.dp, baseColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = baseColor),
                    onClick = onClick
                )
                .testTag("mic_button")
        ) {
            Icon(
                imageVector = when (state) {
                    AssistantState.SPEAKING -> Icons.Default.VolumeUp
                    AssistantState.THINKING -> Icons.Default.GraphicEq
                    AssistantState.IDLE -> Icons.Default.MicOff
                    else -> Icons.Default.Mic
                },
                contentDescription = state.label,
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}
