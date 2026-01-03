package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*
import java.util.Calendar

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val attendanceRepo = FirebaseManager.attendanceRepository
    private val employeeRepo = FirebaseManager.employeeRepository
    private val performanceRepo = FirebaseManager.performanceRepository
    private val taskRepo = FirebaseManager.taskRepository
    
    // Attendance Analytics
    suspend fun getMonthlyAttendanceTrend(employeeId: Int, month: Int, year: Int): List<Pair<Int, Int>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val startDate = DateTimeHelper.getMonthStart(month, year)
            val endDate = DateTimeHelper.getMonthEnd(month, year)
            
            val attendance = attendanceRepo.getAttendanceForPeriod(employeeId.toString(), startDate, endDate).first()
            
            // Group by day and count present/absent
            val trend = mutableListOf<Pair<Int, Int>>()
            for (day in 1..daysInMonth) {
                val dayAttendance = attendance.count { record ->
                    val recordCalendar = Calendar.getInstance()
                    recordCalendar.timeInMillis = record.date
                    recordCalendar.get(Calendar.DAY_OF_MONTH) == day
                }
                trend.add(Pair(day, dayAttendance))
            }
            trend
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getDepartmentAttendanceStats(date: Long, adminId: String? = null): Map<String, Pair<Int, Int>> {
        return try {
            val employees = if (adminId != null) {
                // Get only admin's employees
                employeeRepo.getEmployeesByAdminId(adminId).first()
            } else {
                // Get all employees (for super admin or testing)
                employeeRepo.getAllActiveEmployees().first()
            }
            val attendance = attendanceRepo.getDailyAttendance(date).first()
            
            val departmentStats = mutableMapOf<String, Pair<Int, Int>>()
            
            employees.groupBy { it.department }.forEach { (department, deptEmployees) ->
                val presentCount = attendance.count { att ->
                    deptEmployees.any { emp -> emp.id == att.employeeId }
                }
                val totalCount = deptEmployees.size
                departmentStats[department] = Pair(presentCount, totalCount)
            }
            
            departmentStats
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    suspend fun getEmployeePerformanceTrend(employeeId: Int): List<Pair<String, Float>> {
        // Performance reviews are optional - return empty list if not implemented
        return emptyList()
    }
    
    suspend fun getTaskCompletionRate(employeeId: Int, month: Int, year: Int): Float {
        return try {
            val startDate = DateTimeHelper.getMonthStart(month, year)
            val endDate = DateTimeHelper.getMonthEnd(month, year)
            
            val tasks = taskRepo.getTasksByEmployee(employeeId.toString()).first()
            val monthTasks = tasks.filter { task ->
                task.createdAt?.time?.let { it in startDate..endDate } ?: false
            }
            
            if (monthTasks.isEmpty()) return 0f
            
            val completedTasks = monthTasks.count { it.status == "Completed" }
            (completedTasks.toFloat() / monthTasks.size) * 100
        } catch (e: Exception) {
            0f
        }
    }
    
    suspend fun getWeeklyWorkHours(employeeId: Int, weekStartDate: Long): List<Pair<String, Float>> {
        return try {
            val weeklyHours = mutableListOf<Pair<String, Float>>()
            val calendar = Calendar.getInstance()
            
            for (day in 0..6) {
                calendar.timeInMillis = weekStartDate
                calendar.add(Calendar.DAY_OF_MONTH, day)
                
                val dayStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val dayEnd = calendar.timeInMillis
                
                val attendance = attendanceRepo.getAttendanceForPeriod(employeeId.toString(), dayStart, dayEnd).first()
                val hours = attendance.sumOf { att ->
                    val checkOut = att.checkOutTime
                    val checkIn = att.checkInTime
                    if (checkOut != null && checkIn != null) {
                        DateTimeHelper.calculateWorkingHours(checkIn.time, checkOut.time).toDouble()
                    } else {
                        0.0
                    }
                }.toFloat()
                
                val dayName = DateTimeHelper.getDayName(dayStart)
                weeklyHours.add(Pair(dayName, hours))
            }
            
            weeklyHours
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getLateArrivalTrend(month: Int, year: Int): Map<Int, Int> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val startDate = DateTimeHelper.getMonthStart(month, year)
            val endDate = DateTimeHelper.getMonthEnd(month, year)
            
            val lateTrend = mutableMapOf<Int, Int>()
            
            for (day in 1..daysInMonth) {
                calendar.timeInMillis = startDate
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val dayStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val dayEnd = calendar.timeInMillis
                
                val dailyAttendance = attendanceRepo.getDailyAttendance(dayStart).first()
                val lateCount = dailyAttendance.count { it.isLate }
                
                lateTrend[day] = lateCount
            }
            
            lateTrend
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
