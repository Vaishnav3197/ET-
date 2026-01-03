package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseAttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val attendanceCollection = db.collection("attendance")

    // Check in
    suspend fun checkIn(attendance: FirebaseAttendance): Result<String> {
        return try {
            val docRef = attendanceCollection.add(attendance).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check out
    suspend fun checkOut(attendanceId: String, checkOutLocation: String, totalHours: Float): Result<Unit> {
        return try {
            attendanceCollection.document(attendanceId).update(
                mapOf(
                    "checkOutTime" to Date(),
                    "checkOutLocation" to checkOutLocation,
                    "totalWorkingHours" to totalHours,
                    "status" to "Checked Out"
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get today's attendance for employee
    fun getTodayAttendance(employeeId: String, todayStart: Long): Flow<FirebaseAttendance?> = callbackFlow {
        val listener = attendanceCollection
            .whereEqualTo("employeeId", employeeId)
            .whereEqualTo("date", todayStart)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val attendance = snapshot?.documents?.firstOrNull()
                    ?.toObject(FirebaseAttendance::class.java)
                trySend(attendance)
            }
        awaitClose { listener.remove() }
    }

    // Get attendance history for employee
    fun getAttendanceHistory(employeeId: String): Flow<List<FirebaseAttendance>> = callbackFlow {
        val listener = attendanceCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebaseAttendance::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get daily attendance for all employees (admin)
    fun getDailyAttendance(date: Long): Flow<List<FirebaseAttendance>> = callbackFlow {
        val listener = attendanceCollection
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebaseAttendance::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get late arrivals for a date
    fun getLateArrivals(date: Long): Flow<List<FirebaseAttendance>> = callbackFlow {
        val listener = attendanceCollection
            .whereEqualTo("date", date)
            .whereEqualTo("isLate", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebaseAttendance::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get present count for a date
    suspend fun getPresentCount(date: Long): Int {
        return try {
            attendanceCollection
                .whereEqualTo("date", date)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }

    // Get monthly stats for employee
    suspend fun getMonthlyStats(employeeId: String, monthStart: Long, monthEnd: Long): MonthlyStats? {
        return try {
            val records = attendanceCollection
                .whereEqualTo("employeeId", employeeId)
                .whereGreaterThanOrEqualTo("date", monthStart)
                .whereLessThanOrEqualTo("date", monthEnd)
                .get()
                .await()
                .toObjects(FirebaseAttendance::class.java)

            if (records.isEmpty()) return null

            val presentDays = records.size
            val lateDays = records.count { it.isLate }
            val totalHours = records.mapNotNull { it.totalWorkingHours }.sum()
            val avgHours = if (presentDays > 0) totalHours / presentDays else 0f

            MonthlyStats(presentDays, lateDays, avgHours)
        } catch (e: Exception) {
            null
        }
    }

    // Get all attendance records (for admin operations)
    fun getAllAttendance(): Flow<List<FirebaseAttendance>> = callbackFlow {
        val listener = attendanceCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebaseAttendance::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Mark attendance (add or update)
    suspend fun markAttendance(attendance: FirebaseAttendance): Result<String> {
        return try {
            if (attendance.id.isEmpty()) {
                // Add new attendance
                val docRef = attendanceCollection.add(attendance).await()
                Result.success(docRef.id)
            } else {
                // Update existing attendance
                attendanceCollection.document(attendance.id).set(attendance).await()
                Result.success(attendance.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get attendance for a period (for analytics)
    fun getAttendanceForPeriod(employeeId: String, startDate: Long, endDate: Long): Flow<List<FirebaseAttendance>> = callbackFlow {
        val listener = attendanceCollection
            .whereEqualTo("employeeId", employeeId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebaseAttendance::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    data class MonthlyStats(
        val presentDays: Int,
        val lateDays: Int,
        val avgWorkingHours: Float
    )
}
