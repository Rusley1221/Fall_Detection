package com.example.falldetection.detection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.falldetection.ui.screens.AlertScreen
import com.example.falldetection.ui.theme.FallDetectionTheme
import kotlinx.coroutines.launch

class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FallDetectionTheme {
                AlertScreen(onClose = { finish() })
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DetectionStateHolder.alert.collect { state ->
                    if (!state.active) finish()
                }
            }
        }
    }
}
