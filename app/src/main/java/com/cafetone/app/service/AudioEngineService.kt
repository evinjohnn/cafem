package com.cafetone.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.cafetone.app.R

class AudioEngineService : LifecycleService() {

    private var envReverb: EnvironmentalReverb? = null
    private var virtualizer: Virtualizer? = null
    private var dynamicsProcessing: AudioEffect? = null
    private var equalizer: Equalizer? = null
    
    private var currentSessionId = AudioEffect.ERROR

    companion object {
        const val CHANNEL_ID = "aura_service_channel"
        const val NOTIF_ID = 1337
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_APP_NAME = "extra_app_name"

        var activeSessionId = 0
        var activeApp = "Idle"
        
        // Modes
        const val MODE_STANDARD = 0
        const val MODE_CAFE = 1
        const val MODE_CINEMA = 2
        
        // Parameters
        var currentMode = MODE_STANDARD
        var intensityPct = 50 // Used for Distance (Cafe) or Immersion (Cinema)
        
        private var instance: AudioEngineService? = null
        
        fun setMode(mode: Int) {
            currentMode = mode
            instance?.applyDsp()
        }
        
        fun updateIntensity(pct: Int) {
            intensityPct = pct
            instance?.applyDsp()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIF_ID, createNotification("AURA: Waiting for Music..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val action = intent?.action
        val sessionId = intent?.getIntExtra(EXTRA_SESSION_ID, AudioEffect.ERROR) ?: AudioEffect.ERROR
        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "Unknown"

        if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
            if (sessionId != AudioEffect.ERROR) {
                attachToSession(sessionId, appName)
            }
        } else if (action == AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) {
             if (sessionId == currentSessionId || sessionId == AudioEffect.ERROR) {
                 releaseEffects()
                 activeApp = "Idle"
                 activeSessionId = 0
                 updateNotification("AURA: Idle")
             }
        }
        return START_STICKY
    }

    private fun attachToSession(sessionId: Int, appName: String) {
        if (currentSessionId == sessionId) return
        releaseEffects()
        
        try {
            // 1. Environmental Reverb
            envReverb = EnvironmentalReverb(0, sessionId).apply { enabled = true }

            // 2. Virtualizer
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
            
            // 3. EQ / Dynamics (Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 10 Band Graphic EQ Config
                val builder = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, // Stereo
                    true, 10, // PreEQ: 10 bands
                    true, 4,  // MBC
                    true, 4,  // PostEQ
                    true      // Limiter
                )
                dynamicsProcessing = DynamicsProcessing(0, sessionId, builder.build()).apply { enabled = true }
            } else {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
            }

            currentSessionId = sessionId
            activeSessionId = sessionId
            activeApp = appName
            
            applyDsp()
            updateNotification("AURA: Enhancing $appName")
            Log.d("AURA", "Attached to session $sessionId")
            
        } catch (e: Exception) {
            Log.e("AURA", "Failed to attach: ${e.message}")
            releaseEffects()
        }
    }
    
    private fun applyDsp() {
        val reverb = envReverb ?: return
        val virt = virtualizer ?: return
        
        try {
            when (currentMode) {
                MODE_STANDARD -> {
                    reverb.roomLevel = -9000 // Dry
                    virt.strength = 0
                    resetEq()
                }
                MODE_CAFE -> applyCafeMode(reverb, virt)
                MODE_CINEMA -> applyCinemaMode(reverb, virt)
            }
        } catch (e: Exception) {
            Log.e("AURA", "DSP Error: ${e.message}")
        }
    }
    
    private fun applyCafeMode(reverb: EnvironmentalReverb, virt: Virtualizer) {
        // CAFE MODE (Sony XM6 Cafe Simulator)
        // Distance 0-100%
        val distFactor = intensityPct / 100f
        
        // Reverb: -25dB (Close) to -7dB (Far)
        val targetRoomLevel = -2500 + (1800 * distFactor).toInt()
        
        reverb.decayTime = 600
        reverb.roomLevel = targetRoomLevel.toShort()
        reverb.roomHFLevel = -1200
        reverb.reverbDelay = 20
        reverb.diffusion = 1000
        reverb.density = 1000
        
        virt.strength = 0

        val bands = listOf(
            EqBand(31f, -15f),
            EqBand(62f, -15f),
            EqBand(125f, -8f),
            EqBand(250f, -2f),
            EqBand(500f, 4f),
            EqBand(1000f, 0f),
            EqBand(2000f, -3f),
            EqBand(4000f, -6f),
            EqBand(8000f, -10f - (10f * distFactor)),
            EqBand(16000f, -15f - (20f * distFactor))
        )
        
        apply10BandEq(bands)
    }

    private fun applyCinemaMode(reverb: EnvironmentalReverb, virt: Virtualizer) {
        // CINEMA MODE (Sony 360RA Inspiration)
        // Goal: 360 Audio Upmix, Fixed Orientation, X-Curve
        
        // Immersion Slider 0-100% controls Virtualizer Strength & Reverb Mix
        val immersion = intensityPct / 100f
        
        // 1. Virtualizer (The 360 Effect)
        // Sony's 360RA relies heavily on widening.
        // We map 0% -> 0, 100% -> 1000 (Max Android Width)
        virt.strength = (1000 * immersion).toInt().toShort()
        
        // 2. Reverb (The Theater Room)
        // Decay: 1.2s (Large but treated)
        // Room Level: -20dB (-2000mB) constant base, slightly increasing with immersion.
        // Reflections: -15dB (Simulate side walls)
        reverb.decayTime = 1200
        reverb.roomLevel = (-2000 + (500 * immersion)).toInt().toShort() // -20dB to -15dB
        reverb.roomHFLevel = -500 // Slight damping, cinema screens are perforated
        reverb.reverbDelay = 40 // 40ms pre-delay to simulate distance to screen
        reverb.diffusion = 1000 // Max diffusion (complex reflections)
        reverb.density = 1000
        
        // 3. EQ (The X-Curve & LFE)
        // Boost Sub-bass (LFE), Boost Presence (Dialogue), Roll-off Highs
        val bands = listOf(
            EqBand(31f, 6f),  // LFE Rumble
            EqBand(62f, 6f),  // Sub Bass
            EqBand(125f, 3f), // Body
            EqBand(250f, 0f),
            EqBand(500f, 0f),
            EqBand(1000f, 0f),
            EqBand(2000f, 3f), // Center Channel / Dialogue
            EqBand(4000f, 0f),
            EqBand(8000f, -3f), // X-Curve Roll-off start
            EqBand(16000f, -6f) // Air reduction
        )
        
        apply10BandEq(bands)
    }
    
    data class EqBand(val freq: Float, val gain: Float)

    private fun apply10BandEq(bands: List<EqBand>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dp = dynamicsProcessing as? DynamicsProcessing ?: return
            val config = dp.config
            
            for (i in bands.indices) {
                if (i >= config.getPreEqBandCount(0)) break
                
                val bandSpec = bands[i]
                val band = config.getPreEqBandByChannelIndex(0, i)
                band.cutoffFrequency = bandSpec.freq
                band.gain = bandSpec.gain
                band.qualityFactor = 1.4f
                
                dp.setPreEqBand(0, i, band)
                dp.setPreEqBand(1, i, band)
            }
            dp.setPreEqAllChannelsTo(config.getPreEqByChannelIndex(0))
        } else {
            // Legacy Fallback
            val eq = equalizer ?: return
            // Simple bass boost for legacy cinema
            if (currentMode == MODE_CINEMA) {
                eq.setBandLevel(0, 600) // +6dB
                eq.setBandLevel(1, 400) // +4dB
            } else {
                for (i in 0 until eq.numberOfBands) {
                    eq.setBandLevel(i.toShort(), 0)
                }
            }
        }
    }
    
    private fun resetEq() {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dp = dynamicsProcessing as? DynamicsProcessing ?: return
            val config = dp.config
            for (i in 0 until config.getPreEqBandCount(0)) {
                val band = config.getPreEqBandByChannelIndex(0, i)
                band.gain = 0f
                dp.setPreEqBand(0, i, band)
                dp.setPreEqBand(1, i, band)
            }
         } else {
             val eq = equalizer ?: return
             for (i in 0 until eq.numberOfBands) {
                 eq.setBandLevel(i.toShort(), 0)
             }
         }
    }

    private fun releaseEffects() {
        try {
            envReverb?.release()
            virtualizer?.release()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (dynamicsProcessing as? DynamicsProcessing)?.release()
            }
            equalizer?.release()
        } catch (e: Exception) { }
        
        envReverb = null
        virtualizer = null
        dynamicsProcessing = null
        equalizer = null
        currentSessionId = AudioEffect.ERROR
    }

    override fun onDestroy() {
        releaseEffects()
        instance = null
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "AURA Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AURA")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, createNotification(text))
    }
}
