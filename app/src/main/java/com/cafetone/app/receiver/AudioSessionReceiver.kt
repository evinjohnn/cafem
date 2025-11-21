package com.cafetone.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import com.cafetone.app.service.AudioEngineService

class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "Unknown"

        Log.d("CafeTone", "Broadcast Received: $action | Session: $sessionId | Pkg: $packageName")

        if (sessionId == AudioEffect.ERROR) return

        val serviceIntent = Intent(context, AudioEngineService::class.java).apply {
            this.action = action
            putExtra(AudioEngineService.EXTRA_SESSION_ID, sessionId)
            putExtra(AudioEngineService.EXTRA_APP_NAME, packageName)
        }

        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("CafeTone", "Failed to start service: ${e.message}")
        }
    }
}
