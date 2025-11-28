package com.cafetone.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Constants & Colors ---
val BgColor = Color(0xFF1c1c1e)
val PillColor = Color(0xFF2c2c2e)
val ActiveTextBlue = Color(0xFF60a5fa) // Tailwind blue-400 equivalent
val InactiveTextGray = Color(0xFFa3a3a3) // Tailwind neutral-400
val GlowCyan = Color(0xFF06b6d4)
val GlowPurple = Color(0xFFa855f7)
val GlowBlue = Color(0xFF3b82f6)

@Composable
fun ChromaticPillSelector(
    items: List<String>,
    activeMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOf(activeMode).coerceAtLeast(0)
    var containerWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // Animation State for the pill's position
    val pillOffset = remember { Animatable(selectedIndex.toFloat()) }

    LaunchedEffect(selectedIndex) {
        pillOffset.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.85f, // Calculated to match Framer's 'damping: 25' relative to stiffness
                stiffness = 250f      // Matching Framer's 'stiffness: 250'
            )
        )
    }

    // Infinite rotation for the chromatic glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow-spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .height(68.dp) // Match provided height
            .clip(RoundedCornerShape(50)) // Match pill shape
            .background(BgColor)
            .onGloballyPositioned {
                containerWidth = it.size.width.toFloat()
            }
    ) {
        if (containerWidth > 0) {
            val pillWidth = containerWidth / items.size
            val pillPadding = 6.dp
            val pillWidthDp = with(density) { pillWidth.toDp() }

            // --- LAYER 1: The Moving Pill (Background) ---
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(pillWidthDp)
                    .offset(x = with(density) { (pillWidth * pillOffset.value).toDp() })
                    .padding(pillPadding)
            ) {
                // 1.1 The Chromatic Glow (Rotating Conic Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.05f
                            scaleY = 1.05f
                            this.alpha = 0.8f
                        }
                        .blur(8.dp) // Creates the soft light leak
                        .rotate(rotation) // The spin
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    GlowCyan,
                                    GlowPurple,
                                    GlowBlue,
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // 1.2 The Sharp Edge Glow (Inner definition)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(1.dp)
                        .blur(1.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GlowCyan, GlowPurple, GlowBlue)
                            ),
                            shape = CircleShape
                        )
                )

                // 1.3 The Dark Inner Pill (Mask)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp) // Inset to show the glow behind
                        .background(PillColor, CircleShape)
                )
            }
        }

        // --- LAYER 2: The Interactive Buttons (Foreground) ---
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            items.forEach { item ->
                TabButton(
                    modifier = Modifier.weight(1f),
                    title = item,
                    isActive = item == activeMode,
                    onClick = { onModeSelected(item) }
                )
            }
        }
    }
}

@Composable
fun TabButton(
    modifier: Modifier = Modifier,
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Animate color transition
    val contentColor by animateColorAsState(
        targetValue = if (isActive) ActiveTextBlue else InactiveTextGray,
        label = "textColor"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Remove default ripple for cleaner look
            ) { onClick() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}
