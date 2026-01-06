package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.*
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskRepo = FirebaseManager.taskRepository
    private val notificationRepo = FirebaseManager.notificationRepository
    
    fun getMyTasks(employeeId: String): Flow<List<Task>> {
        return if (employeeId.isEmpty()) {
            // Admin view - get all tasks
            taskRepo.getAllTasks().map { firebaseTasks ->
                firebaseTasks.map { it.toRoomTask() }
            }
        } else {
            // Employee view - get employee's tasks
            taskRepo.getEmployeeTasks(employeeId).map { firebaseTasks ->
                firebaseTasks.map { it.toRoomTask() }
            }
        }
    }
    
    fun getTasksByStatus(employeeId: String, status: String): Flow<List<Task>> {
        return taskRepo.getEmployeeTasksByStatus(employeeId, status).map { firebaseTasks ->
            firebaseTasks.map { it.toRoomTask() }
        }
    }
    
    fun getOverdueTasks(): Flow<List<Task>> {
        return taskRepo.getOverdueTasks(System.currentTimeMillis()).map { firebaseTasks ->
            firebaseTasks.map { it.toRoomTask() }
        }
    }
    
    suspend fun getTaskStats(employeeId: String): TaskStats {
        return try {
            val tasks = runCatching {
                taskRepo.getEmployeeTasks(employeeId).first()
            }.getOrElse { emptyList() }
            
            val completed = tasks.count { it.status == "Completed" }
            val total = tasks.size.coerceAtLeast(1) // Prevent division by zero
            val completionRate = if (total > 0) {
                (completed.toFloat() / total) * 100
            } else {
                0f
            }
            
            TaskStats(completed, total, completionRate.coerceIn(0f, 100f))
        } catch (e: Exception) {
            android.util.Log.e("TaskViewModel", "Error getting task stats", e)
            TaskStats(0, 0, 0f)
        }
    }
    
    suspend fun updateTaskStatus(taskId: Int, status: String): Result<String> {
        return try {
            if (taskId <= 0) {
                return Result.failure(Exception("Invalid task ID"))
            }
            
            val task = runCatching {
                taskRepo.getTaskById(taskId.toString())
            }.getOrNull()
            
            if (task == null) {
                return Result.failure(Exception("Task not found"))
            }
            
            val updatedTask = task.copy(status = status)
            taskRepo.updateTask(taskId.toString(), updatedTask)
            Result.success("Task updated to $status")
        } catch (e: Exception) {
            android.util.Log.e("TaskViewModel", "Error updating task status", e)
            Result.failure(e)
        }
    }
    
    suspend fun createTask(
        title: String,
        description: String,
        assignedToId: Int,
        assignedByAdminId: Int,
        deadline: Long,
        priority: String
    ): Result<String> {
        return try {
            val task = FirebaseTask(
                title = title,
                description = description,
                assignedToId = assignedToId.toString(),
                assignedByAdminId = assignedByAdminId.toString(),
                deadline = deadline,
                priority = priority,
                status = "Pending"
            )
            
            val result = taskRepo.addTask(task)
            val taskId = result.getOrNull()
            
            if (taskId != null) {
                // Send notification to employee
                val notification = FirebaseNotification(
                    userId = assignedToId.toString(),
                    title = "New Task Assigned",
                    message = "You have been assigned a new task: $title",
                    type = "Task",
                    relatedId = taskId,
                    isRead = false
                )
                notificationRepo.sendNotification(notification)
                
                Result.success(taskId)
            } else {
                Result.failure(Exception("Failed to create task"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // New method that accepts String Firebase IDs directly
    suspend fun createTaskWithFirebaseIds(
        title: String,
        description: String,
        assignedToFirebaseId: String,
        assignedByAdminFirebaseId: String,
        deadline: Long,
        priority: String
    ): Result<String> {
        return try {
            val task = FirebaseTask(
                title = title,
                description = description,
                assignedToId = assignedToFirebaseId,  // Use Firebase document ID directly
                assignedByAdminId = assignedByAdminFirebaseId,
                deadline = deadline,
                priority = priority,
                status = "Pending"
            )
            
            val result = taskRepo.addTask(task)
            val taskId = result.getOrNull()
            
            if (taskId != null) {
                // Send notification to employee using Firebase ID
                val notification = FirebaseNotification(
                    userId = assignedToFirebaseId,
                    title = "New Task Assigned",
                    message = "You have been assigned a new task: $title",
                    type = "Task",
                    relatedId = taskId,
                    isRead = false
                )
                notificationRepo.sendNotification(notification)
                
                Result.success(taskId)
            } else {
                Result.failure(Exception("Failed to create task"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTask(taskId: Int): Result<String> {
        return try {
            val result = taskRepo.deleteTask(taskId.toString())
            if (result.isSuccess) {
                Result.success("Task deleted successfully")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to delete task"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class TaskStats(
        val completedCount: Int,
        val totalCount: Int,
        val completionRate: Float
    )
}
