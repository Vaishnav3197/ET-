package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationCollection = db.collection("notifications")

    // Send notification
    suspend fun addNotification(notification: FirebaseNotification): Result<String> {
        return sendNotification(notification)
    }

    // Send notification (original method)
    suspend fun sendNotification(notification: FirebaseNotification): Result<String> {
        return try {
            val docRef = notificationCollection.add(notification).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark as read
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark all as read for user
    suspend fun markAllAsRead(employeeId: String): Result<Unit> {
        return try {
            val unreadNotifications = notificationCollection
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = db.batch()
            unreadNotifications.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete notification
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete all notifications for user
    suspend fun deleteAllNotifications(employeeId: String): Result<Unit> {
        return try {
            val notifications = notificationCollection
                .whereEqualTo("employeeId", employeeId)
                .get()
                .await()

            val batch = db.batch()
            notifications.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get employee's notifications
    fun getEmployeeNotifications(employeeId: String): Flow<List<FirebaseNotification>> = callbackFlow {
        val listener = notificationCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("sentDate", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(FirebaseNotification::class.java) ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    // Get unread notifications
    fun getUnreadNotifications(employeeId: String): Flow<List<FirebaseNotification>> = callbackFlow {
        val listener = notificationCollection
            .whereEqualTo("employeeId", employeeId)
            .whereEqualTo("isRead", false)
            .orderBy("sentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(FirebaseNotification::class.java) ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    // Get unread count
    fun getUnreadCount(employeeId: String): Flow<Int> = callbackFlow {
        val listener = notificationCollection
            .whereEqualTo("employeeId", employeeId)
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

    // Get notifications by type
    fun getNotificationsByType(employeeId: String, type: String): Flow<List<FirebaseNotification>> = callbackFlow {
        val listener = notificationCollection
            .whereEqualTo("employeeId", employeeId)
            .whereEqualTo("type", type)
            .orderBy("sentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(FirebaseNotification::class.java) ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    // Send notification to multiple users
    suspend fun sendBulkNotifications(userIds: List<String>, notification: FirebaseNotification): Result<Int> {
        return try {
            val batch = db.batch()
            var count = 0

            userIds.forEach { userId ->
                val notif = notification.copy(userId = userId)
                val docRef = notificationCollection.document()
                batch.set(docRef, notif)
                count++

                // Firestore batch limit is 500 operations
                if (count % 500 == 0) {
                    batch.commit().await()
                }
            }

            if (count % 500 != 0) {
                batch.commit().await()
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Send notification to all active employees
    suspend fun sendToAllEmployees(notification: FirebaseNotification): Result<Int> {
        return try {
            // Get all active employee IDs
            val employeeIds = db.collection("employees")
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.id }

            sendBulkNotifications(employeeIds, notification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete old notifications (older than 30 days)
    suspend fun deleteOldNotifications(employeeId: String, daysOld: Int = 30): Result<Int> {
        return try {
            val cutoffDate = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            
            val oldNotifications = notificationCollection
                .whereEqualTo("employeeId", employeeId)
                .whereLessThan("sentDate", cutoffDate)
                .get()
                .await()

            val batch = db.batch()
            oldNotifications.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Result.success(oldNotifications.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
