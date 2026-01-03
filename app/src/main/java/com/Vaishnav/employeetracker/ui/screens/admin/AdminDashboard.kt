package com.Vaishnav.employeetracker.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import com.Vaishnav.employeetracker.viewmodel.AdminViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onNavigateToAttendance: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLeave: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPayroll: () -> Unit,
    onNavigateToShiftManagement: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToMessaging: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    adminViewModel: AdminViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var dailyStats by remember { mutableStateOf<AdminViewModel.DailyStats?>(null) }
    var leaveStats by remember { mutableStateOf<AdminViewModel.LeaveStats?>(null) }
    var taskStats by remember { mutableStateOf<AdminViewModel.TaskStats?>(null) }
    var departmentStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var myEmployeesCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Get current admin's Firebase UID
    val currentAdminId = remember { 
        com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager.getInstance().getCurrentUserId() ?: ""
    }
    
    // Validate admin authentication
    if (currentAdminId.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Error: Admin not authenticated",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = onLogout) {
                    Text("Return to Login")
                }
            }
        }
        return
    }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                android.util.Log.d("AdminDashboard", "Loading admin data for: $currentAdminId")
                
                // Pass currentAdminId to all stats methods to show only this admin's data
                dailyStats = adminViewModel.getTodayStats(currentAdminId)
                leaveStats = adminViewModel.getLeaveStats(currentAdminId)
                taskStats = adminViewModel.getTaskStats(currentAdminId)
                departmentStats = adminViewModel.getDepartmentStats(currentAdminId)
                
                // Get count of employees added by this admin - use first() instead of collect
                val employees = com.Vaishnav.employeetracker.data.firebase.FirebaseManager.employeeRepository
                    .getEmployeesByAdminId(currentAdminId)
                    .first()
                myEmployeesCount = employees.size
                android.util.Log.d("AdminDashboard", "Admin has ${employees.size} employees")
                android.util.Log.d("AdminDashboard", "Stats loaded - Daily: $dailyStats, Leave: $leaveStats, Task: $taskStats")
                    
                isLoading = false
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error loading admin data", e)
                android.util.Log.e("AdminDashboard", "Stack trace:", e)
                errorMessage = "Error loading dashboard: ${e.message}\n${e.javaClass.simpleName}"
                isLoading = false
            }
        }
    }
    
    // Show loading state
    if (isLoading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF6366F1)
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }
    
    // Show error state
    if (errorMessage != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF6366F1)
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = onLogout) {
                        Text("Return to Login")
                    }
                }
            }
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", color = Color.White) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, "Profile", tint = Color.White)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1)
                )
            )
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // TrackHR Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(padding.calculateTopPadding()))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Admin Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = DateTimeHelper.formatDateTime(System.currentTimeMillis()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(0.dp))
            
            // Live Attendance Counter
            dailyStats?.let { stats ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Today's Attendance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${stats.presentCount}/${stats.totalEmployees}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Employees Present",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        if (stats.lateCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "${stats.lateCount} Late Arrivals",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // Advanced Features
            Text(
                text = "Advanced Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminActionCard(
                    icon = Icons.Default.Analytics,
                    title = "Analytics",
                    count = null,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAnalytics
                )
                AdminActionCard(
                    icon = Icons.Default.Payment,
                    title = "Payroll",
                    count = null,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPayroll
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminActionCard(
                    icon = Icons.Default.Schedule,
                    title = "Shifts",
                    count = null,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToShiftManagement
                )
                AdminActionCard(
                    icon = Icons.Default.Description,
                    title = "Documents",
                    count = null,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDocuments
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminActionCard(
                    icon = Icons.Default.Message,
                    title = "Messages",
                    count = null,
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMessaging
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminActionCard(
                    icon = Icons.Default.People,
                    title = "Employees",
                    count = myEmployeesCount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToEmployees
                )
                AdminActionCard(
                    icon = Icons.Default.AccessTime,
                    title = "Attendance",
                    count = dailyStats?.presentCount,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAttendance
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminActionCard(
                    icon = Icons.Default.Assignment,
                    title = "Tasks",
                    count = taskStats?.totalTasks,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
                AdminActionCard(
                    icon = Icons.Default.EventNote,
                    title = "Leave",
                    count = leaveStats?.pendingCount,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToLeave
                )
            }
            
            // Stats Overview
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            taskStats?.let { stats ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Task Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        StatProgressRow("Pending", stats.pending, stats.totalTasks.coerceAtLeast(1), MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatProgressRow("In Progress", stats.inProgress, stats.totalTasks.coerceAtLeast(1), Color(0xFFFF9800))
                        Spacer(modifier = Modifier.height(8.dp))
                        StatProgressRow("Completed", stats.completed, stats.totalTasks.coerceAtLeast(1), Color(0xFF4CAF50))
                        
                        if (stats.overdue > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${stats.overdue} Overdue Tasks",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Department Distribution
            if (departmentStats.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Department Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        departmentStats.forEach { (dept, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dept)
                                Text(
                                    text = "$count",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            // Reports Button
            Button(
                onClick = onNavigateToReports,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Reports")
            }
        }
        }
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout from your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminActionCard(
    icon: ImageVector,
    title: String,
    count: Int?,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            count?.let {
                Text(
                    text = "$it",
                    style = MaterialTheme.typography.headlineMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatProgressRow(label: String, value: Int, total: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label)
            Text(
                text = "$value / $total",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = if (total > 0) value.toFloat() / total else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}
