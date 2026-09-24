package com.android.launcher3

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.android.launcher3.ui.HomeScreen
import com.android.launcher3.ui.theme.QuarkLauncherTheme
import com.android.launcher3.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

class SecondaryDisplayLauncher : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var platformBackCallback: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupPredictiveBack()

        setContent {
            QuarkLauncherTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    private fun setupPredictiveBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val animationCallback = object : OnBackAnimationCallback {
                override fun onBackStarted(backEvent: BackEvent) {
                    viewModel.updateBackProgress(backEvent.progress)
                }

                override fun onBackProgressed(backEvent: BackEvent) {
                    viewModel.updateBackProgress(backEvent.progress)
                }

                override fun onBackInvoked() {
                    viewModel.setDrawerOpen(false)
                }

                override fun onBackCancelled() {
                    viewModel.updateBackProgress(0f)
                }
            }
            platformBackCallback = animationCallback

            lifecycleScope.launch {
                viewModel.isDrawerOpen.collect { isOpen ->
                    if (isOpen) {
                        try {
                            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                                animationCallback
                            )
                        } catch (ignored: Exception) {
                        }
                    } else {
                        try {
                            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(animationCallback)
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val callback = OnBackInvokedCallback {
                if (viewModel.isDrawerOpen.value) {
                    viewModel.setDrawerOpen(false)
                }
            }
            platformBackCallback = callback

            lifecycleScope.launch {
                viewModel.isDrawerOpen.collect { isOpen ->
                    if (isOpen) {
                        try {
                            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                                callback
                            )
                        } catch (ignored: Exception) {
                        }
                    } else {
                        try {
                            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action) {
            viewModel.setDrawerOpen(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && platformBackCallback is OnBackInvokedCallback) {
            try {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(platformBackCallback as OnBackInvokedCallback)
            } catch (ignored: Exception) {
            }
        }
    }
}
