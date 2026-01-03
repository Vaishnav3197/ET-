package com.Vaishnav.employeetracker.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.viewmodel.AnalyticsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    isAdmin: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val currentUserId = Firebase.auth.currentUser?.uid
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Attendance", "Performance", "Tasks")

    var monthlyTrend by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var deptStats by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    var performanceTrend by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var taskCompletion by remember { mutableStateOf(0f) }
    var weeklyHours by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var lateTrend by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    LaunchedEffect(currentUserId) {
        scope.launch {
            // Get employee ID from auth (for individual analytics, admins can see all)
            val employeeId = 1 // TODO: Get from AuthManager or map Firebase UID to employee
            monthlyTrend = viewModel.getMonthlyAttendanceTrend(employeeId, LocalDate.now().monthValue, LocalDate.now().year)
            deptStats = viewModel.getDepartmentAttendanceStats(System.currentTimeMillis(), if (isAdmin) currentUserId else null)
            // weeklyHours = viewModel.getWeeklyWorkHours(...)
            // lateTrend = viewModel.getLateArrivalTrend(...)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export report */ }) {
                        Icon(Icons.Default.Share, "Export", tint = Color.White)
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
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Overview
                        item {
                            Text(
                                text = "Quick Stats",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    StatCard(
                                        icon = Icons.Default.Check,
                                        title = "Attendance",
                                        value = "${monthlyTrend.count { it.second > 0 }}/${monthlyTrend.size}",
                                        subtitle = "Days Present",
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                item {
                                    StatCard(
                                        icon = Icons.Default.Star,
                                        title = "Performance",
                                        value = "4.2/5.0",
                                        subtitle = "Avg Rating",
                                        color = Color(0xFFFF9800)
                                    )
                                }
                                item {
                                    StatCard(
                                        icon = Icons.Default.Done,
                                        title = "Tasks",
                                        value = "${(taskCompletion * 100).toInt()}%",
                                        subtitle = "Completed",
                                        color = Color(0xFF2196F3)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Department Stats",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(deptStats.entries.toList()) { (dept, stats) ->
                            DepartmentStatCard(dept, stats.first, stats.second)
                        }
                    }

                    1 -> { // Attendance
                        item {
                            Text(
                                text = "Monthly Trend",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            AttendanceChartCard(monthlyTrend)
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Late Arrivals",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            LateArrivalCard(lateTrend)
                        }
                    }

                    2 -> { // Performance
                        item {
                            Text(
                                text = "Performance Metrics",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            PerformanceOverviewCard()
                        }
                    }

                    3 -> { // Tasks
                        item {
                            Text(
                                text = "Task Analytics",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            TaskCompletionCard(taskCompletion)
                        }
                        item {
                            WeeklyWorkHoursCard(weeklyHours)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DepartmentStatCard(dept: String, present: Int, total: Int) {
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
                    text = dept,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$present / $total present",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val percentage = if (total > 0) (present.toFloat() / total * 100).toInt() else 0
            CircularProgressIndicator(
                progress = if (total > 0) present.toFloat() / total else 0f,
                modifier = Modifier.size(60.dp),
                strokeWidth = 6.dp,
                color = when {
                    percentage >= 90 -> Color(0xFF4CAF50)
                    percentage >= 75 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AttendanceChartCard(trend: List<Pair<Int, Int>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Last ${trend.size} Days",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple bar chart visualization
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.takeLast(15).forEach { (dayOfMonth, count) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((count * 20).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (count > 0) MaterialTheme.colorScheme.primary
                                    else Color.Gray.copy(alpha = 0.3f)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayOfMonth.toString(),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LateArrivalCard(trend: List<Pair<Int, Int>>) {
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
                Text(
                    text = "Total Late Days",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${trend.sumOf { it.second }}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = (trend.count { it.second > 0 }.toFloat() / trend.size.coerceAtLeast(1)),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun PerformanceOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            PerformanceMetricRow("Quality", 4.5f)
            Spacer(modifier = Modifier.height(12.dp))
            PerformanceMetricRow("Punctuality", 4.0f)
            Spacer(modifier = Modifier.height(12.dp))
            PerformanceMetricRow("Teamwork", 4.8f)
            Spacer(modifier = Modifier.height(12.dp))
            PerformanceMetricRow("Communication", 4.3f)
        }
    }
}

@Composable
fun PerformanceMetricRow(metric: String, rating: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = metric, fontSize = 14.sp)
            Text(
                text = "$rating / 5.0",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = rating / 5f,
            modifier = Modifier.fillMaxWidth(),
            color = when {
                rating >= 4.5f -> Color(0xFF4CAF50)
                rating >= 3.5f -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
        )
    }
}

@Composable
fun TaskCompletionCard(completion: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(completion * 100).toInt()}%",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tasks Completed",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklyWorkHoursCard(hours: Map<String, Double>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Work Hours",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            hours.forEach { (day, hrs) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day,
                        fontSize = 14.sp,
                        modifier = Modifier.width(80.dp)
                    )
                    LinearProgressIndicator(
                        progress = (hrs / 8).toFloat().coerceAtMost(1f),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = String.format("%.1fh", hrs),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
