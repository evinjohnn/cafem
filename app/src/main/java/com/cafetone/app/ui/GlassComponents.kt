package com.cafetone.app.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// --- THEME COLORS ---
val ColorStandard = Color(0xFF00E5FF) // Cyan
val ColorCafe = Color(0xFFFF9100) // Amber/Orange
val ColorCinema = Color(0xFF6200EA) // Deep Purple
val ColorGlass = Color(0x05FFFFFF) // Ultra thin white for glass
val ColorGlassBorder = Color(0x20FFFFFF) // Subtle border
val ColorTextMain = Color(0xFFEEEEEE)
val ColorTextDim = Color(0x80EEEEEE)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
    ) {
        // 1. Background Layer (Blurred)
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    // Real-time Blur for Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect
                            .createBlurEffect(
                                40f, 40f, 
                                android.graphics.Shader.TileMode.MIRROR
                            )
                            .asComposeRenderEffect()
                    }
                    alpha = 0.98f // Slight transparency
                }
                .background(ColorGlass)
                .border(1.dp, Brush.linearGradient(
                    colors = listOf(Color(0x40FFFFFF), Color(0x05FFFFFF)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ), RoundedCornerShape(cornerRadius))
        )
        
        // 2. Content Layer (Unblurred)
        Box(modifier = Modifier.matchParentSize()) {
            content()
        }
    }
}

@Composable
fun LiquidBackground(primaryColor: Color) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // API 33+: Use AGSL Shader for "Million Dollar" Liquid
        LiquidShaderBackground(primaryColor)
    } else {
        // Fallback: Canvas Animation
        LiquidCanvasBackground(primaryColor)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LiquidShaderBackground(primaryColor: Color) {
    val time by rememberInfiniteTransition(label = "Time").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "TimeAnim"
    )
    
    // Simple Mesh Gradient Shader
    // Note: In a real production app, this AGSL string would be more complex.
    // We mix the primary color with deep purples and blacks.
    val shader = remember {
        RuntimeShader("""
            uniform float time;
            uniform float2 resolution;
            uniform float4 color;
            
            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / resolution.xy;
                
                // Fluid movement
                float t = time * 0.5;
                float2 p = uv * 2.0 - 1.0;
                
                float noise = sin(p.x * 3.0 + t) * cos(p.y * 3.0 + t);
                float dist = length(p + float2(sin(t * 0.3), cos(t * 0.2)) * 0.5);
                
                float glow = 1.0 / (dist * 2.0 + 0.5);
                
                // Mix colors
                float4 base = float4(0.0, 0.0, 0.0, 1.0);
                float4 accent = color * glow * 0.6;
                float4 fluid = float4(0.2, 0.0, 0.4, 1.0) * (noise * 0.5 + 0.5) * 0.3;
                
                return base + accent + fluid;
            }
        """.trimIndent())
    }
    
    val brush = remember(primaryColor) {
        ShaderBrush(shader)
    }
    
    // Update uniforms
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("color", primaryColor.red, primaryColor.green, primaryColor.blue, primaryColor.alpha)
                clip = true
            }
            .background(brush)
    )
}

@Composable
fun LiquidCanvasBackground(primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Liquid")
    
    // Animation 1
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "Offset1"
    )
    
    // Animation 2 (Slower, Reverse)
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing)),
        label = "Offset2"
    )

    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val w = size.width
        val h = size.height
        
        // Deep Background Mesh
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.8f
            )
        )

        // Moving Blob 1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                radius = w * 0.6f
            ),
            radius = w * 0.5f,
            center = Offset(
                x = w * 0.3f + sin(offset1) * w * 0.2f,
                y = h * 0.3f + cos(offset1) * h * 0.2f
            )
        )

        // Moving Blob 2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ColorCinema.copy(alpha = 0.3f), Color.Transparent),
                radius = w * 0.5f
            ),
            radius = w * 0.4f,
            center = Offset(
                x = w * 0.7f - sin(offset2) * w * 0.2f,
                y = h * 0.7f - cos(offset2) * h * 0.2f
            )
        )
        
        // Moving Blob 3 (Accent)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ColorStandard.copy(alpha = 0.2f), Color.Transparent),
                radius = w * 0.4f
            ),
            radius = w * 0.3f,
            center = Offset(
                x = w * 0.5f + sin(offset1 + 2f) * w * 0.3f,
                y = h * 0.8f + sin(offset2) * h * 0.1f
            )
        )
    }
    
    // Overlay Blur to fuse blobs
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(60.dp) 
    )
}
