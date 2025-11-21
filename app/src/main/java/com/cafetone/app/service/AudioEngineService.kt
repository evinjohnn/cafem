package com.cafetone.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.cafetone.app.R

class AudioEngineService : LifecycleService() {

    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId = AudioEffect.ERROR

    companion object {
        const val CHANNEL_ID = "cafe_tone_channel"
        const val NOTIF_ID = 1337
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_APP_NAME = "extra_app_name"

        // Public state for UI
        var activeSessionId = 0
        var activeApp = "Idle"
        
        // Effect Parameters (0-1000)
        var targetBassStrength: Short = 0
        var targetVirtStrength: Short = 0
        
        private var instance: AudioEngineService? = null
        
        fun updateBass(strength: Short) {
            targetBassStrength = strength
            instance?.applyEffects()
        }
        
        fun updateVirtualizer(strength: Short) {
            targetVirtStrength = strength
            instance?.applyEffects()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIF_ID, createNotification("Waiting for Music..."))
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
                 updateNotification("Idle")
             }
        }
        
        return START_STICKY
    }

    private fun attachToSession(sessionId: Int, appName: String) {
        if (currentSessionId == sessionId) return
        
        releaseEffects()
        
        try {
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
            
            currentSessionId = sessionId
            activeSessionId = sessionId
            activeApp = appName
            
            applyEffects()
            updateNotification("Enhancing $appName")
            Log.d("CafeTone", "Attached to session $sessionId")
            
        } catch (e: Exception) {
            Log.e("CafeTone", "Failed to attach: ${e.message}")
            // Don't crash, just reset
            releaseEffects()
        }
    }
    
    private fun applyEffects() {
        try {
            bassBoost?.setStrength(targetBassStrength)
            virtualizer?.setStrength(targetVirtStrength)
        } catch (e: Exception) {
            Log.e("CafeTone", "Error setting strength: ${e.message}")
        }
    }

    private fun releaseEffects() {
        try {
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) { }
        
        bassBoost = null
        virtualizer = null
        currentSessionId = AudioEffect.ERROR
    }

    override fun onDestroy() {
        releaseEffects()
        instance = null
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CafeTone Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CafeTone V2")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher) // Fallback to launcher icon
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, createNotification(text))
    }
}
