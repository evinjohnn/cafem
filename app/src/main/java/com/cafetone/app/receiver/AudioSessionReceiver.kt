package com.cafetone.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import androidx.core.content.ContextCompat
import com.cafetone.app.service.CafeModeService

class AudioSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
        val pkgName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "Unknown"

        Log.d("CafeTone", "Broadcast Received: $action | ID: $sessionId | Pkg: $pkgName")

        if (sessionId == 0) return

        if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
            // Pass the session ID to our Foreground Service
            val serviceIntent = Intent(context, CafeModeService::class.java).apply {
                this.action = CafeModeService.ACTION_UPDATE_SESSION
                this.putExtra(CafeModeService.EXTRA_SESSION_ID, sessionId)
            }
            
            // Ensure service is running
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}

