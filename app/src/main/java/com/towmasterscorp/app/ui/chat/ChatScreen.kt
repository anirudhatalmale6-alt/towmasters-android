package com.towmasterscorp.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.ChatMessage
import com.towmasterscorp.app.data.models.SendMessageRequest
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.ui.theme.*
import kotlinx.coroutines.launch


@Composable
fun ChatScreen(
    currentUser: User,
    otherUserId: Int,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var otherUserName by remember { mutableStateOf("Chat") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun loadMessages() {
        scope.launch {
            isLoading = true
            try {
                val response = ApiClient.getApi().getMessages(otherUserId)
                if (response.isSuccessful && response.body()?.success == true) {
                    messages = response.body()?.messages ?: emptyList()
                    if (messages.isNotEmpty()) {
                        val otherMsg = messages.firstOrNull { it.senderId == otherUserId }
                        if (otherMsg?.senderName != null) {
                            otherUserName = otherMsg.senderName
                        }
                    }
                    // Scroll to bottom
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            } catch (e: Exception) {
                // Handle silently
            }
            isLoading = false
        }
    }

    fun sendMessage() {
        if (messageText.isBlank()) return
        val text = messageText.trim()
        messageText = ""

        scope.launch {
            isSending = true
            try {
                val response = ApiClient.getApi().sendMessage(
                    SendMessageRequest(recipientId = otherUserId, message = text)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    messages = messages + ChatMessage(
                        id = (messages.lastOrNull()?.id ?: 0) + 1,
                        senderId = currentUser.id,
                        receiverId = otherUserId,
                        message = text,
                        senderName = currentUser.fullName
                    )
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            } catch (e: Exception) {
                // Re-add message text if failed
                messageText = text
            }
            isSending = false
        }
    }

    LaunchedEffect(otherUserId) {
        loadMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUserName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadMessages() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = messageText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (messageText.isNotBlank()) Secondary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromCurrentUser = message.senderId == currentUser.id
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isFromCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
            ),
            color = if (isFromCurrentUser) Primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isFromCurrentUser && message.senderName != null) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFromCurrentUser) OnPrimary.copy(alpha = 0.7f)
                        else Secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.message,
                    fontSize = 14.sp,
                    color = if (isFromCurrentUser) OnPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (message.createdAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.createdAt,
                        fontSize = 10.sp,
                        color = if (isFromCurrentUser) OnPrimary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
