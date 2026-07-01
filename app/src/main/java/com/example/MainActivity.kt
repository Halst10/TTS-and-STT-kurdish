package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VoiceViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: VoiceViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "مۆڵەتی مایکرۆفۆن درا! ئێستا دەتوانیت قسە بکەیت.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "بۆ گۆڕینی دەنگ و کۆپیکردن، مۆڵەتی مایکرۆفۆن پێویستە.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge full bleed
        enableEdgeToEdge()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[VoiceViewModel::class.java]

        // Check and request runtime audio permission
        checkAudioPermission()

        setContent {
            MyApplicationTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

