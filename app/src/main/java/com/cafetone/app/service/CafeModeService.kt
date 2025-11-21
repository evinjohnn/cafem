package com.cafetone.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cafetone.app.R

class CafeModeService : Service() {

    // Binder for Activity communication
    private val binder = LocalBinder()
    
    // DSP Engines
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    
    // State
    private var currentSessionId: Int = 0
    private var isEngineEnabled: Boolean = true
    private var soundstageStrength: Short = 0
    private var ambiencePreset: Short = 0

    companion object {
        const val CHANNEL_ID = "cafe_tone_engine"
        const val ACTION_UPDATE_SESSION = "com.cafetone.app.UPDATE_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
    }

    inner class LocalBinder : Binder() {
        fun getService(): CafeModeService = this@CafeModeService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        Log.d("CafeTone", "Audio Engine Started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_SESSION) {
            val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, 0)
            if (sessionId != 0 && sessionId != currentSessionId) {
                attachToSession(sessionId)
            }
        }
        return START_STICKY
    }

    private fun attachToSession(sessionId: Int) {
        try {
            // Release old effects
            releaseEffects()

            currentSessionId = sessionId
            Log.d("CafeTone", "Attaching to Session: $sessionId")

            // 1. Virtualizer (Soundstage)
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = isEngineEnabled
                if (strengthSupported) {
                    setStrength(soundstageStrength)
                }
            }

            // 2. PresetReverb (Ambience)
            presetReverb = PresetReverb(0, sessionId).apply {
                enabled = isEngineEnabled
                preset = ambiencePreset
            }

        } catch (e: Exception) {
            Log.e("CafeTone", "DSP Error: ${e.message}")
            // Fallback: Try global if specific fails (rarely works on modern Android but worth a try)
        }
    }

    // --- Public Control Methods ---

    fun setEngineEnabled(enabled: Boolean) {
        isEngineEnabled = enabled
        virtualizer?.enabled = enabled
        presetReverb?.enabled = enabled
    }

    fun setSoundstage(strength: Short) {
        soundstageStrength = strength
        try {
            virtualizer?.setStrength(strength)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun setAmbience(preset: Short) {
        ambiencePreset = preset
        try {
            presetReverb?.preset = preset
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun isSessionActive(): Boolean = currentSessionId != 0

    private fun releaseEffects() {
        virtualizer?.release()
        presetReverb?.release()
        virtualizer = null
        presetReverb = null
    }

    override fun onDestroy() {
        releaseEffects()
        super.onDestroy()
    }

    // --- Notification Boilerplate ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_desc))
            .setSmallIcon(R.drawable.ic_cafe)
            .setOngoing(true)
            .build()
    }
}

