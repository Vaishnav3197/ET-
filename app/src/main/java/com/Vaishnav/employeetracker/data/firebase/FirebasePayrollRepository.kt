package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePayrollRepository {
    private val db = FirebaseFirestore.getInstance()
    private val payrollCollection = db.collection("payroll_records")

    // Add payroll record
    suspend fun addPayrollRecord(record: FirebasePayrollRecord): Result<String> {
        return try {
            val docRef = payrollCollection.add(record).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update payroll record
    suspend fun updatePayrollRecord(recordId: String, record: FirebasePayrollRecord): Result<Unit> {
        return try {
            payrollCollection.document(recordId).set(record).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update payment status
    suspend fun updatePaymentStatus(recordId: String, status: String): Result<Unit> {
        return try {
            payrollCollection.document(recordId).update("paymentStatus", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete payroll record
    suspend fun deletePayrollRecord(recordId: String): Result<Unit> {
        return try {
            payrollCollection.document(recordId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get payroll record by ID
    suspend fun getPayrollRecordById(recordId: String): FirebasePayrollRecord? {
        return try {
            payrollCollection.document(recordId).get().await()
                .toObject(FirebasePayrollRecord::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get employee's payroll records
    fun getEmployeePayrollRecords(employeeId: String): Flow<List<FirebasePayrollRecord>> = callbackFlow {
        val listener = payrollCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("payPeriodStart", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebasePayrollRecord::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get payroll records for a specific period
    fun getPayrollRecordsForPeriod(startDate: Long, endDate: Long): Flow<List<FirebasePayrollRecord>> = callbackFlow {
        val listener = payrollCollection
            .whereGreaterThanOrEqualTo("payPeriodStart", startDate)
            .whereLessThanOrEqualTo("payPeriodStart", endDate)
            .orderBy("payPeriodStart", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebasePayrollRecord::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get all payroll records (admin)
    fun getAllPayrollRecords(): Flow<List<FirebasePayrollRecord>> = callbackFlow {
        val listener = payrollCollection
            .orderBy("payPeriodStart", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebasePayrollRecord::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get payroll records by payment status
    fun getPayrollRecordsByStatus(status: String): Flow<List<FirebasePayrollRecord>> = callbackFlow {
        val listener = payrollCollection
            .whereEqualTo("paymentStatus", status)
            .orderBy("payPeriodStart", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebasePayrollRecord::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get pending payments
    fun getPendingPayments(): Flow<List<FirebasePayrollRecord>> = callbackFlow {
        val listener = payrollCollection
            .whereEqualTo("paymentStatus", "Pending")
            .orderBy("payPeriodStart", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FirebasePayrollRecord::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    // Get employee's latest payroll record
    suspend fun getLatestPayrollRecord(employeeId: String): FirebasePayrollRecord? {
        return try {
            payrollCollection
                .whereEqualTo("employeeId", employeeId)
                .orderBy("payPeriodStart", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .toObjects(FirebasePayrollRecord::class.java)
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // Calculate total paid amount for employee
    suspend fun getTotalPaidAmount(employeeId: String): Double {
        return try {
            val records = payrollCollection
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("status", "Paid")
                .get()
                .await()
                .toObjects(FirebasePayrollRecord::class.java)

            records.sumOf { it.netSalary.toDouble() }
        } catch (e: Exception) {
            0.0
        }
    }

    // Calculate total earnings for period
    suspend fun getTotalEarningsForPeriod(employeeId: String, month: Int, year: Int): PayrollSummary? {
        return try {
            val records = payrollCollection
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .get()
                .await()
                .toObjects(FirebasePayrollRecord::class.java)

            if (records.isEmpty()) return null

            val totalGross = records.sumOf { it.grossSalary.toDouble() }
            val totalDeductions = records.sumOf { it.lateDeductions.toDouble() }
            val totalNet = records.sumOf { it.netSalary.toDouble() }
            val totalOvertimeHours = records.sumOf { it.overtimeHours.toDouble() }

            PayrollSummary(
                totalGrossPay = totalGross,
                totalDeductions = totalDeductions,
                totalNetPay = totalNet,
                totalHoursWorked = totalOvertimeHours.toFloat(),
                recordCount = records.size
            )
        } catch (e: Exception) {
            null
        }
    }

    // Get yearly earnings summary
    suspend fun getYearlyEarnings(employeeId: String, year: Int): PayrollSummary? {
        return try {
            // Query all months in the year
            val records = payrollCollection
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("year", year)
                .get()
                .await()
                .toObjects(FirebasePayrollRecord::class.java)

            if (records.isEmpty()) return null

            val totalGross = records.sumOf { it.grossSalary.toDouble() }
            val totalDeductions = records.sumOf { it.lateDeductions.toDouble() }
            val totalNet = records.sumOf { it.netSalary.toDouble() }
            val totalOvertimeHours = records.sumOf { it.overtimeHours.toDouble() }

            PayrollSummary(
                totalGrossPay = totalGross,
                totalDeductions = totalDeductions,
                totalNetPay = totalNet,
                totalHoursWorked = totalOvertimeHours.toFloat(),
                recordCount = records.size
            )
        } catch (e: Exception) {
            null
        }
    }

    // Batch update payment status
    suspend fun batchUpdatePaymentStatus(recordIds: List<String>, status: String): Result<Int> {
        return try {
            val batch = db.batch()
            var count = 0

            recordIds.forEach { recordId ->
                val docRef = payrollCollection.document(recordId)
                batch.update(docRef, "paymentStatus", status)
                count++

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

    // Generate payroll report for department
    suspend fun getDepartmentPayrollSummary(department: String, month: Int, year: Int): DepartmentPayrollSummary? {
        return try {
            // First get all employees in the department
            val employeeIds = db.collection("employees")
                .whereEqualTo("department", department)
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.id }

            if (employeeIds.isEmpty()) return null

            // Get payroll records for all employees in the department
            val records = payrollCollection
                .whereIn("employeeId", employeeIds.take(10)) // Firestore 'in' query limit is 10
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .get()
                .await()
                .toObjects(FirebasePayrollRecord::class.java)

            if (records.isEmpty()) return null

            val totalGross = records.sumOf { it.grossSalary.toDouble() }
            val totalNet = records.sumOf { it.netSalary.toDouble() }
            val employeeCount = records.map { it.employeeId }.distinct().size

            DepartmentPayrollSummary(
                department = department,
                totalGrossPay = totalGross,
                totalNetPay = totalNet,
                employeeCount = employeeCount,
                recordCount = records.size
            )
        } catch (e: Exception) {
            null
        }
    }

    data class PayrollSummary(
        val totalGrossPay: Double,
        val totalDeductions: Double,
        val totalNetPay: Double,
        val totalHoursWorked: Float,
        val recordCount: Int
    )

    data class DepartmentPayrollSummary(
        val department: String,
        val totalGrossPay: Double,
        val totalNetPay: Double,
        val employeeCount: Int,
        val recordCount: Int
    )
}
