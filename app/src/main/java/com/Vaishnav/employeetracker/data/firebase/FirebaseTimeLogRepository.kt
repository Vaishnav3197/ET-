package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseTimeLogRepository {
    private val db = FirebaseFirestore.getInstance()
    private val timeLogCollection = db.collection("time_logs")
    private val breakRecordCollection = db.collection("break_records")

    // ============= Time Log Management =============

    // Clock in
    suspend fun clockIn(timeLog: FirebaseTimeLog): Result<String> {
        return try {
            val docRef = timeLogCollection.add(timeLog).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Clock out
    suspend fun clockOut(timeLogId: String): Result<Unit> {
        return try {
            timeLogCollection.document(timeLogId).update(
                mapOf(
                    "clockOut" to com.google.firebase.Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update time log notes
    suspend fun updateNotes(timeLogId: String, notes: String): Result<Unit> {
        return try {
            timeLogCollection.document(timeLogId).update("notes", notes).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete time log
    suspend fun deleteTimeLog(timeLogId: String): Result<Unit> {
        return try {
            timeLogCollection.document(timeLogId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get active time log for employee (currently clocked in)
    suspend fun getActiveTimeLog(employeeId: String): FirebaseTimeLog? {
        return try {
            timeLogCollection
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("clockOutTime", null)
                .limit(1)
                .get()
                .await()
                .toObjects(FirebaseTimeLog::class.java)
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // Get time logs for employee
    fun getEmployeeTimeLogs(employeeId: String): Flow<List<FirebaseTimeLog>> = callbackFlow {
        val listener = timeLogCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("clockInTime", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(FirebaseTimeLog::class.java) ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    // Get time logs for date range
    fun getTimeLogsForPeriod(employeeId: String, startDate: Long, endDate: Long): Flow<List<FirebaseTimeLog>> = callbackFlow {
        val listener = timeLogCollection
            .whereEqualTo("employeeId", employeeId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .orderBy("clockInTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(FirebaseTimeLog::class.java) ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    // Get all time logs for a date (admin)
    fun getDailyTimeLogs(date: Long): Flow<List<FirebaseTimeLog>> = callbackFlow {
        val listener = timeLogCollection
            .whereEqualTo("date", date)
            .orderBy("clockInTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(FirebaseTimeLog::class.java) ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    // Calculate total hours for period (using overtime minutes)
    suspend fun getTotalHoursForPeriod(employeeId: String, startDate: Long, endDate: Long): Float {
        return try {
            val logs = timeLogCollection
                .whereEqualTo("employeeId", employeeId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
                .toObjects(FirebaseTimeLog::class.java)

            logs.sumOf { it.overtimeMinutes }.toFloat() / 60
        } catch (e: Exception) {
            0f
        }
    }

    // ============= Break Record Management =============

    // Start break
    suspend fun startBreak(breakRecord: FirebaseBreakRecord): Result<String> {
        return try {
            val docRef = breakRecordCollection.add(breakRecord).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // End break
    suspend fun endBreak(breakId: String): Result<Unit> {
        return try {
            breakRecordCollection.document(breakId).update(
                mapOf(
                    "breakEnd" to com.google.firebase.Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete break record
    suspend fun deleteBreakRecord(breakId: String): Result<Unit> {
        return try {
            breakRecordCollection.document(breakId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get active break for time log
    suspend fun getActiveBreak(timeLogId: String): FirebaseBreakRecord? {
        return try {
            breakRecordCollection
                .whereEqualTo("timeLogId", timeLogId)
                .whereEqualTo("breakEndTime", null)
                .limit(1)
                .get()
                .await()
                .toObjects(FirebaseBreakRecord::class.java)
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // Get break records for time log
    fun getBreaksForTimeLog(timeLogId: String): Flow<List<FirebaseBreakRecord>> = callbackFlow {
        val listener = breakRecordCollection
            .whereEqualTo("timeLogId", timeLogId)
            .orderBy("breakStartTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val breaks = snapshot?.toObjects(FirebaseBreakRecord::class.java) ?: emptyList()
                trySend(breaks)
            }
        awaitClose { listener.remove() }
    }

    // Get total break time for time log (calculated from timestamps)
    suspend fun getTotalBreakTime(timeLogId: String): Int {
        return try {
            val breaks = breakRecordCollection
                .whereEqualTo("timeLogId", timeLogId)
                .get()
                .await()
                .toObjects(FirebaseBreakRecord::class.java)

            breaks.sumOf { 
                val start = it.breakStart?.time ?: 0
                val end = it.breakEnd?.time ?: 0
                if (end > start) ((end - start) / (1000 * 60)).toInt() else 0
            }
        } catch (e: Exception) {
            0
        }
    }

    // Get break records for employee on a date
    fun getDailyBreaks(employeeId: String, date: Long): Flow<List<FirebaseBreakRecord>> = callbackFlow {
        val listener = breakRecordCollection
            .whereEqualTo("employeeId", employeeId)
            .whereEqualTo("date", date)
            .orderBy("breakStart", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val breaks = snapshot?.toObjects(FirebaseBreakRecord::class.java) ?: emptyList()
                trySend(breaks)
            }
        awaitClose { listener.remove() }
    }
}
