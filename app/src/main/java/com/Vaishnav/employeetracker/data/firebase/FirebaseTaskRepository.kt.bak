package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseTaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val taskCollection = db.collection("tasks")

    // Create task
    suspend fun addTask(task: FirebaseTask): Result<String> {
        return try {
            val docRef = taskCollection.add(task).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update task
    suspend fun updateTask(taskId: String, task: FirebaseTask): Result<Unit> {
        return try {
            taskCollection.document(taskId).set(task).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update task status
    suspend fun updateTaskStatus(taskId: String, status: String): Result<Unit> {
        return try {
            taskCollection.document(taskId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update task progress
    suspend fun updateTaskProgress(taskId: String, progress: Int): Result<Unit> {
        return try {
            taskCollection.document(taskId).update("progress", progress).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete task
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            taskCollection.document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get task by ID
    suspend fun getTaskById(taskId: String): FirebaseTask? {
        return try {
            taskCollection.document(taskId).get().await()
                .toObject(FirebaseTask::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get tasks assigned to employee
    fun getEmployeeTasks(employeeId: String): Flow<List<FirebaseTask>> = callbackFlow {
        val listener = taskCollection
            .whereEqualTo("assignedToId", employeeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(FirebaseTask::class.java) ?: emptyList()
                // Sort in memory by createdAt descending
                val sortedTasks = tasks.sortedByDescending { it.createdAt?.time ?: 0L }
                trySend(sortedTasks)
            }
        awaitClose { listener.remove() }
    }

    // Get tasks by status for employee
    fun getEmployeeTasksByStatus(employeeId: String, status: String): Flow<List<FirebaseTask>> = callbackFlow {
        val listener = taskCollection
            .whereEqualTo("assignedToId", employeeId)
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(FirebaseTask::class.java) ?: emptyList()
                // Sort in memory by deadline ascending
                val sortedTasks = tasks.sortedBy { it.deadline }
                trySend(sortedTasks)
            }
        awaitClose { listener.remove() }
    }

    // Get tasks created by employee (admin/manager)
    fun getTasksCreatedBy(creatorId: String): Flow<List<FirebaseTask>> = callbackFlow {
        val listener = taskCollection
            .whereEqualTo("createdById", creatorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(FirebaseTask::class.java) ?: emptyList()
                // Sort in memory by createdAt descending
                val sortedTasks = tasks.sortedByDescending { it.createdAt?.time ?: 0L }
                trySend(sortedTasks)
            }
        awaitClose { listener.remove() }
    }

    // Get all tasks (admin)
    fun getAllTasks(): Flow<List<FirebaseTask>> = callbackFlow {
        val listener = taskCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(FirebaseTask::class.java) ?: emptyList()
                // Sort in memory by createdAt descending
                val sortedTasks = tasks.sortedByDescending { it.createdAt?.time ?: 0L }
                trySend(sortedTasks)
            }
        awaitClose { listener.remove() }
    }

    // Get tasks by priority
    fun getTasksByPriority(priority: String): Flow<List<FirebaseTask>> = callbackFlow {
        val listener = taskCollection
            .whereEqualTo("priority", priority)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(FirebaseTask::class.java) ?: emptyList()
                // Sort in memory by deadline ascending
                val sortedTasks = tasks.sortedBy { it.deadline }
                trySend(sortedTasks)
            }
        awaitClose { listener.remove() }
    }

    // Get overdue tasks
    fun getOverdueTasks(currentTime: Long): Flow<List<FirebaseTask>> = callbackFlow {
        val listener = taskCollection
            .whereLessThan("deadline", currentTime)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(FirebaseTask::class.java) ?: emptyList()
                // Filter and sort in memory
                val overdueTasks = tasks
                    .filter { it.status != "Completed" }
                    .sortedBy { it.deadline }
                trySend(overdueTasks)
            }
        awaitClose { listener.remove() }
    }

    // Get task statistics for employee
    suspend fun getEmployeeTaskStats(employeeId: String): TaskStats? {
        return try {
            val tasks = taskCollection
                .whereEqualTo("assignedToId", employeeId)
                .get()
                .await()
                .toObjects(FirebaseTask::class.java)

            if (tasks.isEmpty()) return null

            val total = tasks.size
            val completed = tasks.count { it.status == "Completed" }
            val inProgress = tasks.count { it.status == "In Progress" }
            val pending = tasks.count { it.status == "Pending" }

            TaskStats(total, completed, inProgress, pending)
        } catch (e: Exception) {
            null
        }
    }
    
    // Get tasks by employee (for analytics)
    fun getTasksByEmployee(employeeId: String): Flow<List<FirebaseTask>> = callbackFlow {
        val listener = taskCollection
            .whereEqualTo("assignedToId", employeeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.toObjects(FirebaseTask::class.java) ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    data class TaskStats(
        val total: Int,
        val completed: Int,
        val inProgress: Int,
        val pending: Int
    )
}
