package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseShift
import com.Vaishnav.employeetracker.data.firebase.FirebaseShiftAssignment
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*

class ShiftViewModel(application: Application) : AndroidViewModel(application) {
    private val shiftRepo = FirebaseManager.shiftRepository
    private val employeeRepo = FirebaseManager.employeeRepository
    
    val allShifts = shiftRepo.getAllShifts()
    
    fun getPendingSwapRequests(adminId: String? = null): Flow<List<com.Vaishnav.employeetracker.data.firebase.FirebaseShiftSwapRequest>> {
        return if (adminId != null) {
            // Filter by admin's employees with continuous observation
            shiftRepo.getPendingSwapRequests().map { allRequests ->
                employeeRepo.getEmployeesByAdminId(adminId).first().let { employees ->
                    val employeeIds = employees.map { it.id }.toSet()
                    allRequests.filter { 
                        employeeIds.contains(it.requesterId) || employeeIds.contains(it.targetEmployeeId)
                    }
                }
            }
        } else {
            // Show all pending swap requests
            shiftRepo.getPendingSwapRequests()
        }
    }
    
    suspend fun createShift(
        shiftName: String,
        startTime: String,
        endTime: String,
        graceMinutes: Int
    ): Result<Long> {
        return try {
            val shift = FirebaseShift(
                id = "",
                shiftName = shiftName,
                startTime = startTime,
                endTime = endTime,
                graceMinutes = graceMinutes
            )
            shiftRepo.addShift(shift)
            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun assignShift(employeeId: String, shiftId: String, date: Long): Result<Long> {
        return try {
            val assignment = FirebaseShiftAssignment(
                id = "",
                employeeId = employeeId,
                shiftId = shiftId,
                date = date
            )
            shiftRepo.assignShift(assignment)
            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getShiftAssignmentsForDate(date: Long) = shiftRepo.getShiftAssignmentsForDate(date)
    
    fun getEmployeeSwapRequests(employeeId: String) = shiftRepo.getSwapRequestsByRequester(employeeId)
    
    suspend fun requestShiftSwap(
        requesterId: String,
        targetEmployeeId: String,
        requesterShiftDate: Long,
        targetShiftDate: Long,
        reason: String
    ): Result<Long> {
        return try {
            val swapRequest = com.Vaishnav.employeetracker.data.firebase.FirebaseShiftSwapRequest(
                id = "",
                requesterId = requesterId,
                targetEmployeeId = targetEmployeeId,
                requesterShiftDate = requesterShiftDate,
                targetShiftDate = targetShiftDate,
                reason = reason,
                status = "Pending",
                requestedAt = null, // ServerTimestamp
                respondedAt = null,
                adminRemarks = null
            )
            shiftRepo.requestShiftSwap(swapRequest)
            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun approveSwapRequest(requestId: String, adminRemarks: String): Result<String> {
        return try {
            val result = shiftRepo.approveSwapRequest(requestId, adminRemarks)
            if (result.isSuccess) {
                // Process the actual shift swap
                shiftRepo.processShiftSwap(requestId)
                Result.success("Swap request approved and processed")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to approve"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rejectSwapRequest(requestId: String, adminRemarks: String): Result<String> {
        return try {
            shiftRepo.rejectSwapRequest(requestId, adminRemarks)
            Result.success("Swap request rejected")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
