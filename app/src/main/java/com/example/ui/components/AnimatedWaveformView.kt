package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StateConnecting
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AnimatedWaveformView(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val phaseAnim = remember { Animatable(0f) }

    LaunchedEffect(state) {
        phaseAnim.animateTo(
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    val waveColor = when (state) {
        AssistantState.LISTENING -> NeonCyan
        AssistantState.SPEAKING -> NeonPink
        AssistantState.CONNECTING -> StateConnecting
        else -> NeonPurple.copy(alpha = 0.5f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val baseAmplitude = when (state) {
            AssistantState.LISTENING -> 25.dp.toPx() * (0.3f + amplitude * 1.5f)
            AssistantState.SPEAKING -> 35.dp.toPx() * (0.4f + amplitude * 2f)
            AssistantState.CONNECTING -> 15.dp.toPx()
            else -> 6.dp.toPx()
        }

        val path1 = Path()
        val path2 = Path()

        val waveCount = 2.5f
        val step = 10f

        path1.moveTo(0f, centerY)
        path2.moveTo(0f, centerY)

        var x = 0f
        while (x <= width) {
            val progress = x / width
            val envelope = sin(progress * PI).toFloat() // Smooth fade at edges

            val y1 = centerY + sin((progress * waveCount * 2 * PI) + phaseAnim.value).toFloat() * baseAmplitude * envelope
            val y2 = centerY + sin((progress * waveCount * 2 * PI) - phaseAnim.value + 1.2f).toFloat() * (baseAmplitude * 0.7f) * envelope

            path1.lineTo(x, y1)
            path2.lineTo(x, y2)

            x += step
        }

        drawPath(
            path = path1,
            brush = Brush.horizontalGradient(
                colors = listOf(NeonCyan, waveColor, NeonPink)
            ),
            style = Stroke(width = 3.dp.toPx())
        )

        drawPath(
            path = path2,
            brush = Brush.horizontalGradient(
                colors = listOf(NeonPurple, NeonPink, NeonCyan)
            ),
            style = Stroke(width = 1.8.dp.toPx())
        )
    }
}
