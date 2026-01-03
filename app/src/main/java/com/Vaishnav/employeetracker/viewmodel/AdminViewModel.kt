package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseAttendance
import com.Vaishnav.employeetracker.data.firebase.FirebasePerformanceRating
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val employeeRepo = FirebaseManager.employeeRepository
    private val attendanceRepo = FirebaseManager.attendanceRepository
    private val taskRepo = FirebaseManager.taskRepository
    private val leaveRepo = FirebaseManager.leaveRepository
    private val performanceRepo = FirebaseManager.performanceRepository
    private val context = application
    
    // Attendance Monitoring
    fun getDailyAttendance(date: Long, adminId: String? = null): Flow<List<FirebaseAttendance>> {
        return if (adminId != null) {
            // Filter by admin's employees with continuous observation
            attendanceRepo.getDailyAttendance(date).map { allAttendance ->
                employeeRepo.getEmployeesByAdminId(adminId).first().let { employees ->
                    val employeeIds = employees.map { it.id }.toSet()
                    allAttendance.filter { employeeIds.contains(it.employeeId) }
                }
            }
        } else {
            // Show all attendance
            attendanceRepo.getDailyAttendance(date)
        }
    }
    
    fun getLateArrivals(date: Long, adminId: String? = null): Flow<List<FirebaseAttendance>> {
        return if (adminId != null) {
            // Filter by admin's employees with continuous observation
            attendanceRepo.getLateArrivals(date).map { allLateArrivals ->
                employeeRepo.getEmployeesByAdminId(adminId).first().let { employees ->
                    val employeeIds = employees.map { it.id }.toSet()
                    allLateArrivals.filter { employeeIds.contains(it.employeeId) }
                }
            }
        } else {
            // Show all late arrivals
            attendanceRepo.getLateArrivals(date)
        }
    }
    
    suspend fun getPresentCount(date: Long): Int {
        return attendanceRepo.getPresentCount(date)
    }
    
    suspend fun getTodayStats(adminId: String? = null): DailyStats {
        return try {
            val today = DateTimeHelper.getStartOfDay(DateTimeHelper.getCurrentTimestamp())
            
            // Get employees for this admin (or all if adminId is null)
            val employees = if (adminId != null && adminId.isNotEmpty()) {
                employeeRepo.getEmployeesByAdminId(adminId).first()
            } else {
                employeeRepo.getAllActiveEmployees().first()
            }
            
            val totalEmployees = employees.size
            val employeeIds = employees.map { it.id }.toSet()
            
            // Get attendance for today - safe retrieval
            val todayAttendance = runCatching {
                attendanceRepo.getDailyAttendance(today).first()
            }.getOrElse { emptyList() }
            
            // Count only present employees that belong to this admin
            val presentCount = todayAttendance.count { attendance ->
                employeeIds.contains(attendance.employeeId)
            }
            
            // Count late arrivals for this admin's employees
            val lateCount = todayAttendance.count { attendance ->
                employeeIds.contains(attendance.employeeId) && attendance.isLate
            }
            
            DailyStats(presentCount, totalEmployees, lateCount)
        } catch (e: Exception) {
            android.util.Log.e("AdminViewModel", "Error getting today stats", e)
            DailyStats(0, 0, 0)
        }
    }
    
    // Employee Management Stats
    suspend fun getDepartmentStats(adminId: String? = null): Map<String, Int> {
        return try {
            val employees = if (adminId != null && adminId.isNotEmpty()) {
                employeeRepo.getEmployeesByAdminId(adminId).first()
            } else {
                employeeRepo.getAllActiveEmployees().first()
            }
            employees.groupBy { it.department }.mapValues { it.value.size }
        } catch (e: Exception) {
            android.util.Log.e("AdminViewModel", "Error getting department stats", e)
            emptyMap()
        }
    }
    
    // Leave Management
    suspend fun getLeaveStats(adminId: String? = null): LeaveStats {
        return try {
            val allLeaves = leaveRepo.getAllLeaveRequests().first()
            
            // Filter leaves by admin's employees if adminId is provided
            val relevantLeaves = if (adminId != null && adminId.isNotEmpty()) {
                val employeeIds = employeeRepo.getEmployeesByAdminId(adminId).first().map { it.id }.toSet()
                allLeaves.filter { employeeIds.contains(it.employeeId) }
            } else {
                allLeaves
            }
            
            val pendingCount = relevantLeaves.count { it.status == "Pending" }
            val approvedCount = relevantLeaves.count { it.status == "Approved" }
            
            LeaveStats(pendingCount, approvedCount)
        } catch (e: Exception) {
            android.util.Log.e("AdminViewModel", "Error getting leave stats", e)
            LeaveStats(0, 0)
        }
    }
    
    // Task Management
    suspend fun getTaskStats(adminId: String? = null): TaskStats {
        return try {
            val allTasks = taskRepo.getAllTasks().first()
            
            // Filter tasks by admin's employees if adminId is provided
            val relevantTasks = if (adminId != null && adminId.isNotEmpty()) {
                val employeeIds = employeeRepo.getEmployeesByAdminId(adminId).first().map { it.id }.toSet()
                allTasks.filter { employeeIds.contains(it.assignedToId) }
            } else {
                allTasks
            }
            
            val pending = relevantTasks.count { it.status == "Pending" }
            val inProgress = relevantTasks.count { it.status == "In Progress" }
            val completed = relevantTasks.count { it.status == "Completed" }
            val now = DateTimeHelper.getCurrentTimestamp()
            val overdue = relevantTasks.count { 
                it.deadline > 0 && it.deadline < now && it.status != "Completed" 
            }
            
            TaskStats(
                totalTasks = relevantTasks.size,
                pending = pending,
                inProgress = inProgress,
                completed = completed,
                overdue = overdue
            )
        } catch (e: Exception) {
            android.util.Log.e("AdminViewModel", "Error getting task stats", e)
            TaskStats(0, 0, 0, 0, 0)
        }
    }
    
    // Performance Ratings
    suspend fun addPerformanceRating(
        employeeId: Int,
        ratedByAdminId: Int,
        rating: Float,
        comments: String,
        month: Int,
        year: Int
    ): Result<Long> {
        return try {
            if (rating < 1 || rating > 5) {
                return Result.failure(Exception("Rating must be between 1 and 5"))
            }
            
            val performanceRating = FirebasePerformanceRating(
                id = "",
                employeeId = employeeId.toString(),
                ratedByAdminId = ratedByAdminId.toString(),
                rating = rating,
                comments = comments,
                month = month,
                year = year,
                createdAt = null // ServerTimestamp
            )
            
            performanceRepo.addPerformanceRating(performanceRating)
            Result.success(1L) // Firebase doesn't return ID, use 1 for success
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getEmployeeRatings(employeeId: Int): List<FirebasePerformanceRating> {
        return performanceRepo.getEmployeeRatings(employeeId.toString()).first()
    }
    
    suspend fun getAverageRating(employeeId: Int): Float {
        val ratings = performanceRepo.getEmployeeRatings(employeeId.toString()).first()
        return if (ratings.isNotEmpty()) {
            ratings.map { it.rating }.average().toFloat()
        } else {
            0f
        }
    }
    
    // Monthly Reports Export
    suspend fun exportMonthlyAttendanceReport(month: Int, year: Int): Result<File> {
        return try {
            val startDate = DateTimeHelper.getMonthStart(month, year)
            val endDate = DateTimeHelper.getMonthEnd(month, year)
            
            val employees = employeeRepo.getAllActiveEmployees().first()
            val allAttendance = attendanceRepo.getAllAttendance().first()
            val csvContent = StringBuilder()
            
            // CSV Header
            csvContent.append("Employee ID,Name,Department,Days Present,Late Days,Avg Working Hours,Attendance %\n")
            
            for (employee in employees) {
                val attendance = allAttendance.filter { 
                    it.employeeId == employee.id && it.date in startDate..endDate 
                }
                val daysPresent = attendance.size
                val lateDays = attendance.count { it.isLate }
                val avgHours = if (attendance.isNotEmpty()) {
                    attendance.mapNotNull { it.totalWorkingHours }.average().toFloat()
                } else {
                    0f
                }
                val workingDays = DateTimeHelper.getWorkingDaysBetween(startDate, endDate)
                val attendancePercent = if (workingDays > 0) (daysPresent.toFloat() / workingDays) * 100 else 0f
                
                csvContent.append("${employee.employeeId},${employee.name},${employee.department},$daysPresent,$lateDays,")
                csvContent.append("${String.format("%.2f", avgHours)},${String.format("%.2f", attendancePercent)}%\n")
            }
            
            // Save to file
            val fileName = "attendance_report_${DateTimeHelper.getMonthName(month)}_$year.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            FileWriter(file).use { it.write(csvContent.toString()) }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportEmployeeReport(): Result<File> {
        return try {
            val employees = employeeRepo.getAllActiveEmployees().first()
            val csvContent = StringBuilder()
            
            // CSV Header
            csvContent.append("Employee ID,Name,Email,Phone,Department,Designation,Joining Date,Status\n")
            
            for (employee in employees) {
                csvContent.append("${employee.employeeId},${employee.name},${employee.email},${employee.phone},")
                csvContent.append("${employee.department},${employee.designation},")
                csvContent.append("${DateTimeHelper.formatDate(employee.joiningDate)},")
                csvContent.append("${if (employee.isActive) "Active" else "Inactive"}\n")
            }
            
            val fileName = "employee_directory_${DateTimeHelper.formatDate(DateTimeHelper.getCurrentTimestamp())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            FileWriter(file).use { it.write(csvContent.toString()) }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class DailyStats(
        val presentCount: Int,
        val totalEmployees: Int,
        val lateCount: Int
    )
    
    data class LeaveStats(
        val pendingCount: Int,
        val totalRequests: Int
    )
    
    data class TaskStats(
        val totalTasks: Int,
        val pending: Int,
        val inProgress: Int,
        val completed: Int,
        val overdue: Int
    )
}
