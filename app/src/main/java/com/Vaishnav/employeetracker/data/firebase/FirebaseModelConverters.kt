package com.Vaishnav.employeetracker.data.firebase

import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.utils.DateTimeHelper

/**
 * Extension functions to convert Firebase models to Room models
 * This maintains backward compatibility with existing UI code
 */

// FirebaseEmployee to Employee
fun FirebaseEmployee.toRoomEmployee(): Employee {
    return Employee(
        id = this.id.hashCode(),
        name = this.name,
        email = this.email,
        phone = this.phone,
        designation = this.designation,
        department = this.department,
        joiningDate = this.joiningDate,
        profilePhotoUri = this.profilePhotoUri,
        employeeId = this.employeeId,
        userId = this.userId.toIntOrNull() ?: 0,
        isActive = this.isActive
    )
}

// Employee to FirebaseEmployee
fun Employee.toFirebaseEmployee(): FirebaseEmployee {
    return FirebaseEmployee(
        id = this.id.toString(),
        name = this.name,
        email = this.email,
        phone = this.phone,
        designation = this.designation,
        department = this.department,
        joiningDate = this.joiningDate,
        profilePhotoUri = this.profilePhotoUri,
        employeeId = this.employeeId,
        userId = this.userId.toString(),
        isActive = this.isActive
    )
}

// FirebaseTask to Task
fun FirebaseTask.toRoomTask(): Task {
    return Task(
        id = this.id.hashCode(),
        title = this.title,
        description = this.description,
        assignedToId = this.assignedToId.toIntOrNull() ?: 0,
        assignedByAdminId = this.assignedByAdminId.toIntOrNull() ?: 0,
        deadline = this.deadline,
        priority = this.priority,
        status = this.status,
        createdAt = this.createdAt?.time ?: System.currentTimeMillis(),
        updatedAt = this.updatedAt?.time ?: System.currentTimeMillis()
    )
}

// Task to FirebaseTask  
fun Task.toFirebaseTask(): FirebaseTask {
    return FirebaseTask(
        id = this.id.toString(),
        title = this.title,
        description = this.description,
        assignedToId = this.assignedToId.toString(),
        assignedByAdminId = this.assignedByAdminId.toString(),
        deadline = this.deadline,
        priority = this.priority,
        status = this.status
    )
}

// FirebaseAttendance to Attendance
fun FirebaseAttendance.toRoomAttendance(): Attendance {
    return Attendance(
        id = this.id.hashCode(),
        employeeId = this.employeeId.toIntOrNull() ?: 0,
        date = this.date,
        checkInTime = this.checkInTime?.time ?: System.currentTimeMillis(),
        checkOutTime = this.checkOutTime?.time,
        checkInLocation = this.checkInLocation,
        checkOutLocation = this.checkOutLocation,
        checkInPhotoUri = this.checkInPhotoUri,
        totalWorkingHours = this.totalWorkingHours,
        isLate = this.isLate,
        status = this.status
    )
}

// FirebaseLeaveRequest to LeaveRequest
fun FirebaseLeaveRequest.toRoomLeaveRequest(): LeaveRequest {
    return LeaveRequest(
        id = this.id.hashCode(),
        employeeId = this.employeeId.toIntOrNull() ?: 0,
        leaveType = this.leaveType,
        startDate = this.startDate,
        endDate = this.endDate,
        reason = this.reason,
        status = this.status,
        requestDate = this.requestDate?.time ?: System.currentTimeMillis(),
        approvedByAdminId = this.approvedByAdminId?.toIntOrNull(),
        approvalDate = this.approvalDate,
        adminRemarks = this.adminRemarks
    )
}

// FirebaseNotification to Notification
fun FirebaseNotification.toRoomNotification(): Notification {
    return Notification(
        id = this.id.hashCode(),
        userId = this.userId.toIntOrNull() ?: 0,
        title = this.title,
        message = this.message,
        type = this.type,
        relatedId = this.relatedId?.toIntOrNull() ?: 0,
        timestamp = this.timestamp?.time ?: System.currentTimeMillis(),
        isRead = this.isRead
    )
}

