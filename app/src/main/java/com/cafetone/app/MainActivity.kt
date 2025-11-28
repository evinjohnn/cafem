package com.cafetone.app

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cafetone.app.service.AudioEngineService
import com.cafetone.app.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent { AuraApp() }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}

@Composable
fun AuraApp() {
    // State for Modes
    var activeMode by remember { mutableStateOf("Cinema") }
    var sliderValue by remember { mutableStateOf(0.5f) } // Default 50%
    
    // Entrance Animation State
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
        // Sync with Service on first composition
        AudioEngineService.setMode(AudioEngineService.MODE_CINEMA)
        AudioEngineService.updateIntensity(50)
    }

    // Dynamic Theme Transition with Spring
    val targetColor = when (activeMode) {
        "Cafe" -> ColorCafe
        "Cinema" -> ColorCinema
        else -> ColorStandard
    }
    val primaryColor by animateColorAsState(
        targetColor, 
        animationSpec = spring(stiffness = Spring.StiffnessLow), 
        label = "ColorAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. LIQUID BACKGROUND LAYER
        LiquidBackground(primaryColor)

        // 2. GLASS UI LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header (Staggered Entrance)
            AnimatedEntrance(visible = isVisible, delay = 0) {
                HeaderSection()
            }

            // Main Visualizer (The Orb)
            AnimatedEntrance(visible = isVisible, delay = 100) {
                VisualizerOrb(activeMode, primaryColor)
            }

            // Controls Section (Glass Card)
            AnimatedEntrance(visible = isVisible, delay = 200) {
                ControlsSection(
                    mode = activeMode,
                    color = primaryColor,
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        AudioEngineService.updateIntensity((it * 100).toInt())
                    }
                )
            }

            // Tab Bar
            AnimatedEntrance(visible = isVisible, delay = 300) {
                ModeTabBar(activeMode) { selectedMode ->
                    activeMode = selectedMode
                    val modeInt = when(selectedMode) {
                        "Cafe" -> AudioEngineService.MODE_CAFE
                        "Cinema" -> AudioEngineService.MODE_CINEMA
                        else -> AudioEngineService.MODE_STANDARD
                    }
                    AudioEngineService.setMode(modeInt)
                }
            }
        }
    }
}

// --- ANIMATION HELPERS ---

@Composable
fun AnimatedEntrance(
    visible: Boolean,
    delay: Int,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = delay),
        label = "Alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 50f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "Slide"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        content()
    }
}

fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "Bounce"
    )
    val view = LocalView.current

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    // Haptic: Light Impact (API 30+) or Clock Tick
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                         view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                         view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { 
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick() 
                }
            )
        }
}

// --- COMPOSABLES ---

@Composable
fun HeaderSection() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("AURA", fontSize = 42.sp, fontWeight = FontWeight.Thin, letterSpacing = 4.sp, color = ColorTextMain)
            Text("ACOUSTIC ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = ColorTextDim)
        }
        // Settings Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorGlass)
                .border(1.dp, ColorGlassBorder, CircleShape)
                .bounceClick { showTroubleshootingDialog(context) },
             contentAlignment = Alignment.Center
        ) {
             Text("?", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VisualizerOrb(mode: String, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)), // Slow continuous rotation
        label = "Rotation"
    )
    
    // Pulse Animation for the core
    val pulseTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer Glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .blur(50.dp)
        )
        
        // Rotating Ring 1
        Box(
            modifier = Modifier
                .size(220.dp)
                .rotate(rotation)
                .border(1.dp, Brush.sweepGradient(listOf(Color.Transparent, color.copy(alpha = 0.5f), Color.Transparent)), CircleShape)
        )
        
        // Rotating Ring 2 (Counter-rotate)
        Box(
            modifier = Modifier
                .size(190.dp)
                .rotate(-rotation * 1.5f)
                .border(1.dp, Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent)), CircleShape)
        )

        // The Core Glass Sphere
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Brush.radialGradient(
                    colors = listOf(ColorGlass, color.copy(alpha = 0.05f)),
                    center = Offset.Unspecified,
                    radius = 200f
                ))
                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                .blur(0.dp), // Sharp glass edge
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mode.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = ColorTextMain)
                Text(
                    when(mode) {
                        "Cafe" -> "20FT DISTANCE"
                        "Cinema" -> "FIXED 360°"
                        else -> "REFERENCE"
                    },
                    fontSize = 9.sp,
                    color = ColorTextDim,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ControlsSection(mode: String, color: Color, value: Float, onValueChange: (Float) -> Unit) {
    val view = LocalView.current
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center, 
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        when(mode) {
                            "Cafe" -> "SOURCE DISTANCE"
                            "Cinema" -> "IMMERSION"
                            else -> "OUTPUT GAIN"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextDim,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    "${(value * 100).toInt()}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Thin,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            // Custom Slider
            Slider(
                value = value,
                onValueChange = { 
                    onValueChange(it)
                    // Haptic Tick on slide
                    if ((it * 100).toInt() % 5 == 0) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = color,
                    inactiveTrackColor = Color(0x1AFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // DSP Tag
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    when(mode) {
                        "Cafe" -> "SONY BACKGROUND LISTENING • REAR STAGE"
                        "Cinema" -> "MULTI-BAND DYNAMICS • 360° AUDIO"
                        else -> "DSEE EXTREME • FLAT EQ"
                    },
                    fontSize = 9.sp,
                    color = Color(0x80FFFFFF),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ModeTabBar(activeMode: String, onModeSelected: (String) -> Unit) {
    val modes = listOf("Standard", "Cafe", "Cinema")
    val selectedIndex = modes.indexOf(activeMode).coerceAtLeast(0)
    
    LiquidGlassSelector(
        items = modes,
        activeMode = activeMode,
        onModeSelected = onModeSelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    )
}

fun showTroubleshootingDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle("Spotify Fix")
        .setMessage("To enable effects in Spotify:\n\n1. Open Spotify Settings.\n2. Scroll to 'Device Broadcast Status'.\n3. Turn it ON.\n\nThis allows Aura to detect the audio session.")
        .setPositiveButton("Open Spotify") { _, _ ->
            try {
                val intent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                if (intent != null) context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Spotify not found", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Close", null)
        .show()
}
