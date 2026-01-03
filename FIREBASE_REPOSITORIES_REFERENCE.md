# Firebase Repositories - Quick Reference Guide

## Table of Contents
1. [Employee Repository](#employee-repository)
2. [Attendance Repository](#attendance-repository)
3. [Task Repository](#task-repository)
4. [Leave Repository](#leave-repository)
5. [Performance Repository](#performance-repository)
6. [Notification Repository](#notification-repository)
7. [Shift Repository](#shift-repository)
8. [Time Log Repository](#timelog-repository)
9. [Payroll Repository](#payroll-repository)
10. [Document Repository](#document-repository)
11. [Message Repository](#message-repository)

---

## Employee Repository

### Create
```kotlin
val employee = FirebaseEmployee(name = "John Doe", email = "john@example.com", ...)
employeeRepo.addEmployee(employee).onSuccess { employeeId -> }
```

### Read
```kotlin
// Get by ID
val employee = employeeRepo.getEmployeeById(employeeId)

// Get all active
employeeRepo.getAllActiveEmployees().collect { employees -> }

// Search
employeeRepo.searchEmployees("John").collect { employees -> }

// By department
employeeRepo.getEmployeesByDepartment("IT").collect { employees -> }
```

### Update
```kotlin
employeeRepo.updateEmployee(employeeId, updatedEmployee)
```

### Delete
```kotlin
employeeRepo.deleteEmployee(employeeId)
```

---

## Attendance Repository

### Check In
```kotlin
val attendance = FirebaseAttendance(
    employeeId = empId,
    date = todayTimestamp,
    checkInTime = Date(),
    ...
)
attendanceRepo.checkIn(attendance).onSuccess { attendanceId -> }
```

### Check Out
```kotlin
attendanceRepo.checkOut(
    attendanceId = attendanceId,
    checkOutLocation = "Office",
    totalHours = 8.5f
)
```

### Queries
```kotlin
// Today's attendance
attendanceRepo.getTodayAttendance(employeeId, todayStart).collect { attendance -> }

// Attendance history (last 30 days)
attendanceRepo.getAttendanceHistory(employeeId).collect { records -> }

// Daily attendance (admin)
attendanceRepo.getDailyAttendance(date).collect { records -> }

// Late arrivals
attendanceRepo.getLateArrivals(date).collect { records -> }

// Monthly stats
val stats = attendanceRepo.getMonthlyStats(employeeId, monthStart, monthEnd)
```

---

## Task Repository

### Create
```kotlin
val task = FirebaseTask(
    title = "Complete project",
    assignedToId = employeeId,
    createdById = creatorId,
    ...
)
taskRepo.addTask(task).onSuccess { taskId -> }
```

### Update
```kotlin
// Update entire task
taskRepo.updateTask(taskId, updatedTask)

// Update status only
taskRepo.updateTaskStatus(taskId, "Completed")

// Update progress
taskRepo.updateTaskProgress(taskId, 75)
```

### Queries
```kotlin
// Employee tasks
taskRepo.getEmployeeTasks(employeeId).collect { tasks -> }

// By status
taskRepo.getEmployeeTasksByStatus(employeeId, "In Progress").collect { tasks -> }

// Created by
taskRepo.getTasksCreatedBy(creatorId).collect { tasks -> }

// By priority
taskRepo.getTasksByPriority("High").collect { tasks -> }

// Overdue tasks
taskRepo.getOverdueTasks(currentTime).collect { tasks -> }

// Task statistics
val stats = taskRepo.getEmployeeTaskStats(employeeId)
```

---

## Leave Repository

### Submit Request
```kotlin
val leave = FirebaseLeaveRequest(
    employeeId = empId,
    leaveType = "Sick Leave",
    startDate = startTimestamp,
    endDate = endTimestamp,
    ...
)
leaveRepo.submitLeaveRequest(leave).onSuccess { requestId -> }
```

### Approve/Reject
```kotlin
leaveRepo.updateLeaveStatus(
    requestId = requestId,
    status = "Approved",
    reviewedById = adminId,
    comments = "Approved for sick leave"
)
```

### Queries
```kotlin
// Employee's requests
leaveRepo.getEmployeeLeaveRequests(employeeId).collect { requests -> }

// Pending requests (admin)
leaveRepo.getPendingLeaveRequests().collect { requests -> }

// By status
leaveRepo.getLeaveRequestsByStatus("Approved").collect { requests -> }

// By type
leaveRepo.getLeaveRequestsByType("Casual Leave").collect { requests -> }

// Leave balance
val balance = leaveRepo.getLeaveBalance(employeeId, "Sick Leave")

// All balances
val allBalances = leaveRepo.getAllLeaveBalances(employeeId)
```

---

## Performance Repository

### Add Rating
```kotlin
val rating = FirebasePerformanceRating(
    employeeId = empId,
    reviewerId = reviewerId,
    qualityOfWork = 4.5f,
    productivity = 4.0f,
    ...
)
performanceRepo.addRating(rating).onSuccess { ratingId -> }
```

### Queries
```kotlin
// Employee ratings
performanceRepo.getEmployeeRatings(employeeId).collect { ratings -> }

// By reviewer
performanceRepo.getRatingsByReviewer(reviewerId).collect { ratings -> }

// For period
performanceRepo.getRatingsForPeriod(startDate, endDate).collect { ratings -> }

// Average rating
val avgRating = performanceRepo.getAverageRating(employeeId)

// Performance stats
val stats = performanceRepo.getPerformanceStats(employeeId)

// Latest rating
val latest = performanceRepo.getLatestRating(employeeId)
```

---

## Notification Repository

### Send Notification
```kotlin
val notification = FirebaseNotification(
    employeeId = empId,
    title = "New Task Assigned",
    message = "You have a new task",
    type = "TASK",
    ...
)
notificationRepo.sendNotification(notification).onSuccess { notifId -> }
```

### Mark as Read
```kotlin
notificationRepo.markAsRead(notificationId)
notificationRepo.markAllAsRead(employeeId)
```

### Queries
```kotlin
// Employee notifications
notificationRepo.getEmployeeNotifications(employeeId).collect { notifications -> }

// Unread only
notificationRepo.getUnreadNotifications(employeeId).collect { notifications -> }

// Unread count
notificationRepo.getUnreadCount(employeeId).collect { count -> }

// By type
notificationRepo.getNotificationsByType(employeeId, "TASK").collect { notifications -> }
```

### Bulk Operations
```kotlin
// Send to multiple users
notificationRepo.sendBulkNotifications(employeeIds, notification)

// Send to all employees
notificationRepo.sendToAllEmployees(notification)

// Delete old notifications
notificationRepo.deleteOldNotifications(employeeId, daysOld = 30)
```

---

## Shift Repository

### Shift Management
```kotlin
// Create shift
val shift = FirebaseShift(shiftName = "Morning", startTime = "9:00 AM", ...)
shiftRepo.addShift(shift).onSuccess { shiftId -> }

// Get all shifts
shiftRepo.getAllShifts().collect { shifts -> }
```

### Shift Assignment
```kotlin
// Assign shift
val assignment = FirebaseShiftAssignment(
    employeeId = empId,
    shiftId = shiftId,
    assignmentDate = dateTimestamp
)
shiftRepo.assignShift(assignment).onSuccess { assignmentId -> }

// Get employee assignments
shiftRepo.getEmployeeShiftAssignments(employeeId).collect { assignments -> }

// Assignments for date
shiftRepo.getShiftAssignmentsForDate(date).collect { assignments -> }
```

### Shift Swap Requests
```kotlin
// Create swap request
val swapRequest = FirebaseShiftSwapRequest(
    requesterId = empId1,
    targetEmployeeId = empId2,
    requesterShiftAssignmentId = assignmentId1,
    targetShiftAssignmentId = assignmentId2,
    ...
)
shiftRepo.createSwapRequest(swapRequest).onSuccess { requestId -> }

// Approve/reject swap
shiftRepo.updateSwapRequestStatus(requestId, "Approved", reviewerId)

// Process approved swap
shiftRepo.processShiftSwap(swapRequestId)

// Get swap requests
shiftRepo.getSwapRequestsByRequester(employeeId).collect { requests -> }
shiftRepo.getPendingSwapRequests().collect { requests -> }
```

---

## TimeLog Repository

### Clock In/Out
```kotlin
// Clock in
val timeLog = FirebaseTimeLog(
    employeeId = empId,
    date = todayTimestamp,
    clockInTime = Date(),
    ...
)
timeLogRepo.clockIn(timeLog).onSuccess { timeLogId -> }

// Clock out
timeLogRepo.clockOut(timeLogId, Date(), totalHours = 8.5f)
```

### Break Management
```kotlin
// Start break
val breakRecord = FirebaseBreakRecord(
    timeLogId = timeLogId,
    employeeId = empId,
    breakStartTime = Date(),
    ...
)
timeLogRepo.startBreak(breakRecord).onSuccess { breakId -> }

// End break
timeLogRepo.endBreak(breakId, Date(), duration = 30)

// Get breaks
timeLogRepo.getBreaksForTimeLog(timeLogId).collect { breaks -> }
```

### Queries
```kotlin
// Active time log
val activeLog = timeLogRepo.getActiveTimeLog(employeeId)

// Employee time logs
timeLogRepo.getEmployeeTimeLogs(employeeId).collect { logs -> }

// For period
timeLogRepo.getTimeLogsForPeriod(employeeId, startDate, endDate).collect { logs -> }

// Total hours
val totalHours = timeLogRepo.getTotalHoursForPeriod(employeeId, startDate, endDate)
```

---

## Payroll Repository

### Create/Update
```kotlin
val payroll = FirebasePayrollRecord(
    employeeId = empId,
    payPeriodStart = startDate,
    payPeriodEnd = endDate,
    basicSalary = 50000.0,
    ...
)
payrollRepo.addPayrollRecord(payroll).onSuccess { recordId -> }

// Update status
payrollRepo.updatePaymentStatus(recordId, "Paid")
```

### Queries
```kotlin
// Employee records
payrollRepo.getEmployeePayrollRecords(employeeId).collect { records -> }

// For period
payrollRepo.getPayrollRecordsForPeriod(startDate, endDate).collect { records -> }

// By status
payrollRepo.getPayrollRecordsByStatus("Pending").collect { records -> }

// Pending payments
payrollRepo.getPendingPayments().collect { records -> }

// Latest record
val latest = payrollRepo.getLatestPayrollRecord(employeeId)

// Total paid
val totalPaid = payrollRepo.getTotalPaidAmount(employeeId)

// Period summary
val summary = payrollRepo.getTotalEarningsForPeriod(employeeId, startDate, endDate)

// Yearly summary
val yearly = payrollRepo.getYearlyEarnings(employeeId, 2025)

// Department summary
val deptSummary = payrollRepo.getDepartmentPayrollSummary("IT", startDate, endDate)
```

---

## Document Repository

### Upload Document
```kotlin
val document = FirebaseDocument(
    employeeId = empId,
    documentName = "Resume.pdf",
    documentType = "Resume",
    ...
)
documentRepo.uploadDocument(document, localFilePath).onSuccess { documentId -> }
```

### Download Document
```kotlin
documentRepo.downloadDocument(documentId, localPath).onSuccess { filePath -> }

// Get download URL
val url = documentRepo.getDownloadUrl(documentId)
```

### Queries
```kotlin
// Employee documents
documentRepo.getEmployeeDocuments(employeeId).collect { documents -> }

// Uploaded by user
documentRepo.getDocumentsUploadedBy(uploaderId).collect { documents -> }

// By type
documentRepo.getDocumentsByType("Resume").collect { documents -> }

// By category
documentRepo.getDocumentsByCategory("Personal").collect { documents -> }

// Search
documentRepo.searchDocuments("resume").collect { documents -> }

// Document count
val count = documentRepo.getDocumentCount(employeeId)

// Storage used
val storage = documentRepo.getTotalStorageUsed(employeeId)

// Recent documents
documentRepo.getRecentDocuments(limit = 10).collect { documents -> }
```

---

## Message Repository

### Send Message
```kotlin
val message = FirebaseMessage(
    groupId = groupId,
    senderId = senderId,
    content = "Hello team!",
    ...
)
messageRepo.sendMessage(message).onSuccess { messageId -> }

// Update message
messageRepo.updateMessage(messageId, "Updated content")

// Mark as read
messageRepo.markAsRead(messageId)
```

### Chat Group Management
```kotlin
// Create group
val group = FirebaseChatGroup(
    groupName = "Project Team",
    groupType = "PROJECT",
    createdById = creatorId,
    ...
)
val memberIds = listOf(member1Id, member2Id, member3Id)
messageRepo.createChatGroup(group, memberIds).onSuccess { groupId -> }

// Update group
messageRepo.updateChatGroup(groupId, updatedGroup)

// Delete group
messageRepo.deleteChatGroup(groupId)
```

### Group Member Management
```kotlin
// Add member
messageRepo.addGroupMember(groupId, memberId, role = "Member")

// Remove member
messageRepo.removeGroupMember(groupId, memberId)

// Update role
messageRepo.updateMemberRole(groupId, memberId, "Admin")

// Check membership
val isMember = messageRepo.isMember(groupId, memberId)

// Get members
messageRepo.getGroupMembers(groupId).collect { members -> }
```

### Queries
```kotlin
// Group messages
messageRepo.getGroupMessages(groupId).collect { messages -> }

// Unread count
messageRepo.getUnreadMessageCount(groupId, userId).collect { count -> }

// User's groups
messageRepo.getUserChatGroups(userId).collect { groups -> }

// Get group by ID
val group = messageRepo.getChatGroupById(groupId)
```

---

## Common Patterns

### Using Result<T>
```kotlin
repository.addItem(item).onSuccess { id ->
    // Handle success
}.onFailure { error ->
    // Handle error
}
```

### Using Flow for Real-time Updates
```kotlin
repository.getItems().collect { items ->
    // Update UI with new data
}
```

### Error Handling
```kotlin
try {
    val result = repository.addItem(item)
    if (result.isSuccess) {
        // Success
    } else {
        // Failure
    }
} catch (e: Exception) {
    // Handle exception
}
```

---

## Best Practices

1. **Always use suspend functions for write operations**
2. **Collect Flow in lifecycle-aware scope (viewModelScope, lifecycleScope)**
3. **Handle Result.failure cases**
4. **Cancel Flow collection when no longer needed**
5. **Use pagination for large datasets**
6. **Enable offline persistence for better UX**
7. **Create Firestore indexes for complex queries**
8. **Validate data before sending to Firestore**

---

**Document Version:** 1.0  
**Last Updated:** December 10, 2025
