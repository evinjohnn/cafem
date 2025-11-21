package com.cafetone.app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cafetone.app.databinding.ActivityMainBinding
import com.cafetone.app.service.AudioEngineService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    
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
        // Initialize sliders with current values (in case activity recreated)
        binding.seekBarBass.progress = AudioEngineService.targetBassStrength.toInt()
        binding.seekBarVirt.progress = AudioEngineService.targetVirtStrength.toInt()

        binding.seekBarBass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                AudioEngineService.updateBass(progress.toShort())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarVirt.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                AudioEngineService.updateVirtualizer(progress.toShort())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateStatus() {
        val sessionId = AudioEngineService.activeSessionId
        val appName = AudioEngineService.activeApp
        
        if (sessionId != 0) {
            binding.tvStatus.text = "STATUS: ACTIVE ($appName)"
            binding.tvStatus.setTextColor(Color.parseColor("#00E5FF")) // Neon Cyan
            binding.tvSessionId.text = "SESSION: $sessionId"
        } else {
            binding.tvStatus.text = "STATUS: IDLE"
            binding.tvStatus.setTextColor(Color.parseColor("#FF2A6D")) // Neon Pink
            binding.tvSessionId.text = "SESSION: --"
        }
    }

    private fun setupTroubleshooting() {
        binding.btnTroubleshoot.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Spotify Fix")
                .setMessage("To enable effects in Spotify:\n\n1. Open Spotify Settings.\n2. Scroll to 'Device Broadcast Status'.\n3. Turn it ON.\n\nThis allows CafeTone to detect the audio session.")
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
