package com.cafetone.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cafetone.app.databinding.ActivityMainBinding
import com.cafetone.app.service.CafeModeService
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cafeService: CafeModeService? = null
    private var isBound = false
    private var isPowerOn = true

    // Status poller to update UI when audio attaches
    private val statusTimer = Timer()

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CafeModeService.LocalBinder
            cafeService = binder.getService()
            isBound = true
            updateUiState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cafeService = null
            isBound = false
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) startAudioService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        setupControls()
        startStatusPoller()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startAudioService()
            }
        } else {
            startAudioService()
        }
    }

    private fun startAudioService() {
        val intent = Intent(this, CafeModeService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupControls() {
        // Power Button
        binding.btnPower.setOnClickListener {
            isPowerOn = !isPowerOn
            cafeService?.setEngineEnabled(isPowerOn)
            
            // UI Update
            val color = if (isPowerOn) getColor(R.color.sony_gold) else getColor(R.color.text_secondary)
            binding.btnPower.imageTintList = ColorStateList.valueOf(color)
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }

        // Soundstage Slider (Virtualizer)
        binding.sliderSoundstage.addOnChangeListener { _, value, fromUser ->
            if (fromUser && isBound) {
                cafeService?.setSoundstage(value.toInt().toShort())
                triggerHaptic()
            }
        }

        // Ambience Slider (Reverb Presets 0-6)
        binding.sliderAmbience.addOnChangeListener { _, value, fromUser ->
            if (fromUser && isBound) {
                cafeService?.setAmbience(value.toInt().toShort())
                triggerHaptic()
            }
        }
    }

    private fun triggerHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        }
    }
    
    private fun startStatusPoller() {
        statusTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (isBound && cafeService != null) {
                        if (cafeService!!.isSessionActive()) {
                            binding.textStatus.text = getString(R.string.status_active)
                            binding.statusIndicator.backgroundTintList = 
                                ColorStateList.valueOf(getColor(R.color.active_green))
                            // Pulse Animation logic could go here
                        } else {
                            binding.textStatus.text = getString(R.string.status_waiting)
                            binding.statusIndicator.backgroundTintList = 
                                ColorStateList.valueOf(getColor(R.color.surface_light_grey))
                        }
                    }
                }
            }
        }, 1000, 2000)
    }

    private fun updateUiState() {
        // Sync UI with Service state if needed
        binding.sliderSoundstage.value = 0f // Default
        binding.sliderAmbience.value = 0f // Default
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        statusTimer.cancel()
    }
}

