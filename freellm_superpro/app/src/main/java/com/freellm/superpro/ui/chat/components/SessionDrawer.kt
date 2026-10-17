package com.freellm.superpro.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freellm.superpro.data.local.entity.ConversationEntity
import com.freellm.superpro.ui.theme.*

@Composable
fun SessionDrawer(
    conversations: List<ConversationEntity>,
    activeConversationId: String?,
    onSelectConversation: (String) -> Unit,
    onNewChatClicked: () -> Unit,
    onPinToggle: (String, Boolean) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        // App Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Logo",
                tint = PrimaryNeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FreeLLM SuperPro",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // New Chat Button
        Button(
            onClick = onNewChatClicked,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Chat",
                tint = PrimaryNeonCyan
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Conversation",
                color = TextPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats...", color = TextSecondaryDark, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryNeonCyan,
                unfocusedBorderColor = DividerDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Chat History",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondaryDark,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        // Conversation List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredConversations, key = { it.id }) { conv ->
                val isActive = conv.id == activeConversationId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) SurfaceVariantDark else SurfaceDark)
                        .clickable { onSelectConversation(conv.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (conv.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
                        contentDescription = "Chat",
                        tint = if (conv.isPinned) PrimaryNeonCyan else TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = conv.title.ifEmpty { "New Conversation" },
                        color = if (isActive) PrimaryNeonCyan else TextPrimaryDark,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Options: Pin / Delete
                    IconButton(
                        onClick = { onPinToggle(conv.id, !conv.isPinned) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (conv.isPinned) PrimaryNeonCyan else TextSecondaryDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteConversation(conv.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextSecondaryDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Divider(color = DividerDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Settings Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onOpenSettings() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = PrimaryNeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Settings & API Keys",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
        }
    }
}
