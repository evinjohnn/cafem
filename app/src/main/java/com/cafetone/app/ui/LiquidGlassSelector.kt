package com.cafetone.app.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiquidGlassSelector(
    items: List<String>,
    activeMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOf(activeMode).coerceAtLeast(0)
    var containerWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // Spring Animation for Fluid Movement
    // Using a slightly bouncier spring to match the "liquid" feel
    val animatedIndex = remember { Animatable(selectedIndex.toFloat()) }

    LaunchedEffect(selectedIndex) {
        animatedIndex.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 300f
            )
        )
    }

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1c1c1e)) // Dark background to make glass pop
            .onGloballyPositioned {
                containerWidth = it.size.width.toFloat()
            }
    ) {
        if (containerWidth > 0) {
            val itemWidth = containerWidth / items.size
            val pillWidth = itemWidth
            val pillHeight = with(density) { 68.dp.toPx() }
            val pillOffset = animatedIndex.value * itemWidth

            // --- THE LIQUID GLASS PILL ---
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Apply RenderEffect for blur on API 31+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = RenderEffect.createBlurEffect(
                                20f, 20f, Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                        alpha = 0.99f // Force layer creation
                    }
            ) {
                val pillSize = Size(pillWidth - 12.dp.toPx(), pillHeight - 12.dp.toPx())
                val pillTopLeft = Offset(pillOffset + 6.dp.toPx(), 6.dp.toPx())
                val cornerRadius = CornerRadius(50.dp.toPx(), 50.dp.toPx())

                // 1. Chromatic Aberration (RGB Split)
                // We draw the shape 3 times with slight offsets and additive blending
                with(drawContext.canvas.nativeCanvas) {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        // We'll use a simpler approach for Compose Canvas
                    }
                }

                // Red Channel (Offset Left)
                drawRoundRect(
                    color = Color(1f, 0f, 0f, 0.4f),
                    topLeft = pillTopLeft.copy(x = pillTopLeft.x - 2f),
                    size = pillSize,
                    cornerRadius = cornerRadius,
                    blendMode = BlendMode.Screen
                )

                // Blue Channel (Offset Right)
                drawRoundRect(
                    color = Color(0f, 0f, 1f, 0.4f),
                    topLeft = pillTopLeft.copy(x = pillTopLeft.x + 2f),
                    size = pillSize,
                    cornerRadius = cornerRadius,
                    blendMode = BlendMode.Screen
                )

                // Green Channel (Center)
                drawRoundRect(
                    color = Color(0f, 1f, 0f, 0.4f),
                    topLeft = pillTopLeft,
                    size = pillSize,
                    cornerRadius = cornerRadius,
                    blendMode = BlendMode.Screen
                )

                // 2. Glass Body (Main Fill)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.1f),
                    topLeft = pillTopLeft,
                    size = pillSize,
                    cornerRadius = cornerRadius
                )

                // 3. Specular Highlights (The "Liquid" Shine)
                // Top Highlight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        startY = pillTopLeft.y,
                        endY = pillTopLeft.y + pillSize.height * 0.4f
                    ),
                    topLeft = pillTopLeft,
                    size = pillSize,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Bottom Reflection
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.3f)
                        ),
                        startY = pillTopLeft.y + pillSize.height * 0.6f,
                        endY = pillTopLeft.y + pillSize.height
                    ),
                    topLeft = pillTopLeft,
                    size = pillSize,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // --- TEXT LABELS ---
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val isSelected = item == activeMode
                // Animate text color
                val textColor by animateColorAsState(
                    if (isSelected) Color.White else Color.Gray,
                    label = "TextColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onModeSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
