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
            // Admin sees ONLY employee attendance (exclude admin account)
            attendanceRepo.getDailyAttendance(date).map { allAttendance ->
                employeeRepo.getAllEmployeesOnly().first().let { employees ->
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
            // Admin sees ONLY employee late arrivals (exclude admin account)
            attendanceRepo.getLateArrivals(date).map { allLateArrivals ->
                employeeRepo.getAllEmployeesOnly().first().let { employees ->
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
            
            // Admin sees ONLY employees (exclude admin account)
            val employees = employeeRepo.getAllEmployeesOnly().first()
            
            val totalEmployees = employees.size
            val employeeIds = employees.map { it.id }.toSet()
            
            // Get attendance for today - safe retrieval
            val todayAttendance = runCatching {
                attendanceRepo.getDailyAttendance(today).first()
            }.getOrElse { emptyList() }
            
            // Count only present employees
            val presentCount = todayAttendance.count { attendance ->
                employeeIds.contains(attendance.employeeId)
            }
            
            // Count late arrivals
            val lateCount = todayAttendance.count { attendance ->
                employeeIds.contains(attendance.employeeId) && attendance.isLate
            }
            
            // Calculate absent count = total employees - present
            val absentCount = totalEmployees - presentCount
            
            DailyStats(presentCount, totalEmployees, lateCount, absentCount)
        } catch (e: Exception) {
            android.util.Log.e("AdminViewModel", "Error getting today stats", e)
            DailyStats(0, 0, 0, 0)
        }
    }
    
    // Get detailed attendance with employee names for admin dashboard
    suspend fun getTodayAttendanceDetails(adminId: String? = null): AttendanceDetails {
        return try {
            val today = DateTimeHelper.getStartOfDay(DateTimeHelper.getCurrentTimestamp())
            
            // Get ONLY employees (exclude admin account)
            val employees = employeeRepo.getAllEmployeesOnly().first()
            val employeeMap = employees.associateBy { it.id }
            
            // Get today's attendance
            val todayAttendance = runCatching {
                attendanceRepo.getDailyAttendance(today).first()
            }.getOrElse { emptyList() }
            
            // Create sets of employee IDs
            val presentEmployeeIds = todayAttendance.map { it.employeeId }.toSet()
            
            // Categorize employees
            val presentEmployees = mutableListOf<EmployeeAttendanceInfo>()
            val lateEmployees = mutableListOf<EmployeeAttendanceInfo>()
            
            todayAttendance.forEach { attendance ->
                employeeMap[attendance.employeeId]?.let { employee ->
                    val info = EmployeeAttendanceInfo(
                        id = employee.id,
                        name = employee.name,
                        employeeId = employee.employeeId,
                        checkInTime = attendance.checkInTime?.time ?: 0L,
                        isLate = attendance.isLate
                    )
                    if (attendance.isLate) {
                        lateEmployees.add(info)
                    } else {
                        presentEmployees.add(info)
                    }
                }
            }
            
            // Get absent employees (those not in attendance)
            val absentEmployees = employees
                .filter { !presentEmployeeIds.contains(it.id) }
                .map { employee ->
                    EmployeeAttendanceInfo(
                        id = employee.id,
                        name = employee.name,
                        employeeId = employee.employeeId,
                        checkInTime = 0L,
                        isLate = false
                    )
                }
            
            AttendanceDetails(
                presentEmployees = presentEmployees.sortedBy { it.name },
                lateEmployees = lateEmployees.sortedBy { it.name },
                absentEmployees = absentEmployees.sortedBy { it.name }
            )
        } catch (e: Exception) {
            android.util.Log.e("AdminViewModel", "Error getting attendance details", e)
            AttendanceDetails(emptyList(), emptyList(), emptyList())
        }
    }
    
    // Employee Management Stats
    suspend fun getDepartmentStats(adminId: String? = null): Map<String, Int> {
        return try {
            // Admin sees department stats for ONLY employees (exclude admin)
            val employees = employeeRepo.getAllEmployeesOnly().first()
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
            
            // Admin sees leave stats for ONLY employees (exclude admin)
            val employeeIds = employeeRepo.getAllEmployeesOnly().first().map { it.id }.toSet()
            val relevantLeaves = allLeaves.filter { employeeIds.contains(it.employeeId) }
            
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
            
            // Admin sees task stats for ONLY employees (exclude admin)
            val employeeIds = employeeRepo.getAllEmployeesOnly().first().map { it.id }.toSet()
            val relevantTasks = allTasks.filter { employeeIds.contains(it.assignedToId) }
            
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
        val lateCount: Int,
        val absentCount: Int
    )
    
    data class AttendanceDetails(
        val presentEmployees: List<EmployeeAttendanceInfo>,
        val lateEmployees: List<EmployeeAttendanceInfo>,
        val absentEmployees: List<EmployeeAttendanceInfo>
    )
    
    data class EmployeeAttendanceInfo(
        val id: String,
        val name: String,
        val employeeId: String,
        val checkInTime: Long,
        val isLate: Boolean
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
