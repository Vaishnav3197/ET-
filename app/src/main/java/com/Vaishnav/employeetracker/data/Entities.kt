package com.Vaishnav.employeetracker.data

// Plain Kotlin data classes - No Room
// Firebase is the ONLY database

data class Employee(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val designation: String = "",
    val department: String = "",
    val joiningDate: Long = 0L,
    val profilePhotoUri: String? = null,
    val employeeId: String = "",
    val userId: Int = 0,
    val isActive: Boolean = true
)

data class Attendance(
    val id: Int = 0,
    val employeeId: Int = 0,
    val date: Long = 0L,
    val checkInTime: Long = 0L,
    val checkOutTime: Long? = null,
    val checkInLocation: String = "",
    val checkOutLocation: String? = null,
    val checkInPhotoUri: String? = null,
    val totalWorkingHours: Float? = null,
    val isLate: Boolean = false,
    val status: String = "Checked In"
)

data class Task(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val assignedToId: Int = 0,
    val assignedByAdminId: Int = 0,
    val deadline: Long = 0L,
    val priority: String = "",
    val status: String = "Pending",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class LeaveRequest(
    val id: Int = 0,
    val employeeId: Int = 0,
    val leaveType: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val reason: String = "",
    val status: String = "Pending",
    val requestDate: Long = 0L,
    val approvedByAdminId: Int? = null,
    val approvalDate: Long? = null,
    val adminRemarks: String? = null
)

data class PerformanceRating(
    val id: Int = 0,
    val employeeId: Int = 0,
    val ratedByAdminId: Int = 0,
    val rating: Float = 0f,
    val comments: String = "",
    val month: Int = 0,
    val year: Int = 0,
    val createdAt: Long = 0L
)

data class Notification(
    val id: Int = 0,
    val userId: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val relatedId: Int? = null,
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

data class Shift(
    val id: Int = 0,
    val shiftName: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val graceMinutes: Int = 15,
    val isActive: Boolean = true
)

data class ShiftAssignment(
    val id: Int = 0,
    val employeeId: Int = 0,
    val shiftId: Int = 0,
    val date: Long = 0L,
    val status: String = "Scheduled"
)

data class ShiftSwapRequest(
    val id: Int = 0,
    val requesterId: Int = 0,
    val targetEmployeeId: Int = 0,
    val requesterShiftDate: Long = 0L,
    val targetShiftDate: Long = 0L,
    val reason: String = "",
    val status: String = "Pending",
    val requestedAt: Long = 0L,
    val respondedAt: Long? = null,
    val adminRemarks: String? = null
)

data class TimeLog(
    val id: Int = 0,
    val employeeId: Int = 0,
    val date: Long = 0L,
    val clockIn: Long = 0L,
    val clockOut: Long? = null,
    val breakDuration: Int = 0,
    val overtimeMinutes: Int = 0,
    val location: String = "",
    val notes: String? = null
)

data class BreakRecord(
    val id: Int = 0,
    val timeLogId: Int = 0,
    val breakStart: Long = 0L,
    val breakEnd: Long? = null,
    val breakType: String = "Regular"
)

data class PayrollRecord(
    val id: Int = 0,
    val employeeId: Int = 0,
    val month: Int = 0,
    val year: Int = 0,
    val baseSalary: Float = 0f,
    val workingDays: Int = 0,
    val presentDays: Int = 0,
    val lateDays: Int = 0,
    val overtimeHours: Float = 0f,
    val overtimePay: Float = 0f,
    val lateDeductions: Float = 0f,
    val bonuses: Float = 0f,
    val grossSalary: Float = 0f,
    val netSalary: Float = 0f,
    val generatedAt: Long = 0L,
    val paidAt: Long? = null,
    val status: String = "Pending"
)

data class Document(
    val id: Int = 0,
    val employeeId: Int = 0,
    val documentType: String = "",
    val documentName: String = "",
    val documentUri: String = "",
    val uploadedAt: Long = 0L,
    val uploadedBy: Int = 0,
    val expiryDate: Long? = null,
    val notes: String? = null
)

data class Message(
    val id: Int = 0,
    val senderId: Int = 0,
    val receiverId: Int? = null,
    val groupId: Int? = null,
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val messageType: String = "Text"
)

data class ChatGroup(
    val id: Int = 0,
    val groupName: String = "",
    val groupType: String = "",
    val createdBy: Int = 0,
    val createdAt: Long = 0L,
    val isActive: Boolean = true
)

data class GroupMember(
    val id: Int = 0,
    val groupId: Int = 0,
    val employeeId: Int = 0,
    val joinedAt: Long = 0L,
    val isAdmin: Boolean = false
)

