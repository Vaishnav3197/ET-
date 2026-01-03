package com.Vaishnav.employeetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.data.firebase.FirebasePayrollRecord
import com.Vaishnav.employeetracker.viewmodel.PayrollViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollScreen(
    employeeId: String,
    isAdmin: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: PayrollViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val currentAdminId = Firebase.auth.currentUser?.uid
    var payrollRecords by remember { mutableStateOf<List<FirebasePayrollRecord>>(emptyList()) }
    var selectedRecord by remember { mutableStateOf<FirebasePayrollRecord?>(null) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    // Treat "all" as empty for admin viewing all records
    val effectiveEmployeeId = if (employeeId == "all") "" else employeeId
    val isViewingAll = effectiveEmployeeId.isEmpty() && isAdmin

    LaunchedEffect(effectiveEmployeeId, isAdmin, currentAdminId) {
        scope.launch {
            try {
                if (isViewingAll) {
                    // Admin viewing all employees - get payroll records filtered by admin's employees
                    payrollRecords = viewModel.getAllPayrollRecords(currentAdminId).first()
                } else if (effectiveEmployeeId.isNotEmpty()) {
                    // Specific employee view - only if employeeId is provided
                    payrollRecords = viewModel.getEmployeePayrolls(effectiveEmployeeId).first()
                } else {
                    // Empty employeeId for non-admin - show empty list
                    payrollRecords = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("PayrollScreen", "Error loading payroll records", e)
                payrollRecords = emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isViewingAll) "All Payroll Records" else "Payroll Management",
                        color = androidx.compose.ui.graphics.Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = androidx.compose.ui.graphics.Color.White)
                    }
                },
                actions = {
                    if (isAdmin && !isViewingAll) {
                        IconButton(onClick = { showGenerateDialog = true }) {
                            Icon(Icons.Default.Add, "Generate Payroll", tint = androidx.compose.ui.graphics.Color.White)
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
            if (isAdmin && !isViewingAll) {
                FloatingActionButton(
                    onClick = { showGenerateDialog = true }
                ) {
                    Icon(Icons.Default.Add, "Generate")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Current Month",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val currentMonth = payrollRecords.firstOrNull { 
                        it.month == LocalDate.now().monthValue && 
                        it.year == LocalDate.now().year 
                    }
                    
                    Text(
                        text = "₹${currentMonth?.netSalary?.toInt() ?: 0}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentMonth?.status == "Paid") "Paid" else "Pending",
                        fontSize = 14.sp,
                        color = if (currentMonth?.status == "Paid") 
                            Color(0xFF4CAF50) 
                        else 
                            Color(0xFFFF9800)
                    )
                }
            }

            // Payroll History
            Text(
                text = if (isViewingAll) "All Employee Payrolls" else "Payroll History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(payrollRecords) { record ->
                    PayrollRecordCard(
                        record = record,
                        showEmployeeInfo = isViewingAll,
                        onClick = { selectedRecord = record }
                    )
                }
            }
        }

        // Generate Payroll Dialog
        if (showGenerateDialog) {
            GeneratePayrollDialog(
                onDismiss = { showGenerateDialog = false },
                onGenerate = { month, year ->
                    scope.launch {
                        isGenerating = true
                        viewModel.generatePayroll(effectiveEmployeeId, month, year, 50000f)
                        viewModel.getEmployeePayrolls(effectiveEmployeeId).collect { records ->
                            payrollRecords = records
                        }
                        isGenerating = false
                        showGenerateDialog = false
                    }
                },
                isGenerating = isGenerating
            )
        }

        // Payroll Details Dialog
        selectedRecord?.let { record ->
            PayrollDetailsDialog(
                record = record,
                onDismiss = { selectedRecord = null },
                onMarkPaid = {
                    if (isAdmin) {
                        scope.launch {
                            viewModel.markPayrollAsPaid(record.id)
                            // Refresh the list after marking as paid
                            if (isViewingAll) {
                                viewModel.getAllPayrollRecords(currentAdminId).collect { records ->
                                    payrollRecords = records
                                }
                            } else {
                                viewModel.getEmployeePayrolls(effectiveEmployeeId).collect { records ->
                                    payrollRecords = records
                                }
                            }
                            selectedRecord = null
                        }
                    }
                },
                isAdmin = isAdmin
            )
        }
    }
}

@Composable
fun PayrollRecordCard(
    record: FirebasePayrollRecord,
    showEmployeeInfo: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (showEmployeeInfo) {
                    Text(
                        text = "Employee ID: ${record.employeeId}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = "${getMonthName(record.month)} ${record.year}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Net Salary: ₹${record.netSalary}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (record.status == "Paid") Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (record.status == "Paid") Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (record.status == "Paid") {
                            record.paidAt?.let { "Paid on ${java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}" } ?: "Paid"
                        } else "Pending",
                        fontSize = 12.sp,
                        color = if (record.status == "Paid") Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollDetailsDialog(
    record: FirebasePayrollRecord,
    onDismiss: () -> Unit,
    onMarkPaid: () -> Unit,
    isAdmin: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Payroll Details - ${getMonthName(record.month)} ${record.year}")
        },
        text = {
            Column {
                PayrollDetailRow("Base Salary", "₹${record.baseSalary}")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                PayrollDetailRow("Days Worked", "${record.workingDays} days")
                PayrollDetailRow("Days Present", "${record.presentDays} days")
                PayrollDetailRow("Gross Salary", "₹${record.grossSalary}", isHighlight = true)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                PayrollDetailRow("Overtime Pay", "+ ₹${record.overtimePay.toInt()}", Color(0xFF4CAF50))
                PayrollDetailRow("Bonus", "+ ₹${record.bonuses.toInt()}", Color(0xFF4CAF50))
                PayrollDetailRow("Deductions", "- ₹${record.lateDeductions.toInt()}", Color(0xFFF44336))
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                PayrollDetailRow("Net Salary", "₹${record.netSalary}", isHighlight = true, isBold = true)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (record.status == "Paid") 
                        Color(0xFF4CAF50).copy(alpha = 0.1f) 
                    else 
                        Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (record.status == "Paid") Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (record.status == "Paid") Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (record.status == "Paid") {
                                record.paidAt?.let { "Paid on ${java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}" } ?: "Paid"
                            } else 
                                "Payment Pending",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isAdmin && record.status != "Paid") {
                Button(onClick = onMarkPaid) {
                    Text("Mark as Paid")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PayrollDetailRow(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isHighlight: Boolean = false,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratePayrollDialog(
    onDismiss: () -> Unit,
    onGenerate: (Int, Int) -> Unit,
    isGenerating: Boolean
) {
    var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var selectedYear by remember { mutableStateOf(LocalDate.now().year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Payroll") },
        text = {
            Column {
                Text("Select month and year to generate payroll")
                Spacer(modifier = Modifier.height(16.dp))
                
                // Month Selector
                var expandedMonth by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedMonth,
                    onExpandedChange = { expandedMonth = it }
                ) {
                    OutlinedTextField(
                        value = getMonthName(selectedMonth),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false }
                    ) {
                        (1..12).forEach { month ->
                            DropdownMenuItem(
                                text = { Text(getMonthName(month)) },
                                onClick = {
                                    selectedMonth = month
                                    expandedMonth = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Year Selector
                var expandedYear by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedYear,
                    onExpandedChange = { expandedYear = it }
                ) {
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        (2020..2030).forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    selectedYear = year
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(selectedMonth, selectedYear) },
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isGenerating) "Generating..." else "Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) {
                Text("Cancel")
            }
        }
    )
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Unknown"
    }
}
