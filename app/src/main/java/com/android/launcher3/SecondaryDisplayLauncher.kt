package com.android.launcher3

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.android.launcher3.ui.HomeScreen
import com.android.launcher3.ui.theme.QuarkLauncherTheme
import com.android.launcher3.viewmodel.LauncherViewModel

class SecondaryDisplayLauncher : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuarkLauncherTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Reset app drawer to closed if home intent is sent again
        if (Intent.ACTION_MAIN == intent.action) {
            viewModel.setDrawerOpen(false)
        }
    }
}
