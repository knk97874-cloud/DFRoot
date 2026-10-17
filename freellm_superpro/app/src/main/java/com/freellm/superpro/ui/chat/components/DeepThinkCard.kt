package com.freellm.superpro.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.ui.theme.*

@Composable
fun DeepThinkCard(
    reasoningText: String,
    durationSeconds: Double,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Pulsing aura animation while actively thinking
    val infiniteTransition = rememberInfiniteTransition(label = "think_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "think_alpha"
    )

    val borderBrush = if (isThinking) {
        Brush.horizontalGradient(
            listOf(
                PrimaryNeonCyan.copy(alpha = alphaAnim),
                SecondaryNeonPurple.copy(alpha = alphaAnim)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(ThinkCardBorder, ThinkCardBorder)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderBrush, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = ThinkCardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Reasoning",
                    tint = if (isThinking) PrimaryNeonCyan else TextSecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                val headerText = if (isThinking) {
                    "Thinking (${String.format("%.1f", durationSeconds)}s)..."
                } else {
                    "Thought for ${String.format("%.1f", durationSeconds)}s"
                }

                Text(
                    text = headerText,
                    color = if (isThinking) PrimaryNeonCyan else TextSecondaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (reasoningText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(reasoningText))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Reasoning",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded || isThinking) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable Content
            AnimatedVisibility(visible = isExpanded || isThinking) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Divider(color = DividerDark, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reasoningText.ifEmpty { "Evaluating task steps and constraints..." },
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
