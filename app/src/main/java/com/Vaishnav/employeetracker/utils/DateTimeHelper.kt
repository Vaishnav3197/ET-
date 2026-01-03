package com.Vaishnav.employeetracker.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateTimeHelper {
    // Office hours configuration
    private const val OFFICE_START_HOUR = 9
    private const val OFFICE_START_MINUTE = 30
    
    /**
     * Get current timestamp
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * Get current date formatted as string
     */
    fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
    
    /**
     * Get start of day (midnight) timestamp
     */
    fun getStartOfDay(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Check if employee checked in late (after 9:30 AM)
     */
    fun isLateCheckIn(checkInTime: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = checkInTime
        
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        return hour > OFFICE_START_HOUR || 
               (hour == OFFICE_START_HOUR && minute > OFFICE_START_MINUTE)
    }
    
    /**
     * Calculate working hours between check-in and check-out
     */
    fun calculateWorkingHours(checkInTime: Long, checkOutTime: Long): Float {
        val diffInMillis = checkOutTime - checkInTime
        val hours = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) / 60.0f
        return maxOf(0f, hours) // Ensure non-negative
    }
    
    /**
     * Format timestamp to time string (HH:mm)
     */
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Format timestamp to date string (dd MMM yyyy)
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Format timestamp to date and time string
     */
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Format working hours to readable string (e.g., "8h 30m")
     */
    fun formatWorkingHours(hours: Float): String {
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "${h}h ${m}m"
    }
    
    /**
     * Get month start timestamp
     */
    fun getMonthStart(month: Int, year: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1) // Calendar months are 0-based
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get month end timestamp
     */
    fun getMonthEnd(month: Int, year: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Get current month and year
     */
    fun getCurrentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }
    
    /**
     * Calculate working days between two dates (excluding weekends)
     */
    fun getWorkingDaysBetween(startDate: Long, endDate: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        
        var workingDays = 0
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endDate
        
        while (calendar.timeInMillis <= endCal.timeInMillis) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                workingDays++
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return workingDays
    }
    
    /**
     * Get month name
     */
    fun getMonthName(month: Int): String {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return if (month in 1..12) months[month - 1] else "Unknown"
    }
    
    /**
     * Format timestamp to month-year string (e.g., "Jan 2024")
     */
    fun formatMonthYear(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Get day name from timestamp (e.g., "Monday")
     */
    fun getDayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
