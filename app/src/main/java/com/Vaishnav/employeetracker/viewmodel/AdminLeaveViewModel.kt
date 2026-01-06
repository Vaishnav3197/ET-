package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Vaishnav.employeetracker.data.firebase.FirebaseLeaveRequest
import com.Vaishnav.employeetracker.data.firebase.FirebaseNotification
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AdminLeaveViewModel - ADMIN ONLY
 * 
 * This ViewModel is EXCLUSIVELY for admin leave approval functionality.
 * It does NOT use employeeId or employee-specific logic.
 * It handles viewing ALL employee leave requests and approving/rejecting them.
 */
class AdminLeaveViewModel(application: Application) : AndroidViewModel(application) {
    private val leaveRepo = FirebaseManager.leaveRepository
    private val employeeRepo = FirebaseManager.employeeRepository
    private val notificationRepo = FirebaseManager.notificationRepository
    
    private val _uiState = MutableStateFlow(AdminLeaveUiState())
    val uiState: StateFlow<AdminLeaveUiState> = _uiState.asStateFlow()
    
    init {
        loadPendingLeaves()
    }
    
    /**
     * Load all pending leave requests for admin approval
     * Filters to show only employee leaves (excludes admin's own leaves)
     */
    private fun loadPendingLeaves() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get employee IDs to filter (exclude admin)
                val employeeIds = employeeRepo.getAllEmployeesOnly()
                    .first()
                    .map { it.id }
                    .toSet()
                
                // Collect pending leaves in real-time
                leaveRepo.getPendingLeaveRequests()
                    .catch { e ->
                        android.util.Log.e("AdminLeaveVM", "Error loading pending leaves", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load leave requests: ${e.message}"
                        )
                    }
                    .collect { allPending ->
                        // Filter to show only employee leaves
                        val employeeLeaves = if (employeeIds.isNotEmpty()) {
                            allPending.filter { it.employeeId in employeeIds }
                        } else {
                            allPending
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            pendingLeaves = employeeLeaves,
                            pendingCount = employeeLeaves.size,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("AdminLeaveVM", "Fatal error in loadPendingLeaves", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to initialize: ${e.message}",
                    pendingLeaves = emptyList(),
                    pendingCount = 0
                )
            }
        }
    }
    
    /**
     * Approve a leave request
     * @param leaveId The leave request ID to approve
     * @param adminId The admin's user ID (Firebase Auth UID)
     * @param remarks Optional approval remarks
     */
    suspend fun approveLeave(
        leaveId: String,
        adminId: String,
        remarks: String = ""
    ): Result<String> {
        return try {
            // Find the leave request
            val leave = _uiState.value.pendingLeaves.find { it.id == leaveId }
                ?: return Result.failure(Exception("Leave request not found"))
            
            // Update leave status
            leaveRepo.updateLeaveStatus(leaveId, "Approved", adminId, remarks)
            
            // Send notification to employee
            val notification = FirebaseNotification(
                id = "",
                userId = leave.employeeId,
                title = "Leave Approved ✅",
                message = "Your leave request from ${DateTimeHelper.formatDate(leave.startDate)} to ${DateTimeHelper.formatDate(leave.endDate)} has been approved${if (remarks.isNotBlank()) ". Remarks: $remarks" else ""}",
                type = "Leave",
                relatedId = leaveId,
                timestamp = null,
                isRead = false
            )
            notificationRepo.addNotification(notification)
            
            android.util.Log.d("AdminLeaveVM", "Leave $leaveId approved successfully")
            Result.success("Leave approved successfully")
        } catch (e: Exception) {
            android.util.Log.e("AdminLeaveVM", "Error approving leave", e)
            Result.failure(Exception("Failed to approve leave: ${e.message}"))
        }
    }
    
    /**
     * Reject a leave request
     * @param leaveId The leave request ID to reject
     * @param adminId The admin's user ID (Firebase Auth UID)
     * @param remarks Required rejection reason
     */
    suspend fun rejectLeave(
        leaveId: String,
        adminId: String,
        remarks: String
    ): Result<String> {
        return try {
            // Validate remarks
            if (remarks.isBlank()) {
                return Result.failure(Exception("Please provide a reason for rejection"))
            }
            
            // Find the leave request
            val leave = _uiState.value.pendingLeaves.find { it.id == leaveId }
                ?: return Result.failure(Exception("Leave request not found"))
            
            // Update leave status
            leaveRepo.updateLeaveStatus(leaveId, "Rejected", adminId, remarks)
            
            // Send notification to employee
            val notification = FirebaseNotification(
                id = "",
                userId = leave.employeeId,
                title = "Leave Rejected ❌",
                message = "Your leave request from ${DateTimeHelper.formatDate(leave.startDate)} to ${DateTimeHelper.formatDate(leave.endDate)} has been rejected. Reason: $remarks",
                type = "Leave",
                relatedId = leaveId,
                timestamp = null,
                isRead = false
            )
            notificationRepo.addNotification(notification)
            
            android.util.Log.d("AdminLeaveVM", "Leave $leaveId rejected successfully")
            Result.success("Leave rejected successfully")
        } catch (e: Exception) {
            android.util.Log.e("AdminLeaveVM", "Error rejecting leave", e)
            Result.failure(Exception("Failed to reject leave: ${e.message}"))
        }
    }
    
    /**
     * Refresh leave data manually
     */
    fun refresh() {
        loadPendingLeaves()
    }
}

/**
 * UI State for Admin Leave Screen
 * Handles loading, error, and empty states properly
 */
data class AdminLeaveUiState(
    val isLoading: Boolean = true,
    val pendingLeaves: List<FirebaseLeaveRequest> = emptyList(),
    val pendingCount: Int = 0,
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && pendingLeaves.isEmpty() && error == null
    
    val hasError: Boolean
        get() = error != null
}
