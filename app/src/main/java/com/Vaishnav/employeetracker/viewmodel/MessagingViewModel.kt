package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.FirebaseChatGroup
import com.Vaishnav.employeetracker.data.firebase.FirebaseGroupMember
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseMessage
import kotlinx.coroutines.flow.*

class MessagingViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepo = FirebaseManager.messageRepository
    private val employeeRepo = FirebaseManager.employeeRepository
    
    companion object {
        private const val TAG = "MessagingViewModel"
    }
    
    // Get all messages sent by user
    fun getUserMessages(userId: String) = messageRepo.getUserMessages(userId)
    
    // Get conversation between two users (1-on-1 chat)
    fun getConversation(user1Id: String, user2Id: String): Flow<List<Message>> = flow {
        try {
            // Create a unique group ID for 1-on-1 chat (consistent regardless of order)
            val groupId = if (user1Id < user2Id) "${user1Id}_${user2Id}" else "${user2Id}_${user1Id}"
            
            messageRepo.getGroupMessages(groupId).collect { firebaseMessages ->
                val messages = firebaseMessages.map { fbMsg ->
                    Message(
                        id = fbMsg.id.hashCode(),
                        senderId = fbMsg.senderId.hashCode(),
                        receiverId = fbMsg.receiverId?.hashCode(),
                        groupId = null,
                        message = fbMsg.message,
                        timestamp = fbMsg.timestamp?.time ?: System.currentTimeMillis(),
                        isRead = fbMsg.isRead,
                        messageType = fbMsg.messageType
                    )
                }
                emit(messages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting conversation", e)
            emit(emptyList())
        }
    }
    
    // Get recent conversations with last message for each user
    fun getRecentConversations(userId: String): Flow<List<Pair<String, Message>>> = flow {
        try {
            messageRepo.getUserMessages(userId).collect { firebaseMessages ->
                // Group messages by conversation partner
                val conversationMap = mutableMapOf<String, FirebaseMessage>()
                
                firebaseMessages.forEach { msg ->
                    val partnerId = if (msg.senderId == userId) msg.receiverId else msg.senderId
                    partnerId?.let {
                        val existing = conversationMap[it]
                        if (existing == null || (msg.timestamp?.time ?: 0) > (existing.timestamp?.time ?: 0)) {
                            conversationMap[it] = msg
                        }
                    }
                }
                
                // Convert to list of pairs
                val conversations = conversationMap.map { (partnerId, lastMsg) ->
                    partnerId to Message(
                        id = lastMsg.id.hashCode(),
                        senderId = lastMsg.senderId.hashCode(),
                        receiverId = lastMsg.receiverId?.hashCode(),
                        groupId = null,
                        message = lastMsg.message,
                        timestamp = lastMsg.timestamp?.time ?: System.currentTimeMillis(),
                        isRead = lastMsg.isRead,
                        messageType = lastMsg.messageType
                    )
                }.sortedByDescending { it.second.timestamp }
                
                emit(conversations)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent conversations", e)
            emit(emptyList())
        }
    }
    
    // Get messages for a group chat
    fun getGroupMessages(groupId: String): Flow<List<Message>> = flow {
        try {
            messageRepo.getGroupMessages(groupId).collect { firebaseMessages ->
                val messages = firebaseMessages.map { fbMsg ->
                    Message(
                        id = fbMsg.id.hashCode(),
                        senderId = fbMsg.senderId.hashCode(),
                        receiverId = null,
                        groupId = groupId.hashCode(),
                        message = fbMsg.message,
                        timestamp = fbMsg.timestamp?.time ?: System.currentTimeMillis(),
                        isRead = fbMsg.isRead,
                        messageType = fbMsg.messageType
                    )
                }
                emit(messages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting group messages", e)
            emit(emptyList())
        }
    }
    
    // Get raw Firebase messages for a group chat (for GroupChatScreen)
    fun getGroupFirebaseMessages(groupId: String): Flow<List<FirebaseMessage>> {
        return messageRepo.getGroupMessages(groupId)
    }
    
    // Get unread message count for a group/conversation
    fun getUnreadCount(groupId: String, userId: String): Flow<Int> {
        return messageRepo.getUnreadMessageCount(groupId, userId)
    }
    
    // Send a message (1-on-1 or group)
    suspend fun sendMessage(
        senderId: String,
        receiverId: String?,
        groupId: String?,
        message: String,
        messageType: String = "Text"
    ): Result<String> {
        return try {
            // For 1-on-1 chat, create consistent group ID
            val actualGroupId = if (receiverId != null && groupId == null) {
                if (senderId < receiverId) "${senderId}_${receiverId}" else "${receiverId}_${senderId}"
            } else {
                groupId
            }
            
            val msg = FirebaseMessage(
                id = "",
                senderId = senderId,
                receiverId = receiverId,
                groupId = actualGroupId,
                message = message,
                timestamp = null, // ServerTimestamp
                isRead = false,
                messageType = messageType
            )
            
            messageRepo.sendMessage(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    // Mark messages as read in a conversation
    suspend fun markAsRead(userId: String, senderId: String) {
        try {
            val groupId = if (userId < senderId) "${userId}_${senderId}" else "${senderId}_${userId}"
            
            // Get all unread messages from sender in this conversation
            messageRepo.getGroupMessages(groupId).first().forEach { msg ->
                if (msg.senderId == senderId && !msg.isRead) {
                    messageRepo.markAsRead(msg.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
        }
    }
    
    // Get all chat groups
    val allGroups: Flow<List<ChatGroup>> = messageRepo.getAllChatGroups().map { firebaseGroups ->
        firebaseGroups.map { fbGroup ->
            ChatGroup(
                id = fbGroup.id.hashCode(),
                groupName = fbGroup.groupName,
                groupType = fbGroup.groupType,
                createdBy = fbGroup.createdBy.hashCode(),
                createdAt = fbGroup.createdAt?.time ?: System.currentTimeMillis(),
                isActive = fbGroup.isActive
            )
        }
    }
    
    // Get groups for specific employee
    fun getEmployeeGroups(employeeId: String): Flow<List<ChatGroup>> {
        return messageRepo.getUserChatGroups(employeeId).map { firebaseGroups ->
            firebaseGroups.map { fbGroup ->
                ChatGroup(
                    id = fbGroup.id.hashCode(),
                    groupName = fbGroup.groupName,
                    groupType = fbGroup.groupType,
                    createdBy = fbGroup.createdBy.hashCode(),
                    createdAt = fbGroup.createdAt?.time ?: System.currentTimeMillis(),
                    isActive = fbGroup.isActive
                )
            }
        }
    }
    
    // Get members of a group
    fun getGroupMembers(groupId: String): Flow<List<GroupMember>> {
        return messageRepo.getGroupMembers(groupId).map { firebaseMembers ->
            firebaseMembers.map { fbMember ->
                GroupMember(
                    id = fbMember.id.hashCode(),
                    groupId = groupId.hashCode(),
                    employeeId = fbMember.employeeId.hashCode(),
                    joinedAt = fbMember.joinedAt?.time ?: System.currentTimeMillis(),
                    isAdmin = fbMember.isAdmin
                )
            }
        }
    }
    
    // Create a new group chat
    suspend fun createGroup(
        groupName: String,
        groupType: String,
        createdBy: String,
        memberIds: List<String>
    ): Result<String> {
        return try {
            val group = FirebaseChatGroup(
                id = "",
                groupName = groupName,
                groupType = groupType,
                createdBy = createdBy,
                createdAt = null, // ServerTimestamp
                isActive = true
            )
            
            messageRepo.createChatGroup(group, memberIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group", e)
            Result.failure(e)
        }
    }
    
    // Add member to existing group
    suspend fun addGroupMember(groupId: String, employeeId: String, isAdmin: Boolean = false): Result<String> {
        return try {
            messageRepo.addGroupMember(groupId, employeeId, isAdmin)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding group member", e)
            Result.failure(e)
        }
    }
    
    // Remove member from group
    suspend fun removeGroupMember(groupId: String, employeeId: String): Result<Unit> {
        return try {
            messageRepo.removeGroupMember(groupId, employeeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing group member", e)
            Result.failure(e)
        }
    }
}
