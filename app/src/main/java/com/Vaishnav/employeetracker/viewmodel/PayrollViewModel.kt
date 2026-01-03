package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebasePayrollRecord
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*

class PayrollViewModel(application: Application) : AndroidViewModel(application) {
    private val payrollRepo = FirebaseManager.payrollRepository
    private val attendanceRepo = FirebaseManager.attendanceRepository
    private val timeLogRepo = FirebaseManager.timeLogRepository
    private val employeeRepo = FirebaseManager.employeeRepository
    
    fun getEmployeePayrolls(employeeId: String): Flow<List<FirebasePayrollRecord>> {
        return flow {
            payrollRepo.getAllPayrollRecords().collect { payrolls ->
                emit(payrolls.filter { it.employeeId == employeeId })
            }
        }
    }
    
    fun getAllPayrollRecords(adminId: String? = null): Flow<List<FirebasePayrollRecord>> {
        return flow {
            if (adminId != null) {
                // Filter by admin's employees
                val employees = employeeRepo.getEmployeesByAdminId(adminId).first()
                val employeeIds = employees.map { it.id }.toSet()
                payrollRepo.getAllPayrollRecords().collect { allPayrolls ->
                    emit(allPayrolls.filter { employeeIds.contains(it.employeeId) })
                }
            } else {
                // Show all payroll records (for super admin or testing)
                payrollRepo.getAllPayrollRecords().collect { emit(it) }
            }
        }
    }
    
    fun getAllPayrollForMonth(month: Int, year: Int, adminId: String? = null): Flow<List<FirebasePayrollRecord>> {
        return flow {
            if (adminId != null) {
                // Filter by admin's employees
                val employees = employeeRepo.getEmployeesByAdminId(adminId).first()
                val employeeIds = employees.map { it.id }.toSet()
                payrollRepo.getAllPayrollRecords().collect { all ->
                    emit(all.filter { it.month == month && it.year == year && employeeIds.contains(it.employeeId) })
                }
            } else {
                payrollRepo.getAllPayrollRecords().collect { all ->
                    emit(all.filter { it.month == month && it.year == year })
                }
            }
        }
    }
    
    fun getPendingPayrolls(adminId: String? = null): Flow<List<FirebasePayrollRecord>> {
        return flow {
            if (adminId != null) {
                // Filter by admin's employees
                val employees = employeeRepo.getEmployeesByAdminId(adminId).first()
                val employeeIds = employees.map { it.id }.toSet()
                payrollRepo.getAllPayrollRecords().collect { all ->
                    emit(all.filter { it.status == "Pending" && employeeIds.contains(it.employeeId) })
                }
            } else {
                payrollRepo.getAllPayrollRecords().collect { all ->
                    emit(all.filter { it.status == "Pending" })
                }
            }
        }
    }
    
    suspend fun generatePayroll(
        employeeId: String,
        month: Int,
        year: Int,
        baseSalary: Float
    ): Result<Long> {
        return try {
            val startDate = DateTimeHelper.getMonthStart(month, year)
            val endDate = DateTimeHelper.getMonthEnd(month, year)
            
            val stats = attendanceRepo.getMonthlyStats(employeeId, startDate, endDate)
            
            val presentDays = stats?.presentDays ?: 0
            val lateDays = stats?.lateDays ?: 0
            
            // Calculate overtime hours from time logs
            val overtimeHours = try {
                timeLogRepo.getTotalHoursForPeriod(employeeId, startDate, endDate)
            } catch (e: Exception) {
                0f
            }
            
            val workingDays = 26
            val perDaySalary = baseSalary / workingDays
            val earnedSalary = perDaySalary * presentDays
            
            val lateDeductions = lateDays * 100f
            val hourlyRate = baseSalary / (workingDays * 8)
            val overtimePay = overtimeHours * hourlyRate * 1.5f
            val bonuses = if (presentDays >= workingDays * 0.95) 1000f else 0f
            
            val grossSalary = earnedSalary + overtimePay + bonuses
            val netSalary = grossSalary - lateDeductions
            
            val payroll = FirebasePayrollRecord(
                id = "",
                employeeId = employeeId,
                month = month,
                year = year,
                baseSalary = baseSalary,
                workingDays = workingDays,
                presentDays = presentDays,
                lateDays = lateDays,
                overtimeHours = overtimeHours,
                overtimePay = overtimePay,
                lateDeductions = lateDeductions,
                bonuses = bonuses,
                grossSalary = grossSalary,
                netSalary = netSalary,
                generatedAt = null // ServerTimestamp handled by Firestore
            )
            
            payrollRepo.addPayrollRecord(payroll)
            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markPayrollAsPaid(payrollId: String): Result<String> {
        return try {
            val payrolls = payrollRepo.getAllPayrollRecords().first()
            val payroll = payrolls.find { it.id == payrollId }
            if (payroll != null) {
                val updated = payroll.copy(
                    status = "Paid",
                    paidAt = DateTimeHelper.getCurrentTimestamp()
                )
                payrollRepo.addPayrollRecord(updated)
                Result.success("Payment marked as paid")
            } else {
                Result.failure(Exception("Payroll record not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTotalPaidSalary(month: Int, year: Int): Float {
        val allPayrolls = payrollRepo.getAllPayrollRecords().first()
        return allPayrolls
            .filter { it.month == month && it.year == year && it.status == "Paid" }
            .sumOf { it.netSalary.toDouble() }
            .toFloat()
    }
}
