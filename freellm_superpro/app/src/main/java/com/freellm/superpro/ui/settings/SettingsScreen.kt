package com.freellm.superpro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.data.preferences.AppPreferences
import com.freellm.superpro.domain.model.ModelOption
import com.freellm.superpro.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    var apiUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("auto") }
    var systemPrompt by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf(0.7f) }
    var amoledTheme by remember { mutableStateOf(true) }

    var isSavedSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apiUrl = preferences.apiUrl.first()
        apiKey = preferences.apiKey.first()
        selectedModel = preferences.modelId.first()
        systemPrompt = preferences.systemPrompt.first()
        temperature = preferences.temperature.first()
        amoledTheme = preferences.amoledTheme.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Endpoints", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        apiUrl = AppPreferences.DEFAULT_API_URL
                        apiKey = AppPreferences.DEFAULT_API_KEY
                        selectedModel = AppPreferences.DEFAULT_MODEL
                        systemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT
                        temperature = 0.7f
                    }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset Defaults")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimaryDark,
                    navigationIconContentColor = TextPrimaryDark,
                    actionIconContentColor = PrimaryNeonCyan
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Base URL Field
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("API Base URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryNeonCyan,
                    unfocusedBorderColor = DividerDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                )
            )

            // API Key Field
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Master API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryNeonCyan,
                    unfocusedBorderColor = DividerDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                )
            )

            // Model Selection
            Text(
                text = "Default Model",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryDark
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ModelOption.DEFAULT_PRESETS.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selectedModel == preset.id) SurfaceVariantDark else SurfaceDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedModel == preset.id,
                            onClick = { selectedModel = preset.id },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryNeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = preset.displayName, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 13.sp)
                            Text(text = preset.description, color = TextSecondaryDark, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Temperature Slider
            Text(
                text = "Temperature: ${String.format("%.2f", temperature)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryDark
            )
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0.0f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryNeonCyan,
                    activeTrackColor = PrimaryNeonCyan,
                    inactiveTrackColor = DividerDark
                )
            )

            // System Prompt Field
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Persona & Instructions") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryNeonCyan,
                    unfocusedBorderColor = DividerDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Settings Button
            Button(
                onClick = {
                    scope.launch {
                        preferences.setApiUrl(apiUrl)
                        preferences.setApiKey(apiKey)
                        preferences.setModelId(selectedModel)
                        preferences.setSystemPrompt(systemPrompt)
                        preferences.setTemperature(temperature)
                        preferences.setAmoledTheme(amoledTheme)
                        isSavedSnackbar = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save", tint = BackgroundDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }

            if (isSavedSnackbar) {
                Text(
                    text = "✓ Configuration saved successfully!",
                    color = AccentEmerald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
