package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseMessageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val messageCollection = db.collection("messages")
    private val chatGroupCollection = db.collection("chat_groups")
    private val groupMemberCollection = db.collection("group_members")

    // ============= Message Management =============

    // Send message
    suspend fun sendMessage(message: FirebaseMessage): Result<String> {
        return try {
            val docRef = messageCollection.add(message).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update message
    suspend fun updateMessage(messageId: String, newMessage: String): Result<Unit> {
        return try {
            messageCollection.document(messageId).update(
                mapOf(
                    "message" to newMessage
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete message
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            messageCollection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark message as read
    suspend fun markAsRead(messageId: String): Result<Unit> {
        return try {
            messageCollection.document(messageId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get messages for a group
    fun getGroupMessages(groupId: String): Flow<List<FirebaseMessage>> = callbackFlow {
        val listener = messageCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("sentTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(FirebaseMessage::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // Get unread message count for user in a group
    fun getUnreadMessageCount(groupId: String, userId: String): Flow<Int> = callbackFlow {
        val listener = messageCollection
            .whereEqualTo("groupId", groupId)
            .whereNotEqualTo("senderId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    // Get messages sent by user
    fun getMessagesBySender(senderId: String): Flow<List<FirebaseMessage>> = callbackFlow {
        val listener = messageCollection
            .whereEqualTo("senderId", senderId)
            .orderBy("sentTime", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(FirebaseMessage::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // Get conversation between two users
    fun getConversation(user1Id: String, user2Id: String): Flow<List<FirebaseMessage>> = callbackFlow {
        // Create consistent group ID for 1-on-1 chat
        val groupId = if (user1Id < user2Id) "${user1Id}_${user2Id}" else "${user2Id}_${user1Id}"
        
        val listener = messageCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(FirebaseMessage::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // Get all messages for a user (sent or received)
    fun getUserMessages(userId: String): Flow<List<FirebaseMessage>> = callbackFlow {
        val listener = messageCollection
            .whereEqualTo("senderId", userId)
            .orderBy("sentTime", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(FirebaseMessage::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // ============= Chat Group Management =============

    // Create chat group
    suspend fun createChatGroup(group: FirebaseChatGroup, memberIds: List<String>): Result<String> {
        return try {
            // Create the group
            val groupRef = chatGroupCollection.add(group).await()
            val groupId = groupRef.id

            // Add members to the group
            val batch = db.batch()
            memberIds.forEach { memberId ->
                val member = FirebaseGroupMember(
                    groupId = groupId,
                    employeeId = memberId,
                    isAdmin = (memberId == group.createdBy)
                )
                val memberRef = groupMemberCollection.document()
                batch.set(memberRef, member)
            }
            batch.commit().await()

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update chat group
    suspend fun updateChatGroup(groupId: String, group: FirebaseChatGroup): Result<Unit> {
        return try {
            chatGroupCollection.document(groupId).set(group).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete chat group
    suspend fun deleteChatGroup(groupId: String): Result<Unit> {
        return try {
            // Delete all messages in the group
            val messages = messageCollection.whereEqualTo("groupId", groupId).get().await()
            val batch = db.batch()
            messages.documents.forEach { batch.delete(it.reference) }

            // Delete all group members
            val members = groupMemberCollection.whereEqualTo("groupId", groupId).get().await()
            members.documents.forEach { batch.delete(it.reference) }

            // Delete the group
            batch.delete(chatGroupCollection.document(groupId))
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get user's chat groups
    fun getUserChatGroups(userId: String): Flow<List<FirebaseChatGroup>> = callbackFlow {
        val listener = groupMemberCollection
            .whereEqualTo("memberId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val groupIds = snapshot?.documents?.mapNotNull { it.get("groupId") as? String } ?: emptyList()
                
                if (groupIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Get group details for all groups user is a member of
                chatGroupCollection
                    .whereIn("__name__", groupIds.take(10)) // Firestore 'in' query limit is 10
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .addSnapshotListener { groupSnapshot, groupError ->
                        if (groupError != null) {
                            close(groupError)
                            return@addSnapshotListener
                        }
                        val groups = groupSnapshot?.toObjects(FirebaseChatGroup::class.java) ?: emptyList()
                        trySend(groups)
                    }
            }
        awaitClose { listener.remove() }
    }

    // Get chat group by ID
    suspend fun getChatGroupById(groupId: String): FirebaseChatGroup? {
        return try {
            chatGroupCollection.document(groupId).get().await()
                .toObject(FirebaseChatGroup::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get all chat groups (admin)
    fun getAllChatGroups(): Flow<List<FirebaseChatGroup>> = callbackFlow {
        val listener = chatGroupCollection
            .orderBy("createdDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.toObjects(FirebaseChatGroup::class.java) ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    // ============= Group Member Management =============

    // Add member to group
    suspend fun addGroupMember(groupId: String, memberId: String, isAdmin: Boolean = false): Result<String> {
        return try {
            val member = FirebaseGroupMember(
                groupId = groupId,
                employeeId = memberId,
                isAdmin = isAdmin
            )
            val docRef = groupMemberCollection.add(member).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Remove member from group
    suspend fun removeGroupMember(groupId: String, memberId: String): Result<Unit> {
        return try {
            val members = groupMemberCollection
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("employeeId", memberId)
                .get()
                .await()

            val batch = db.batch()
            members.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update member role
    suspend fun updateMemberRole(groupId: String, memberId: String, isAdmin: Boolean): Result<Unit> {
        return try {
            val members = groupMemberCollection
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("employeeId", memberId)
                .get()
                .await()

            val batch = db.batch()
            members.documents.forEach { batch.update(it.reference, "isAdmin", isAdmin) }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get group members
    fun getGroupMembers(groupId: String): Flow<List<FirebaseGroupMember>> = callbackFlow {
        val listener = groupMemberCollection
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(FirebaseGroupMember::class.java) ?: emptyList()
                trySend(members)
            }
        awaitClose { listener.remove() }
    }

    // Check if user is member of group
    suspend fun isMember(groupId: String, memberId: String): Boolean {
        return try {
            val members = groupMemberCollection
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("memberId", memberId)
                .get()
                .await()

            !members.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // Get member count for group
    suspend fun getMemberCount(groupId: String): Int {
        return try {
            groupMemberCollection
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }
}
