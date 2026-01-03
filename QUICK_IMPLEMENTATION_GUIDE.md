# Quick Implementation Guide
## For Remaining Core Features

---

## 🎯 PRIORITY 1: Smart Attendance System

### Step 1: Create AttendanceViewModel
```kotlin
// File: viewmodel/AttendanceViewModel.kt
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    
    private val _todayAttendance = MutableStateFlow<Attendance?>(null)
    val todayAttendance: StateFlow<Attendance?> = _todayAttendance
    
    private val _attendanceStatus = MutableStateFlow<AttendanceStatus>(AttendanceStatus.NOT_CHECKED_IN)
    val attendanceStatus: StateFlow<AttendanceStatus> = _attendanceStatus
    
    fun checkIn(employeeId: Int, employeeName: String, location: String, photoUri: String?) {
        viewModelScope.launch {
            val currentTime = DateTimeHelper.getCurrentTimestamp()
            val today = DateTimeHelper.getTodayDateOnly()
            val isLate = DateTimeHelper.isLateCheckIn(currentTime)
            
            val attendance = Attendance(
                employeeId = employeeId,
                employeeName = employeeName,
                date = today,
                checkInTime = currentTime,
                checkInLocation = location,
                checkInPhoto = photoUri,
                isLate = isLate,
                status = "Checked In"
            )
            
            attendanceDao.insertAttendance(attendance)
            loadTodayAttendance(employeeId)
        }
    }
    
    fun checkOut(attendanceId: Int, location: String) {
        viewModelScope.launch {
            val attendance = _todayAttendance.value
            if (attendance != null) {
                val checkOutTime = DateTimeHelper.getCurrentTimestamp()
                val workingHours = DateTimeHelper.calculateWorkingHours(
                    attendance.checkInTime, 
                    checkOutTime
                )
                
                val updated = attendance.copy(
                    checkOutTime = checkOutTime,
                    checkOutLocation = location,
                    totalWorkingHours = workingHours,
                    status = "Checked Out"
                )
                
                attendanceDao.updateAttendance(updated)
                loadTodayAttendance(attendance.employeeId)
            }
        }
    }
    
    fun loadTodayAttendance(employeeId: Int) {
        viewModelScope.launch {
            val today = DateTimeHelper.getTodayDateOnly()
            val attendance = attendanceDao.getTodayAttendance(employeeId, today)
            _todayAttendance.value = attendance
            
            _attendanceStatus.value = when {
                attendance == null -> AttendanceStatus.NOT_CHECKED_IN
                attendance.checkOutTime == null -> AttendanceStatus.CHECKED_IN
                else -> AttendanceStatus.CHECKED_OUT
            }
        }
    }
}

enum class AttendanceStatus {
    NOT_CHECKED_IN,
    CHECKED_IN,
    CHECKED_OUT
}
```

### Step 2: Create AttendanceScreen UI
```kotlin
// File: ui/screens/AttendanceScreen.kt
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    employeeId: Int,
    employeeName: String,
    onNavigateBack: () -> Unit
) {
    val attendanceStatus by viewModel.attendanceStatus.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val context = LocalContext.current
    
    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Proceed with check-in
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadTodayAttendance(employeeId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (attendanceStatus) {
                        AttendanceStatus.NOT_CHECKED_IN -> MaterialTheme.colorScheme.surfaceVariant
                        AttendanceStatus.CHECKED_IN -> MaterialTheme.colorScheme.primaryContainer
                        AttendanceStatus.CHECKED_OUT -> MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (attendanceStatus) {
                            AttendanceStatus.NOT_CHECKED_IN -> Icons.Default.TimerOff
                            AttendanceStatus.CHECKED_IN -> Icons.Default.Timer
                            AttendanceStatus.CHECKED_OUT -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = when (attendanceStatus) {
                            AttendanceStatus.NOT_CHECKED_IN -> "Not Checked In"
                            AttendanceStatus.CHECKED_IN -> "Working"
                            AttendanceStatus.CHECKED_OUT -> "Checked Out"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    todayAttendance?.let { attendance ->
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (attendance.checkInTime != null) {
                            Text("Checked in at: ${DateTimeHelper.formatTime(attendance.checkInTime)}")
                        }
                        
                        if (attendance.checkOutTime != null) {
                            Text("Checked out at: ${DateTimeHelper.formatTime(attendance.checkOutTime)}")
                            attendance.totalWorkingHours?.let { hours ->
                                Text("Total: ${DateTimeHelper.formatWorkingHours(hours)}")
                            }
                        }
                        
                        if (attendance.isLate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Late Arrival",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Check-In/Out Button
            when (attendanceStatus) {
                AttendanceStatus.NOT_CHECKED_IN -> {
                    Button(
                        onClick = {
                            if (LocationHelper.hasLocationPermission(context)) {
                                // Handle check-in with GPS
                                handleCheckIn(context, viewModel, employeeId, employeeName)
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Login, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check In")
                    }
                }
                
                AttendanceStatus.CHECKED_IN -> {
                    Button(
                        onClick = {
                            todayAttendance?.let { attendance ->
                                handleCheckOut(context, viewModel, attendance.id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Out")
                    }
                }
                
                AttendanceStatus.CHECKED_OUT -> {
                    Text(
                        text = "You have completed your work for today!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { /* Navigate to history */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View History")
            }
        }
    }
}

private fun handleCheckIn(
    context: Context,
    viewModel: AttendanceViewModel,
    employeeId: Int,
    employeeName: String
) {
    CoroutineScope(Dispatchers.Main).launch {
        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            val isWithinOffice = LocationHelper.isWithinOfficeLocation(
                location.latitude,
                location.longitude
            )
            
            if (isWithinOffice) {
                val locationString = LocationHelper.formatLocation(location)
                viewModel.checkIn(employeeId, employeeName, locationString, null)
                Toast.makeText(context, "✓ Checked in successfully!", Toast.LENGTH_SHORT).show()
            } else {
                val distance = LocationHelper.getDistanceFromOffice(
                    location.latitude,
                    location.longitude
                )
                Toast.makeText(
                    context,
                    "You are ${distance.toInt()}m away from office. Please move closer.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(context, "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun handleCheckOut(
    context: Context,
    viewModel: AttendanceViewModel,
    attendanceId: Int
) {
    CoroutineScope(Dispatchers.Main).launch {
        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            val locationString = LocationHelper.formatLocation(location)
            viewModel.checkOut(attendanceId, locationString)
            Toast.makeText(context, "✓ Checked out successfully!", Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## 🎯 PRIORITY 2: Leave Management

### Step 1: Create LeaveViewModel
```kotlin
// File: viewmodel/LeaveViewModel.kt
class LeaveViewModel(application: Application) : AndroidViewModel(application) {
    private val leaveDao = AppDatabase.getDatabase(application).leaveRequestDao()
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()
    
