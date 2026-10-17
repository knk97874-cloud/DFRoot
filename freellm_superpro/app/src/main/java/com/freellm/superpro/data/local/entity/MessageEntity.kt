package com.freellm.superpro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String,
    val content: String,
    val reasoningContent: String,
    val reasoningDurationSeconds: Double,
    val searchSourcesJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
