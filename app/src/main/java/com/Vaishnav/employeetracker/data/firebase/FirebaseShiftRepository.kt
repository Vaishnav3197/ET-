package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseShiftRepository {
    private val db = FirebaseFirestore.getInstance()
    private val shiftCollection = db.collection("shifts")
    private val assignmentCollection = db.collection("shift_assignments")
    private val swapRequestCollection = db.collection("shift_swap_requests")

    // ============= Shift Management =============

    // Create shift
    suspend fun addShift(shift: FirebaseShift): Result<String> {
        return try {
            val docRef = shiftCollection.add(shift).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update shift
    suspend fun updateShift(shiftId: String, shift: FirebaseShift): Result<Unit> {
        return try {
            shiftCollection.document(shiftId).set(shift).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete shift
    suspend fun deleteShift(shiftId: String): Result<Unit> {
        return try {
            shiftCollection.document(shiftId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all shifts
    fun getAllShifts(): Flow<List<FirebaseShift>> = callbackFlow {
        val listener = shiftCollection
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val shifts = snapshot?.toObjects(FirebaseShift::class.java) ?: emptyList()
                trySend(shifts)
            }
        awaitClose { listener.remove() }
    }

    // ============= Shift Assignment Management =============

    // Assign shift to employee
    suspend fun assignShift(assignment: FirebaseShiftAssignment): Result<String> {
        return try {
            val docRef = assignmentCollection.add(assignment).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update shift assignment
    suspend fun updateAssignment(assignmentId: String, assignment: FirebaseShiftAssignment): Result<Unit> {
        return try {
            assignmentCollection.document(assignmentId).set(assignment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete shift assignment
    suspend fun deleteAssignment(assignmentId: String): Result<Unit> {
        return try {
            assignmentCollection.document(assignmentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get employee's shift assignments
    fun getEmployeeShiftAssignments(employeeId: String): Flow<List<FirebaseShiftAssignment>> = callbackFlow {
        val listener = assignmentCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("assignmentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val assignments = snapshot?.toObjects(FirebaseShiftAssignment::class.java) ?: emptyList()
                trySend(assignments)
            }
        awaitClose { listener.remove() }
    }

    // Get shift assignments for a specific date
    fun getShiftAssignmentsForDate(date: Long): Flow<List<FirebaseShiftAssignment>> = callbackFlow {
        val listener = assignmentCollection
            .whereEqualTo("assignmentDate", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val assignments = snapshot?.toObjects(FirebaseShiftAssignment::class.java) ?: emptyList()
                trySend(assignments)
            }
        awaitClose { listener.remove() }
    }

    // Get all shift assignments (admin)
    fun getAllShiftAssignments(): Flow<List<FirebaseShiftAssignment>> = callbackFlow {
        val listener = assignmentCollection
            .orderBy("assignmentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val assignments = snapshot?.toObjects(FirebaseShiftAssignment::class.java) ?: emptyList()
                trySend(assignments)
            }
        awaitClose { listener.remove() }
    }

    // ============= Shift Swap Request Management =============

    // Create swap request
    suspend fun requestShiftSwap(request: FirebaseShiftSwapRequest): Result<String> {
        return try {
            val docRef = swapRequestCollection.add(request).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Old method name for compatibility
    suspend fun createSwapRequest(request: FirebaseShiftSwapRequest): Result<String> {
        return requestShiftSwap(request)
    }

    // Update swap request status
    suspend fun approveSwapRequest(requestId: String, adminRemarks: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "Approved",
                "adminRemarks" to adminRemarks,
                "respondedAt" to System.currentTimeMillis()
            )
            swapRequestCollection.document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rejectSwapRequest(requestId: String, adminRemarks: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "Rejected",
                "adminRemarks" to adminRemarks,
                "respondedAt" to System.currentTimeMillis()
            )
            swapRequestCollection.document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update swap request status (generic)
    suspend fun updateSwapRequestStatus(requestId: String, status: String, reviewedById: String?): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>("status" to status)
            reviewedById?.let { updates["reviewedById"] = it }
            
            swapRequestCollection.document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete swap request
    suspend fun deleteSwapRequest(requestId: String): Result<Unit> {
        return try {
            swapRequestCollection.document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get swap requests by requester
    fun getSwapRequestsByRequester(employeeId: String): Flow<List<FirebaseShiftSwapRequest>> = callbackFlow {
        val listener = swapRequestCollection
            .whereEqualTo("requesterId", employeeId)
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseShiftSwapRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get swap requests for target employee
    fun getSwapRequestsForTarget(employeeId: String): Flow<List<FirebaseShiftSwapRequest>> = callbackFlow {
        val listener = swapRequestCollection
            .whereEqualTo("targetEmployeeId", employeeId)
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseShiftSwapRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get pending swap requests (admin)
    fun getPendingSwapRequests(): Flow<List<FirebaseShiftSwapRequest>> = callbackFlow {
        val listener = swapRequestCollection
            .whereEqualTo("status", "Pending")
            .orderBy("requestDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseShiftSwapRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Get all swap requests (admin)
    fun getAllSwapRequests(): Flow<List<FirebaseShiftSwapRequest>> = callbackFlow {
        val listener = swapRequestCollection
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FirebaseShiftSwapRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Process approved swap request (swap shifts between two employees)
    suspend fun processShiftSwap(swapRequestId: String): Result<Unit> {
        return try {
            // Get the swap request
            val swapRequest = swapRequestCollection.document(swapRequestId).get().await()
                .toObject(FirebaseShiftSwapRequest::class.java) ?: return Result.failure(Exception("Swap request not found"))

            if (swapRequest.status != "Approved") {
                return Result.failure(Exception("Swap request is not approved"))
            }

            // Get assignments by employee and date
            val requesterAssignments = assignmentCollection
                .whereEqualTo("employeeId", swapRequest.requesterId)
                .whereEqualTo("date", swapRequest.requesterShiftDate)
                .get().await()
            
            val targetAssignments = assignmentCollection
                .whereEqualTo("employeeId", swapRequest.targetEmployeeId)
                .whereEqualTo("date", swapRequest.targetShiftDate)
                .get().await()

            if (requesterAssignments.isEmpty || targetAssignments.isEmpty) {
                return Result.failure(Exception("Assignments not found"))
            }

            val requesterDoc = requesterAssignments.documents[0]
            val targetDoc = targetAssignments.documents[0]

            // Swap employee IDs in both assignments
            val batch = db.batch()
            batch.update(requesterDoc.reference, "employeeId", swapRequest.targetEmployeeId)
            batch.update(targetDoc.reference, "employeeId", swapRequest.requesterId)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
