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
                "approvedByAdminId" to reviewedById,
                "approvalDate" to System.currentTimeMillis()
            )
            comments?.let { updates["adminRemarks"] = it }
            
            android.util.Log.d("FirebaseLeaveRepo", "Updating leave $requestId: status=$status, adminId=$reviewedById")
            leaveCollection.document(requestId).update(updates).await()
            android.util.Log.d("FirebaseLeaveRepo", "✅ Leave $requestId updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseLeaveRepo", "❌ Error updating leave $requestId", e)
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

    // Get employee's leave requests (REAL-TIME)
    fun getEmployeeLeaveRequests(employeeId: String): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        android.util.Log.d("FirebaseLeaveRepo", "Setting up REAL-TIME listener for employee $employeeId leaves...")
        val listener = leaveCollection
            .whereEqualTo("employeeId", employeeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseLeaveRepo", "❌ Error in employee leaves listener", error)
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                // Sort in-memory by requestDate (newest first)
                val sortedRequests = requests.sortedByDescending { it.requestDate?.time ?: 0L }
                android.util.Log.d("FirebaseLeaveRepo", "✅ Employee leaves updated: ${sortedRequests.size} leaves, statuses: ${sortedRequests.map { it.status }}")
                trySend(sortedRequests)
            }
        awaitClose { 
            android.util.Log.d("FirebaseLeaveRepo", "Closing employee leaves listener for $employeeId")
            listener.remove() 
        }
    }

    // Get pending leave requests (admin)
    fun getPendingLeaveRequests(): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        android.util.Log.d("FirebaseLeaveRepo", "Setting up pending leaves listener...")
        val listener = leaveCollection
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseLeaveRepo", "❌ Error in pending leaves listener", error)
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                // Sort in-memory to avoid Firestore index requirement
                val sortedRequests = requests.sortedBy { it.requestDate?.time ?: 0L }
                android.util.Log.d("FirebaseLeaveRepo", "✅ Loaded ${sortedRequests.size} pending leave requests")
                trySend(sortedRequests)
            }
        awaitClose { 
            android.util.Log.d("FirebaseLeaveRepo", "Closing pending leaves listener")
            listener.remove() 
        }
    }

    // Get all leave requests (admin)
    fun getAllLeaveRequests(): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        android.util.Log.d("FirebaseLeaveRepo", "Setting up all leaves listener...")
        val listener = leaveCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseLeaveRepo", "❌ Error in all leaves listener", error)
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                // Sort in-memory (newest first)
                val sortedRequests = requests.sortedByDescending { it.requestDate?.time ?: 0L }
                android.util.Log.d("FirebaseLeaveRepo", "✅ Loaded ${sortedRequests.size} total leave requests")
                trySend(sortedRequests)
            }
        awaitClose { 
            android.util.Log.d("FirebaseLeaveRepo", "Closing all leaves listener")
            listener.remove() 
        }
    }

    // Get leave requests by status
    fun getLeaveRequestsByStatus(status: String): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        val listener = leaveCollection
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                // Sort in-memory (newest first)
                val sortedRequests = requests.sortedByDescending { it.requestDate?.time ?: 0L }
                trySend(sortedRequests)
            }
        awaitClose { listener.remove() }
    }

    // Get leave requests by type
    fun getLeaveRequestsByType(leaveType: String): Flow<List<FirebaseLeaveRequest>> = callbackFlow {
        val listener = leaveCollection
            .whereEqualTo("leaveType", leaveType)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseLeaveRequest::class.java) ?: emptyList()
                // Sort in-memory (newest first)
                val sortedRequests = requests.sortedByDescending { it.requestDate?.time ?: 0L }
                trySend(sortedRequests)
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