    val myLeaves = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val pendingLeaves = leaveDao.getPendingLeaveRequests()
    
    fun applyLeave(
        employeeId: Int,
        employeeName: String,
        leaveType: String,
        startDate: Long,
        endDate: Long,
        reason: String
    ) {
        viewModelScope.launch {
            val leave = LeaveRequest(
                employeeId = employeeId,
                employeeName = employeeName,
                leaveType = leaveType,
                startDate = startDate,
                endDate = endDate,
                reason = reason,
                requestDate = System.currentTimeMillis()
            )
            leaveDao.insertLeaveRequest(leave)
        }
    }
    
    fun approveLeave(leaveId: Int, adminId: Int, remarks: String = "") {
        viewModelScope.launch {
            val leave = leaveDao.getLeaveRequestById(leaveId)
            if (leave != null) {
                val updated = leave.copy(
                    status = "Approved",
                    approvedByAdminId = adminId,
                    approvalDate = System.currentTimeMillis(),
                    adminRemarks = remarks
                )
                leaveDao.updateLeaveRequest(updated)
                
                // Send notification
                notificationDao.insertNotification(
                    Notification(
                        userId = leave.employeeId,
                        title = "Leave Approved",
                        message = "Your leave request has been approved.",
                        type = "Leave",
                        timestamp = System.currentTimeMillis(),
                        relatedId = leaveId
                    )
                )
            }
        }
    }
    
    fun rejectLeave(leaveId: Int, adminId: Int, remarks: String) {
        viewModelScope.launch {
            val leave = leaveDao.getLeaveRequestById(leaveId)
            if (leave != null) {
                val updated = leave.copy(
                    status = "Rejected",
                    approvedByAdminId = adminId,
                    approvalDate = System.currentTimeMillis(),
                    adminRemarks = remarks
                )
                leaveDao.updateLeaveRequest(updated)
                
                // Send notification
                notificationDao.insertNotification(
                    Notification(
                        userId = leave.employeeId,
                        title = "Leave Rejected",
                        message = "Your leave request has been rejected. Reason: $remarks",
                        type = "Leave",
                        timestamp = System.currentTimeMillis(),
                        relatedId = leaveId
                    )
                )
            }
        }
    }
}
```

---

## 🎯 PRIORITY 3: Add to Employee Dashboard

### Update DashboardScreen to include Attendance Card
```kotlin
// Add to DashboardScreen.kt

// Attendance Status Card
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onNavigateToAttendance() },
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = "Today's Attendance",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = when (attendanceStatus) {
                    "NOT_CHECKED_IN" -> "Tap to Check In"
                    "CHECKED_IN" -> "Working - Check out later"
                    else -> "Completed for today"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

---

## 🔄 Integration Checklist

### 1. Add ViewModels to MainActivity
```kotlin
private val attendanceViewModel: AttendanceViewModel by viewModels()
private val leaveViewModel: LeaveViewModel by viewModels()
```

### 2. Update NavigationGraph
Add routes for:
- Attendance screen
- Attendance history
- Leave request screen
- Leave approval screen (admin)
- My leaves screen

### 3. Update Dashboard Navigation
Add navigation callbacks for:
- onNavigateToAttendance
- onNavigateToLeaveRequest
- onNavigateToMyLeaves

### 4. Add Admin Dashboard Cards
- Attendance Monitoring
- Leave Approvals (with pending count badge)
- Reports & Export

---

## ⚡ Quick Testing

### Test Attendance:
1. Grant location permission
2. Modify office coordinates in LocationHelper.kt to your current location
3. Click Check In
4. Wait 30 seconds
5. Click Check Out
6. View calculated working hours

### Test Leave:
1. Apply for leave (select dates)
2. View in "My Leaves"
3. Admin approves/rejects
4. Check notification

---

## 📦 Final Build Command

```bash
.\gradlew.bat assembleDebug
```

APK will be at:
```
app\build\outputs\apk\debug\app-debug.apk
```

---

*This guide covers the core implementation. Refer to IMPLEMENTATION_STATUS.md for complete details.*
