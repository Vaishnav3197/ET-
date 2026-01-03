package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseLeaveRepository {
    private val db = FirebaseFirestore.getInstance()
    private val leaveCollection = db.collection("leave_requests")

    // Submit leave request
    suspend fun addLeaveRequest(request: FirebaseLeaveRequest): Result<String> {
        return submitLeaveRequest(request)
    }

    // Submit leave request (original method)
    suspend fun submitLeaveRequest(request: FirebaseLeaveRequest): Result<String> {
        return try {
            val docRef = leaveCollection.add(request).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update leave request status
    suspend fun updateLeaveStatus(
        requestId: String,
        status: String,
        reviewedById: String,
        comments: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "reviewedById" to reviewedById
            )
            comments?.let { updates["adminComments"] = it }
            
            leaveCollection.document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cancel leave request
    suspend fun cancelLeaveRequest(requestId: String): Result<Unit> {
        return try {
            leaveCollection.document(requestId)
                .update("status", "Cancelled")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete leave request
    suspend fun deleteLeaveRequest(requestId: String): Result<Unit> {
        return try {
            leaveCollection.document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get leave request by ID
    suspend fun getLeaveRequestById(requestId: String): FirebaseLeaveRequest? {
        return try {
            leaveCollection.document(requestId).get().await()
                .toObject(FirebaseLeaveRequest::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get employee's leave requests
    fun getEmployeeLeaveRequests(employeeId: String): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        val listener = leaveCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("submittedDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get pending leave requests (admin)
    fun getPendingLeaveRequests(): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        val listener = leaveCollection
            .whereEqualTo("status", "Pending")
            .orderBy("submittedDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get all leave requests (admin)
    fun getAllLeaveRequests(): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        val listener = leaveCollection
            .orderBy("submittedDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get leave requests by status
    fun getLeaveRequestsByStatus(status: String): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        val listener = leaveCollection
            .whereEqualTo("status", status)
            .orderBy("submittedDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get leave requests by type
    fun getLeaveRequestsByType(leaveType: String): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        val listener = leaveCollection
            .whereEqualTo("leaveType", leaveType)
            .orderBy("submittedDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get leave balance for employee
    suspend fun getLeaveBalance(employeeId: String, leaveType: String): LeaveBalance? {
        return try {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val yearStart = java.util.Calendar.getInstance().apply {
                set(currentYear, 0, 1, 0, 0, 0)
            }.timeInMillis

            val approvedRequests = leaveCollection
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("leaveType", leaveType)
                .whereEqualTo("status", "Approved")
                .whereGreaterThanOrEqualTo("startDate", yearStart)
                .get()
                .await()
                .toObjects(FirebaseLeaveRequest::class.java)

            // Calculate days from startDate and endDate
            val usedDays = approvedRequests.sumOf { 
                val days = (it.endDate - it.startDate) / (1000 * 60 * 60 * 24)
                days.toInt() + 1
            }
            val totalDays = getLeaveAllocation(leaveType)

            LeaveBalance(
                leaveType = leaveType,
                totalDays = totalDays,
                usedDays = usedDays,
                remainingDays = totalDays - usedDays
            )
        } catch (e: Exception) {
            null
        }
    }

    // Get all leave balances for employee
    suspend fun getAllLeaveBalances(employeeId: String): List<LeaveBalance> {
        val leaveTypes = listOf("Sick Leave", "Casual Leave", "Earned Leave", "Unpaid Leave")
        return leaveTypes.mapNotNull { getLeaveBalance(employeeId, it) }
    }

    private fun getLeaveAllocation(leaveType: String): Int {
        return when (leaveType) {
            "Sick Leave" -> 10
            "Casual Leave" -> 10
            "Earned Leave" -> 15
            "Unpaid Leave" -> Int.MAX_VALUE
            else -> 0
        }
    }

    data class LeaveBalance(
        val leaveType: String,
        val totalDays: Int,
        val usedDays: Int,
        val remainingDays: Int
    )
}
