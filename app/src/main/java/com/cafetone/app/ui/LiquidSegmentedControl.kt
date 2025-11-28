package com.cafetone.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiquidSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var itemWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // Spring Animation for Fluid Movement
    val animatedOffset by animateFloatAsState(
        targetValue = selectedIndex * itemWidth,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = 0.7f // Slightly less bouncy for a "heavier" glass feel
        ),
        label = "GlassOffset"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x10FFFFFF)) // Reverted to light transparent background
            .onGloballyPositioned {
                if (items.isNotEmpty()) {
                    itemWidth = it.size.width / items.size.toFloat()
                }
            }
    ) {
        // 1. THE GLASS PILL INDICATOR (CSS Reconstruction)
        // Style: .glass-surface--fallback (Dark Mode)
        // Background: rgba(255, 255, 255, 0.1)
        // Border: 1px solid rgba(255, 255, 255, 0.2)
        // Shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.2)
        
        val blobWidth = with(density) { itemWidth.toDp() }
        
        Box(
            modifier = Modifier
                .width(blobWidth)
                .fillMaxHeight()
                .offset(x = with(density) { animatedOffset.toDp() })
                .padding(4.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    spotColor = Color(0x331F2687), // Blue-ish shadow from CSS
                    ambientColor = Color(0x111F2687)
                )
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.1f)) // Glass Body
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                )
        ) {
            // Inner Highlights (Simulating inset shadows)
            // Top Highlight: inset 0 1px 0 0 rgba(255, 255, 255, 0.4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.4f))
                    .align(Alignment.TopCenter)
            )
            
            // Bottom Highlight: inset 0 -1px 0 0 rgba(255, 255, 255, 0.2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.BottomCenter)
            )
        }

        // 2. TEXT LAYER
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, text ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    if (isSelected) Color.Black else Color.White, // Reverted to White
                    label = "TextColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null 
                        ) { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text.uppercase(),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
