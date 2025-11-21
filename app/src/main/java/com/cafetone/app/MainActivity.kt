package com.cafetone.app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cafetone.app.databinding.ActivityMainBinding
import com.cafetone.app.service.AudioEngineService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    
    private val colorActive = Color.parseColor("#00E5FF") // Cyan
    private val colorInactive = Color.parseColor("#1A1A1A")
    private val textActive = Color.parseColor("#000000")
    private val textInactive = Color.parseColor("#888888")
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        setupControls()
        setupEnvironmentSelector()
        setupTroubleshooting()
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupControls() {
        // Distance / Immersion Slider
        binding.seekBarVirt.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                AudioEngineService.updateIntensity(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupEnvironmentSelector() {
        updateEnvButtons(AudioEngineService.currentMode)

        binding.btnEnvNone.setOnClickListener { 
            setMode(AudioEngineService.MODE_STANDARD)
            updateUiForMode(AudioEngineService.MODE_STANDARD)
        }
        binding.btnEnvCafe.setOnClickListener { 
            setMode(AudioEngineService.MODE_CAFE)
            updateUiForMode(AudioEngineService.MODE_CAFE)
        }
        // Renaming button references in layout would be better, but reusing for now:
        // btnEnvConcert -> Theater (not in current AURA spec but keeping for button existence)
        // btnEnvCathedral -> Cinema
        
        binding.btnEnvCathedral.setOnClickListener { 
            setMode(AudioEngineService.MODE_CINEMA)
            updateUiForMode(AudioEngineService.MODE_CINEMA)
        }
        
         // Map Concert button to Standard or ignore for now based on AURA spec having only 3 modes
         binding.btnEnvConcert.setOnClickListener {
             // Optional: Could be another mode or just link to Standard
             setMode(AudioEngineService.MODE_STANDARD)
             updateUiForMode(AudioEngineService.MODE_STANDARD)
         }
    }
    
    private fun setMode(mode: Int) {
        AudioEngineService.setMode(mode)
        updateEnvButtons(mode)
    }
    
    private fun updateUiForMode(mode: Int) {
        binding.seekBarVirt.progress = AudioEngineService.intensityPct
        
        when (mode) {
            AudioEngineService.MODE_CAFE -> {
                binding.tvVirtLabel.text = "SOURCE DISTANCE (NEAR -> FAR)"
            }
            AudioEngineService.MODE_CINEMA -> {
                binding.tvVirtLabel.text = "IMMERSION (0% -> 360°)"
            }
            else -> {
                binding.tvVirtLabel.text = "INTENSITY"
            }
        }
    }
    
    private fun updateEnvButtons(selectedMode: Int) {
        resetButton(binding.btnEnvNone)
        resetButton(binding.btnEnvCafe)
        resetButton(binding.btnEnvConcert)
        resetButton(binding.btnEnvCathedral)

        when (selectedMode) {
            AudioEngineService.MODE_STANDARD -> highlightButton(binding.btnEnvNone)
            AudioEngineService.MODE_CAFE -> highlightButton(binding.btnEnvCafe)
            AudioEngineService.MODE_CINEMA -> highlightButton(binding.btnEnvCathedral)
        }
    }
    
    private fun highlightButton(btn: Button) {
        btn.backgroundTintList = ColorStateList.valueOf(colorActive)
        btn.setTextColor(textActive)
    }
    
    private fun resetButton(btn: Button) {
        btn.backgroundTintList = ColorStateList.valueOf(colorInactive)
        btn.setTextColor(textInactive)
    }

    private fun updateStatus() {
        val sessionId = AudioEngineService.activeSessionId
        val appName = AudioEngineService.activeApp
        
        if (sessionId != 0) {
            binding.tvStatus.text = "STATUS: ACTIVE ($appName)"
            binding.tvStatus.setTextColor(Color.parseColor("#00E5FF"))
            binding.tvSessionId.text = "SESSION: $sessionId"
        } else {
            binding.tvStatus.text = "STATUS: IDLE"
            binding.tvStatus.setTextColor(Color.parseColor("#FF2A6D"))
            binding.tvSessionId.text = "SESSION: --"
        }
    }

    private fun setupTroubleshooting() {
        binding.btnTroubleshoot.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Spotify Fix")
                .setMessage("Enable 'Device Broadcast Status' in Spotify Settings.")
                .setPositiveButton("Open Spotify") { _, _ ->
                    try {
                        val intent = packageManager.getLaunchIntentForPackage("com.spotify.music")
                        if (intent != null) startActivity(intent)
                    } catch (e: Exception) { }
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}
