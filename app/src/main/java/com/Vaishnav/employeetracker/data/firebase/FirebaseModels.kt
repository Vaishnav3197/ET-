package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Firebase Employee Model
data class FirebaseEmployee(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val designation: String = "",
    val department: String = "",
    val joiningDate: Long = 0L,
    val profilePhotoUri: String? = null,
    val employeeId: String = "", // Generated ID like "EMP001"
    val userId: String = "", // Firebase Auth UID
    val role: String = "USER", // USER or ADMIN
    val addedBy: String = "", // Admin's Firebase Auth UID who added this employee
    val isActive: Boolean = true,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

// Firebase Attendance Model
data class FirebaseAttendance(
    @DocumentId
    val id: String = "",
    val employeeId: String = "",
    val date: Long = 0L, // Date only (midnight timestamp)
    @ServerTimestamp
    val checkInTime: Date? = null,
    val checkOutTime: Date? = null, // Manually set on checkout, NOT auto-timestamp
    val checkInLocation: String = "", // "lat,lng"
    val checkOutLocation: String? = null,
    val checkInPhotoUri: String? = null,
    val totalWorkingHours: Float? = null,
    val isLate: Boolean = false,
    val status: String = "Checked In" // "Checked In", "Checked Out"
)

// Firebase Task Model
data class FirebaseTask(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val assignedToId: String = "",
    val assignedByAdminId: String = "",
    val deadline: Long = 0L,
    val priority: String = "", // "High", "Medium", "Low"
    val status: String = "Pending", // "Pending", "In Progress", "Completed"
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

// Firebase Leave Request Model
data class FirebaseLeaveRequest(
    @DocumentId
    val id: String = "",
    val employeeId: String = "",
    val leaveType: String = "", // "Sick", "Casual", "Emergency"
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val reason: String = "",
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    @ServerTimestamp
    val requestDate: Date? = null,
    val approvedByAdminId: String? = null,
    val approvalDate: Long? = null,
    val adminRemarks: String? = null
)

// Firebase Performance Rating Model
data class FirebasePerformanceRating(
    @DocumentId
    val id: String = "",
    val employeeId: String = "",
    val ratedByAdminId: String = "",
    val rating: Float = 0f, // 1.0 to 5.0
    val comments: String = "",
    val month: Int = 0, // 1-12
    val year: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
)

// Firebase Notification Model
data class FirebaseNotification(
    @DocumentId
    val id: String = "",
    val userId: String = "", // User who receives notification
    val title: String = "",
    val message: String = "",
    val type: String = "", // "Leave", "Task", "Attendance", "Performance"
    val relatedId: String? = null, // ID of related leave/task
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false
)

// Firebase Shift Model
data class FirebaseShift(
    @DocumentId
    val id: String = "",
    val shiftName: String = "", // "Morning", "Evening", "Night"
    val startTime: String = "", // "09:00"
    val endTime: String = "", // "18:00"
    val graceMinutes: Int = 15,
    val isActive: Boolean = true
)

// Firebase Shift Assignment Model
data class FirebaseShiftAssignment(
    @DocumentId
    val id: String = "",
    val employeeId: String = "",
    val shiftId: String = "",
    val date: Long = 0L,
    val status: String = "Scheduled" // "Scheduled", "Completed", "Swap Requested"
)

// Firebase Shift Swap Request Model
data class FirebaseShiftSwapRequest(
    @DocumentId
    val id: String = "",
    val requesterId: String = "",
    val targetEmployeeId: String = "",
    val requesterShiftDate: Long = 0L,
    val targetShiftDate: Long = 0L,
    val reason: String = "",
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    @ServerTimestamp
    val requestedAt: Date? = null,
    val respondedAt: Long? = null,
    val adminRemarks: String? = null
)

// Firebase Time Log Model
data class FirebaseTimeLog(
    @DocumentId
    val id: String = "",
    val employeeId: String = "",
    val date: Long = 0L,
    @ServerTimestamp
    val clockIn: Date? = null,
    @ServerTimestamp
    val clockOut: Date? = null,
    val breakDuration: Int = 0,
    val overtimeMinutes: Int = 0,
    val location: String = "",
    val notes: String? = null
)

// Firebase Break Record Model
data class FirebaseBreakRecord(
    @DocumentId
    val id: String = "",
    val timeLogId: String = "",
    @ServerTimestamp
    val breakStart: Date? = null,
    @ServerTimestamp
    val breakEnd: Date? = null,
    val breakType: String = "Regular" // "Regular", "Lunch", "Tea"
)

// Firebase Payroll Record Model
data class FirebasePayrollRecord(
    @DocumentId
    val id: String = "",
    val employeeId: String = "",
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
    @ServerTimestamp
    val generatedAt: Date? = null,
    val paidAt: Long? = null,
    val status: String = "Pending" // "Pending", "Paid"
)

// Firebase Document Model
data class FirebaseDocument(
    @DocumentId
    val id: String = "",
    val employeeId: String = "",
    val documentType: String = "", // "ID_Proof", "Certificate", "Resume", "Contract"
    val documentName: String = "",
    val documentUri: String = "",
    @ServerTimestamp
    val uploadedAt: Date? = null,
    val uploadedBy: String = "", // Admin ID
    val expiryDate: Long? = null,
    val notes: String? = null
)

// Firebase Message Model
data class FirebaseMessage(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val receiverId: String? = null,
    val groupId: String? = null,
    val message: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false,
    val messageType: String = "Text" // "Text", "Image", "File"
)

// Firebase Chat Group Model
data class FirebaseChatGroup(
    @DocumentId
    val id: String = "",
    val groupName: String = "",
    val groupType: String = "", // "Department", "Team", "Custom"
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val isActive: Boolean = true
)

// Firebase Group Member Model
data class FirebaseGroupMember(
    @DocumentId
    val id: String = "",
    val groupId: String = "",
    val employeeId: String = "",
    @ServerTimestamp
    val joinedAt: Date? = null,
    val isAdmin: Boolean = false
)
