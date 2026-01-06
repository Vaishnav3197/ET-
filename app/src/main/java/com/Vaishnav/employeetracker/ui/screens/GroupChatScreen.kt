package com.Vaishnav.employeetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseMessage
import com.Vaishnav.employeetracker.viewmodel.MessagingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * WhatsApp-style Group Chat Screen
 * All employees + admin in one group chat
 * Real-time updates with message persistence
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String = "company_group",
    onNavigateBack: () -> Unit,
    messagingViewModel: MessagingViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var messageText by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var currentUserName by remember { mutableStateOf("User") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Get messages from ViewModel
    val messages by messagingViewModel.getGroupFirebaseMessages(groupId).collectAsState(initial = emptyList())
    
    // Get current user info
    val firebaseAuthManager = remember { FirebaseAuthManager.getInstance() }
    val firebaseUserId = firebaseAuthManager.getCurrentUserId()
    
    // Load current user data
    LaunchedEffect(firebaseUserId) {
        if (firebaseUserId.isNullOrEmpty()) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            val employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
            if (employee != null) {
                currentUserId = employee.id
                currentUserName = employee.name
                android.util.Log.d("GroupChat", "Loaded user: ${employee.name} (ID: ${employee.id})")
            } else {
                errorMessage = "User profile not found"
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupChat", "Error loading user", e)
            errorMessage = "Error loading profile: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Company Chat", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isLoading) "Loading..." else "${messages.size} messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1)
                )
            )
        },
        containerColor = Color(0xFFECE5DD) // WhatsApp-like background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Show error if any
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "👋 Welcome to Company Chat!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start a conversation with your team",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == currentUserId,
                            currentUserName = currentUserName
                        )
                    }
                }
            }
            
            // Message input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        enabled = !isLoading && currentUserId != null
                    )
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentUserId != null) {
                                scope.launch {
                                    messagingViewModel.sendMessage(
                                        senderId = currentUserId!!,
                                        receiverId = null,
                                        groupId = groupId,
                                        message = messageText.trim()
                                    )
                                    messageText = ""
                                }
                            }
                        },
                        containerColor = Color(0xFF6366F1),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: FirebaseMessage,
    isCurrentUser: Boolean,
    currentUserName: String
) {
    val scope = rememberCoroutineScope()
    var senderName by remember { mutableStateOf<String?>(null) }
    
    // Load sender name
    LaunchedEffect(message.senderId) {
        scope.launch {
            try {
                val employee = FirebaseManager.employeeRepository.getEmployeeById(message.senderId)
                senderName = employee?.name ?: "Unknown"
            } catch (e: Exception) {
                senderName = "User"
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) Color(0xFFDCF8C6) else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show sender name for other users' messages
                if (!isCurrentUser && senderName != null) {
                    Text(
                        text = senderName!!,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Message text
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                
                // Timestamp
                message.timestamp?.let { timestamp ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTime(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

private fun formatTime(date: Date): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }
    
    return when {
        now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
        }
        else -> {
            SimpleDateFormat("MMM dd yyyy, HH:mm", Locale.getDefault()).format(date)
        }
    }
}
