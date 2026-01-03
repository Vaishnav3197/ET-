package com.Vaishnav.employeetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.data.Shift
import com.Vaishnav.employeetracker.data.ShiftAssignment
import com.Vaishnav.employeetracker.data.ShiftSwapRequest
import com.Vaishnav.employeetracker.data.firebase.FirebaseShift
import com.Vaishnav.employeetracker.data.firebase.FirebaseShiftSwapRequest
import com.Vaishnav.employeetracker.viewmodel.ShiftViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementScreen(
    employeeId: String,
    isAdmin: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: ShiftViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val currentAdminId = Firebase.auth.currentUser?.uid
    var selectedTab by remember { mutableStateOf(0) }
    // Treat "all" as empty for admin viewing all records
    val effectiveEmployeeId = if (employeeId == "all") "" else employeeId
    val isViewingAll = effectiveEmployeeId.isEmpty() && isAdmin
    val tabs = if (isViewingAll) 
        listOf("All Shifts", "Swap Requests") 
    else 
        listOf("Calendar", "My Shifts", "Swap Requests")
    
    var shifts by remember { mutableStateOf<List<FirebaseShift>>(emptyList()) }
    var assignments by remember { mutableStateOf<List<ShiftAssignment>>(emptyList()) }
    var swapRequests by remember { mutableStateOf<List<FirebaseShiftSwapRequest>>(emptyList()) }
    var showCreateShiftDialog by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveEmployeeId, isAdmin, currentAdminId) {
        scope.launch {
            try {
                if (isViewingAll) {
                    // Admin viewing all shifts
                    shifts = viewModel.allShifts.first()
                } else if (isAdmin) {
                    shifts = viewModel.allShifts.first()
                }
                
                // Get swap requests
                if (isAdmin) {
                    swapRequests = viewModel.getPendingSwapRequests(currentAdminId).first()
                } else if (effectiveEmployeeId.isNotEmpty()) {
                    // Employee viewing own shifts - only if employeeId is provided
                    swapRequests = viewModel.getEmployeeSwapRequests(effectiveEmployeeId).first()
                }
            } catch (e: Exception) {
                android.util.Log.e("ShiftManagement", "Error loading shifts", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isViewingAll) "All Employee Shifts" else "Shift Management",
                        color = androidx.compose.ui.graphics.Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = androidx.compose.ui.graphics.Color.White)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showCreateShiftDialog = true }) {
                            Icon(Icons.Default.Add, "Create Shift", tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF6366F1)
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
        floatingActionButton = {
            if (!isAdmin && selectedTab == 1) {
                FloatingActionButton(
                    onClick = { /* Request swap */ },
                    containerColor = androidx.compose.ui.graphics.Color(0xFF6366F1)
                ) {
                    Icon(Icons.Default.SwapHoriz, "Swap Shift", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ShiftCalendarView(assignments = assignments)
                1 -> MyShiftsView(
                    assignments = assignments,
                    onRequestSwap = { assignmentId ->
                        // Handle swap request
                    }
                )
                2 -> SwapRequestsView(
                    swapRequests = swapRequests,
                    isAdmin = isAdmin,
                    onApprove = { requestId ->
                        scope.launch {
                            viewModel.approveSwapRequest(requestId, "Approved by admin")
                            if (isAdmin) {
                                swapRequests = viewModel.getPendingSwapRequests(currentAdminId).first()
                            } else {
                                swapRequests = viewModel.getEmployeeSwapRequests(effectiveEmployeeId).first()
                            }
                        }
                    },
                    onReject = { requestId ->
                        scope.launch {
                            // viewModel.approveSwapRequest(requestId, "Rejected by admin") with status Rejected
                            // Not implemented in ViewModel yet
                        }
                    }
                )
            }
        }

        if (showCreateShiftDialog) {
            CreateShiftDialog(
                onDismiss = { showCreateShiftDialog = false },
                onCreate = { name, startTime, endTime ->
                    scope.launch {
                        viewModel.createShift(name, startTime.toString(), endTime.toString(), 15)
                        shifts = viewModel.allShifts.first()
                        showCreateShiftDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun ShiftCalendarView(assignments: List<ShiftAssignment>) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Month Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                Icon(Icons.Default.ArrowBack, "Previous Month")
            }
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                Icon(Icons.Default.ArrowForward, "Next Month")
            }
        }

        // Calendar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Day headers
            items(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Calendar days
            val firstDayOfMonth = selectedMonth.atDay(1).dayOfWeek.value % 7
            val daysInMonth = selectedMonth.lengthOfMonth()

            // Empty cells before month starts
            items(firstDayOfMonth) {
                Box(modifier = Modifier.height(60.dp))
            }

            // Days of month
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val date = selectedMonth.atDay(day)
                val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val hasShift = assignments.any { it.date == dateMillis }
                
                CalendarDayCell(
                    day = day,
                    hasShift = hasShift,
                    isToday = date == LocalDate.now()
                )
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    hasShift: Boolean,
    isToday: Boolean
) {
    Surface(
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth(),
        color = when {
            isToday -> MaterialTheme.colorScheme.primaryContainer
            hasShift -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.toString(),
                    fontSize = 16.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                if (hasShift) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun MyShiftsView(
    assignments: List<ShiftAssignment>,
    onRequestSwap: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (assignments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No shifts assigned",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(assignments) { assignment ->
                ShiftAssignmentCard(
                    assignment = assignment,
                    onSwapClick = { onRequestSwap(assignment.id) }
                )
            }
        }
    }
}

@Composable
fun ShiftAssignmentCard(
    assignment: ShiftAssignment,
    onSwapClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = java.time.Instant.ofEpochMilli(assignment.date)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shift Time",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                AssistChip(
                    onClick = onSwapClick,
                    label = { Text("Swap") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun SwapRequestsView(
    swapRequests: List<FirebaseShiftSwapRequest>,
    isAdmin: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (swapRequests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No swap requests",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(swapRequests) { request ->
                SwapRequestCard(
                    request = request,
                    isAdmin = isAdmin,
                    onApprove = { onApprove(request.id) },
                    onReject = { onReject(request.id) }
                )
            }
        }
    }
}

@Composable
fun SwapRequestCard(
    request: FirebaseShiftSwapRequest,
    isAdmin: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shift Swap Request",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    request.requestedAt?.let { requestTime ->
                        Text(
                            text = "Requested: ${java.time.Instant.ofEpochMilli(requestTime.time)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (request.reason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: ${request.reason}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Surface(
                    color = when (request.status) {
                        "Pending" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        "Approved" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "Rejected" -> Color(0xFFF44336).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = request.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (request.status) {
                            "Pending" -> Color(0xFFFF9800)
                            "Approved" -> Color(0xFF4CAF50)
                            "Rejected" -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (isAdmin && request.status == "Pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject")
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShiftDialog(
    onDismiss: () -> Unit,
    onCreate: (String, LocalTime, LocalTime) -> Unit
) {
    var shiftName by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(17) }
    var endMinute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Shift") },
        text = {
            Column {
                OutlinedTextField(
                    value = shiftName,
                    onValueChange = { shiftName = it },
                    label = { Text("Shift Name") },
                    placeholder = { Text("e.g., Morning Shift") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Start Time", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startHour.toString().padStart(2, '0'),
                        onValueChange = { 
                            it.toIntOrNull()?.let { h -> 
                                if (h in 0..23) startHour = h 
                            }
                        },
                        label = { Text("HH") },
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", modifier = Modifier.padding(top = 16.dp))
                    OutlinedTextField(
                        value = startMinute.toString().padStart(2, '0'),
                        onValueChange = { 
                            it.toIntOrNull()?.let { m -> 
                                if (m in 0..59) startMinute = m 
                            }
                        },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("End Time", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = endHour.toString().padStart(2, '0'),
                        onValueChange = { 
                            it.toIntOrNull()?.let { h -> 
                                if (h in 0..23) endHour = h 
                            }
                        },
                        label = { Text("HH") },
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", modifier = Modifier.padding(top = 16.dp))
                    OutlinedTextField(
                        value = endMinute.toString().padStart(2, '0'),
                        onValueChange = { 
                            it.toIntOrNull()?.let { m -> 
                                if (m in 0..59) endMinute = m 
                            }
                        },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        shiftName,
                        LocalTime.of(startHour, startMinute),
                        LocalTime.of(endHour, endMinute)
                    )
                },
                enabled = shiftName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