// FirebaseShift to Shift
fun FirebaseShift.toRoomShift(): Shift {
    return Shift(
        id = this.id.hashCode(),
        shiftName = this.shiftName,
        startTime = this.startTime,
        endTime = this.endTime,
        graceMinutes = this.graceMinutes
    )
}

// FirebaseShiftAssignment to ShiftAssignment
fun FirebaseShiftAssignment.toRoomShiftAssignment(): ShiftAssignment {
    return ShiftAssignment(
        id = this.id.hashCode(),
        employeeId = this.employeeId.toIntOrNull() ?: 0,
        shiftId = this.shiftId.toIntOrNull() ?: 0,
        date = this.date
    )
}

// FirebaseTimeLog to TimeLog
fun FirebaseTimeLog.toRoomTimeLog(): TimeLog {
    return TimeLog(
        id = this.id.hashCode(),
        employeeId = this.employeeId.toIntOrNull() ?: 0,
        date = this.date,
        clockIn = this.clockIn?.time ?: System.currentTimeMillis(),
        clockOut = this.clockOut?.time,
        breakDuration = this.breakDuration,
        overtimeMinutes = this.overtimeMinutes,
        location = this.location
    )
}

// Note: FirebasePayrollRecord converter not included - use repository methods directly

// FirebaseDocument to Document
fun FirebaseDocument.toRoomDocument(): Document {
    return Document(
        id = this.id.hashCode(),
        employeeId = this.employeeId.toIntOrNull() ?: 0,
        documentType = this.documentType,
        documentName = this.documentName,
        documentUri = this.documentUri,
        uploadedAt = this.uploadedAt?.time ?: System.currentTimeMillis(),
        uploadedBy = this.uploadedBy.toIntOrNull() ?: 0,
        expiryDate = this.expiryDate,
        notes = this.notes
    )
}

// FirebasePerformanceRating to PerformanceRating
fun FirebasePerformanceRating.toRoomPerformanceRating(): PerformanceRating {
    return PerformanceRating(
        id = this.id.hashCode(),
        employeeId = this.employeeId.toIntOrNull() ?: 0,
        ratedByAdminId = this.ratedByAdminId.toIntOrNull() ?: 0,
        rating = this.rating,
        comments = this.comments,
        month = this.month,
        year = this.year,
        createdAt = this.createdAt?.time ?: System.currentTimeMillis()
    )
}

// FirebaseMessage to Message
fun FirebaseMessage.toRoomMessage(): Message {
    return Message(
        id = this.id.hashCode(),
        senderId = this.senderId.toIntOrNull() ?: 0,
        receiverId = this.receiverId?.toIntOrNull(),
        groupId = this.groupId?.toIntOrNull(),
        message = this.message,
        timestamp = this.timestamp?.time ?: System.currentTimeMillis(),
        isRead = this.isRead,
        messageType = this.messageType
    )
}

// FirebaseChatGroup to ChatGroup
fun FirebaseChatGroup.toRoomChatGroup(): ChatGroup {
    return ChatGroup(
        id = this.id.hashCode(),
        groupName = this.groupName,
        groupType = this.groupType,
        createdBy = this.createdBy.toIntOrNull() ?: 0,
        createdAt = this.createdAt?.time ?: System.currentTimeMillis(),
        isActive = this.isActive
    )
}

// FirebaseGroupMember to GroupMember
fun FirebaseGroupMember.toRoomGroupMember(): GroupMember {
    return GroupMember(
        id = this.id.hashCode(),
        groupId = this.groupId.toIntOrNull() ?: 0,
        employeeId = this.employeeId.toIntOrNull() ?: 0,
        joinedAt = this.joinedAt?.time ?: System.currentTimeMillis(),
        isAdmin = this.isAdmin
    )
}
