package com.freellm.superpro.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.domain.model.Message
import com.freellm.superpro.domain.model.Role
import com.freellm.superpro.domain.model.SearchSource
import com.freellm.superpro.ui.theme.*

@Composable
fun MessageBubble(
    message: Message,
    onSourceClicked: (SearchSource) -> Unit,
    onSpeakClicked: (String) -> Unit,
    onOpenCanvas: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == Role.USER
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Assistant Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PrimaryNeonCyan)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "AI",
                    tint = BackgroundDark,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // 1. Web Sources Carousel (if sources present)
            if (message.searchSources.isNotEmpty()) {
                WebSourcesCarousel(
                    sources = message.searchSources,
                    onSourceClicked = onSourceClicked,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 2. DeepThink Card (if reasoning present)
            if (message.reasoningContent.isNotEmpty() || message.isReasoningActive) {
                DeepThinkCard(
                    reasoningText = message.reasoningContent,
                    durationSeconds = message.reasoningDurationSeconds,
                    isThinking = message.isReasoningActive,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 3. Message Body Bubble
            if (message.content.isNotEmpty() || message.isStreaming) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            )
                        )
                        .background(if (isUser) UserBubbleColor else AssistantBubbleColor)
                        .border(0.5.dp, if (isUser) PrimaryNeonCyan.copy(alpha = 0.3f) else DividerDark, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.content.ifEmpty { "..." },
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }

                // 4. Action bar for Assistant (Copy / TTS / Canvas Preview)
                if (!isUser && message.content.isNotEmpty() && !message.isStreaming) {
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = { onSpeakClicked(message.content) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak TTS",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Check if HTML or SVG is detected to offer Canvas mode
                        if (message.content.contains("<html", ignoreCase = true) || message.content.contains("<svg", ignoreCase = true)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryNeonCyan.copy(alpha = 0.15f))
                                    .clickable { onOpenCanvas(message.content) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Canvas",
                                    tint = PrimaryNeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Open Canvas",
                                    fontSize = 11.sp,
                                    color = PrimaryNeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
