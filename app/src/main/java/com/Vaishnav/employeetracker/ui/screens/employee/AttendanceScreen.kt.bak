package com.Vaishnav.employeetracker.ui.screens.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.data.Attendance
import com.Vaishnav.employeetracker.data.firebase.FirebaseAttendance
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import com.Vaishnav.employeetracker.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    employeeId: String,
    employeeName: String,
    onNavigateBack: () -> Unit,
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val attendanceStatus by attendanceViewModel.attendanceStatus.collectAsState()
    val todayAttendance by attendanceViewModel.todayAttendance.collectAsState()
    val attendanceHistory by attendanceViewModel.getAttendanceHistory(employeeId).collectAsState(initial = emptyList())
    
    var showMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(employeeId) {
        if (employeeId.isNotEmpty()) {
            try {
                attendanceViewModel.loadTodayAttendance(employeeId)
            } catch (e: Exception) {
                android.util.Log.e("AttendanceScreen", "Error loading attendance", e)
                showMessage = "Error loading attendance: ${e.message}"
            }
        } else {
            showMessage = "Invalid employee ID"
        }
    }
    
    fun handleCheckIn() {
        isLoading = true
        scope.launch {
            // No location required - allow check-in from anywhere
            val result = attendanceViewModel.checkIn(employeeId, employeeName, "Remote", null)
            
            result.onSuccess { message ->
                showMessage = message
                attendanceViewModel.loadTodayAttendance(employeeId)
            }.onFailure { error ->
                showMessage = error.message ?: "Check-in failed"
            }
            isLoading = false
        }
    }
    
    fun handleCheckOut() {
        isLoading = true
        scope.launch {
            // No location required - allow check-out from anywhere
            val result = attendanceViewModel.checkOut(employeeId, "Remote")
            
            result.onSuccess { message ->
                showMessage = message
                attendanceViewModel.loadTodayAttendance(employeeId)
            }.onFailure { error ->
                showMessage = error.message ?: "Check-out failed"
            }
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1)
                )
            )
        },
        containerColor = Color(0xFFF3F4F6),
        snackbarHost = {
            showMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (attendanceStatus) {
                            "Checked In" -> MaterialTheme.colorScheme.primaryContainer
                            "Checked Out" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (attendanceStatus) {
                                "Checked In" -> Icons.Default.CheckCircle
                                "Checked Out" -> Icons.Default.Done
                                else -> Icons.Default.Schedule
                            },
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = when (attendanceStatus) {
                                "Checked In" -> MaterialTheme.colorScheme.primary
                                "Checked Out" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = attendanceStatus,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        todayAttendance?.let { attendance ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Check-in: ${DateTimeHelper.formatTime(attendance.checkInTime?.time ?: 0L)}")
                            attendance.checkOutTime?.let { checkOut ->
                                Text("Check-out: ${DateTimeHelper.formatTime(checkOut.time)}")
                                attendance.totalWorkingHours?.let { hours ->
                                    Text(
                                        text = "Working Hours: ${DateTimeHelper.formatWorkingHours(hours)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                // Action Buttons
                when (attendanceStatus) {
                    "Not Checked In" -> {
                        Button(
                            onClick = { handleCheckIn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check In")
                            }
                        }
                    }
                    "Checked In" -> {
                        Button(
                            onClick = { handleCheckOut() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            } else {
                                Icon(Icons.Default.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check Out")
                            }
                        }
                    }
                    "Checked Out" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("You have completed today's attendance")
                            }
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Attendance History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(attendanceHistory.take(10)) { attendance ->
                AttendanceHistoryCard(attendance)
            }
        }
    }
}

@Composable
fun AttendanceHistoryCard(attendance: FirebaseAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateTimeHelper.formatDate(attendance.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "In: ${DateTimeHelper.formatTime(attendance.checkInTime?.time ?: 0L)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                attendance.checkOutTime?.let {
                    Text(
                        text = "Out: ${DateTimeHelper.formatTime(it.time)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (attendance.isLate) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LATE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                attendance.totalWorkingHours?.let { hours ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateTimeHelper.formatWorkingHours(hours),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
