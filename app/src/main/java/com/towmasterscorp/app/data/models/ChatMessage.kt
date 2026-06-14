package com.towmasterscorp.app.data.models

import com.google.gson.annotations.SerializedName

data class ChatConversation(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_name") val userName: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("last_message") val lastMessage: String? = null,
    @SerializedName("last_message_at") val lastMessageAt: String? = null,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("is_online") val isOnline: Boolean = false
)

data class ChatMessage(
    @SerializedName("id") val id: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("message") val message: String,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("sender_name") val senderName: String? = null
)

data class SendMessageRequest(
    @SerializedName("to") val to: Int,
    @SerializedName("message") val message: String
)
