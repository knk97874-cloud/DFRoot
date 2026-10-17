package com.freellm.superpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.freellm.superpro.ui.chat.ChatScreen
import com.freellm.superpro.ui.chat.ChatViewModel
import com.freellm.superpro.ui.settings.SettingsScreen
import com.freellm.superpro.ui.theme.FreeLLMSuperProTheme

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreeLLMSuperProTheme {
                var currentScreen by remember { mutableStateOf("chat") }

                when (currentScreen) {
                    "chat" -> {
                        ChatScreen(
                            viewModel = chatViewModel,
                            onNavigateToSettings = { currentScreen = "settings" }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            onNavigateBack = { currentScreen = "chat" }
                        )
                    }
                }
            }
        }
    }
}
