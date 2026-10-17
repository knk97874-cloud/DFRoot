package com.freellm.superpro.ui.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.domain.model.ModelOption
import com.freellm.superpro.ui.theme.*

@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClicked: () -> Unit,
    deepThinkEnabled: Boolean,
    onDeepThinkToggle: () -> Unit,
    webSearchEnabled: Boolean,
    onWebSearchToggle: () -> Unit,
    currentModel: String,
    onModelSelect: (String) -> Unit,
    onAttachFileClicked: () -> Unit,
    onVoiceInputClicked: () -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    var showModelMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Quick Mode Pill Toggles Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DeepThink Pill
            val thinkColor by animateColorAsState(
                if (deepThinkEnabled) PrimaryNeonCyan else TextSecondaryDark,
                label = "think_pill_anim"
            )
            ModePill(
                icon = Icons.Default.Psychology,
                label = if (deepThinkEnabled) "DeepThink: ON" else "DeepThink: OFF",
                isActive = deepThinkEnabled,
                activeColor = PrimaryNeonCyan,
                onClick = onDeepThinkToggle
            )

            // Web Search Pill
            val searchColor by animateColorAsState(
                if (webSearchEnabled) AccentEmerald else TextSecondaryDark,
                label = "search_pill_anim"
            )
            ModePill(
                icon = Icons.Default.Public,
                label = if (webSearchEnabled) "Web: ON" else "Web: OFF",
                isActive = webSearchEnabled,
                activeColor = AccentEmerald,
                onClick = onWebSearchToggle
            )

            // Model Switcher Pill
            Box {
                ModePill(
                    icon = Icons.Default.Bolt,
                    label = currentModel.uppercase(),
                    isActive = true,
                    activeColor = SecondaryNeonPurple,
                    onClick = { showModelMenu = true }
                )

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    ModelOption.DEFAULT_PRESETS.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = preset.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = preset.description,
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                onModelSelect(preset.id)
                                showModelMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Main Input Surface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, DividerDark, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment Button
            IconButton(
                onClick = onAttachFileClicked,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach File",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Ask anything, search web, or analyze...",
                        color = TextSecondaryDark.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(
                        color = TextPrimaryDark,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(PrimaryNeonCyan),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Voice STT Button
            IconButton(
                onClick = onVoiceInputClicked,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Send Button
            val canSend = inputText.isNotBlank() && !isStreaming
            IconButton(
                onClick = onSendClicked,
                enabled = canSend,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (canSend) PrimaryNeonCyan else DividerDark)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Send",
                    tint = if (canSend) BackgroundDark else TextSecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ModePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.15f) else SurfaceDark)
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.5f) else DividerDark,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else TextSecondaryDark,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) activeColor else TextSecondaryDark
        )
    }
}
