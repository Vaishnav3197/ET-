package com.Vaishnav.employeetracker.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.Vaishnav.employeetracker.data.Employee
import com.Vaishnav.employeetracker.data.DepartmentDesignation
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import com.Vaishnav.employeetracker.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDirectoryScreen(
    onNavigateBack: () -> Unit,
    employeeViewModel: EmployeeViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var generatedCredentials by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDepartmentFilter by remember { mutableStateOf(false) }
    
    // Admin sees ALL employees - but NOT admin account itself
    val employees by if (searchQuery.isBlank()) {
        employeeViewModel.getAllEmployeesOnly().collectAsState(initial = emptyList())
    } else {
        employeeViewModel.searchEmployees(searchQuery).collectAsState(initial = emptyList())
    }
    
    // Get current admin's Firebase UID for creating new employees
    val currentAdminId = remember { com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager.getInstance().getCurrentUserId() ?: "" }
    
    // Filter by department
    val filteredEmployees = if (selectedDepartment == "All") {
        employees
    } else {
        employees.filter { it.department == selectedDepartment }
    }
    
    // Group by department and sort
    val groupedEmployees = filteredEmployees
        .groupBy { it.department }
        .mapValues { (_, empList) -> empList.sortedBy { it.designation } }
        .toSortedMap()
    
    val departments = listOf("All") + DepartmentDesignation.getDepartmentNames()
    
    // Debug logging
    LaunchedEffect(employees) {
        android.util.Log.d("EmployeeDirectory", "UI received ${employees.size} employees")
        employees.forEachIndexed { index, emp ->
            android.util.Log.d("EmployeeDirectory", "  Employee $index: ${emp.name} (ID: ${emp.id}, EmpID: ${emp.employeeId})")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Directory", color = androidx.compose.ui.graphics.Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = androidx.compose.ui.graphics.Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showDepartmentFilter = !showDepartmentFilter }) {
                        Icon(
                            Icons.Default.FilterList, 
                            "Filter by Department", 
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, "Clear All Employees", tint = androidx.compose.ui.graphics.Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF6366F1)
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = androidx.compose.ui.graphics.Color(0xFF6366F1)
            ) {
                Icon(Icons.Default.Add, "Add Employee", tint = androidx.compose.ui.graphics.Color.White)
            }
        },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search employees...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Department Filter Chips
            if (showDepartmentFilter) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(departments) { dept ->
                        FilterChip(
                            selected = selectedDepartment == dept,
                            onClick = { selectedDepartment = dept },
                            label = { 
                                Text(
                                    text = if (dept == "All") "All Departments" else dept,
                                    style = MaterialTheme.typography.labelMedium
                                ) 
                            },
                            leadingIcon = if (selectedDepartment == dept) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Employee List
            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No employees found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Display employees grouped by department
                    groupedEmployees.forEach { (department, deptEmployees) ->
                        item {
                            // Department Header
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.BusinessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = department,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${deptEmployees.size} ${if (deptEmployees.size == 1) "employee" else "employees"}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        items(deptEmployees, key = { it.id }) { employee ->
                            EmployeeDirectoryItem(
                                employee = employee,
                                onClick = { selectedEmployee = employee },
                                onDeactivate = {
                                    scope.launch {
                                        val result = employeeViewModel.deactivateEmployee(employee.id)
                                        result.onSuccess { showMessage = it }
                                            .onFailure { showMessage = it.message }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Clear All Confirmation Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Employees?") },
            text = { 
                Text(
                    "This will permanently delete all employee records from the database. This action cannot be undone.\\n\\nAre you sure you want to continue?",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            showClearAllDialog = false
                            val result = employeeViewModel.deleteAllEmployees()
                            result.onSuccess { message ->
                                showMessage = message
                            }.onFailure { error ->
                                showMessage = "Failed to clear employees: ${error.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Red
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showAddDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, email, phone, designation, department, employeeId, joiningDate ->
                scope.launch {
                    try {
                        android.util.Log.d("EmployeeDirectory", "Starting employee creation for: $name ($employeeId)")
                        
                        val result = employeeViewModel.addEmployee(
                            name = name,
                            email = email,
                            phone = phone,
                            designation = designation,
                            department = department,
                            joiningDate = joiningDate,
                            employeeId = employeeId,
                            adminId = currentAdminId
                        )
                        
                        result.onSuccess { credentials ->
                            android.util.Log.d("EmployeeDirectory", "Employee created successfully, credentials generated")
                            showMessage = "Employee saved to Firebase successfully!"
                            generatedCredentials = credentials
                            showAddDialog = false
                            showCredentialsDialog = true
                        }.onFailure { error ->
                            android.util.Log.e("EmployeeDirectory", "Failed to create employee", error)
                            showMessage = "Firebase Error: ${error.message ?: "Failed to add employee"}"
                            showAddDialog = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("EmployeeDirectory", "Exception during employee creation", e)
                        showMessage = "Error: ${e.message ?: "Unknown error occurred"}"
                        showAddDialog = false
                    }
                }
            }
        )
    }
    
    selectedEmployee?.let { employee ->
        EmployeeDetailsDialog(
            employee = employee,
            onDismiss = { selectedEmployee = null },
            onEdit = { updatedEmployee ->
                scope.launch {
                    val result = employeeViewModel.updateEmployee(
                        id = updatedEmployee.id,
                        name = updatedEmployee.name,
                        email = updatedEmployee.email,
                        phone = updatedEmployee.phone,
                        designation = updatedEmployee.designation,
                        department = updatedEmployee.department
                    )
                    
                    result.onSuccess {
                        showMessage = it
                        selectedEmployee = null
                    }.onFailure {
                        showMessage = it.message
                    }
                }
            }
        )
    }
    
    // Show credentials dialog when employee is created
    if (showCredentialsDialog && generatedCredentials != null) {
        EmployeeCredentialsDialog(
            username = generatedCredentials!!.first,
            password = generatedCredentials!!.second,
            onDismiss = {
                showCredentialsDialog = false
                generatedCredentials = null
                showMessage = "Employee added successfully"
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDirectoryItem(
    employee: Employee,
    onClick: () -> Unit,
    onDeactivate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (employee.profilePhotoUri != null) {
                AsyncImage(
                    model = employee.profilePhotoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = employee.designation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = employee.employeeId,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Details") },
                        onClick = {
                            onClick()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Visibility, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Deactivate", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDeactivate()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var joiningDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Department dropdown state
    var departmentExpanded by remember { mutableStateOf(false) }
    var availableDesignations by remember { mutableStateOf<List<String>>(emptyList()) }
    var designationExpanded by remember { mutableStateOf(false) }
    var showValidationError by remember { mutableStateOf(false) }
    
    val departments = DepartmentDesignation.getDepartmentNames()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Employee") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { char -> char.isDigit() }.take(10) },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = phone.isNotEmpty() && phone.length != 10,
                    supportingText = {
                        if (phone.isNotEmpty() && phone.length != 10) {
                            Text("Phone must be 10 digits")
                        }
                    }
                )
                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Employee ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Department Dropdown
                ExposedDropdownMenuBox(
                    expanded = departmentExpanded,
                    onExpandedChange = { departmentExpanded = it }
                ) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false }
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = {
                                    department = dept
                                    departmentExpanded = false
                                    // Update available designations
                                    availableDesignations = DepartmentDesignation.getDesignationsForDepartment(dept)
                                    // Reset designation when department changes
                                    designation = ""
                                    showValidationError = false
                                }
                            )
                        }
                    }
                }
                
                // Designation Dropdown (enabled only if department is selected)
                ExposedDropdownMenuBox(
                    expanded = designationExpanded,
                    onExpandedChange = { designationExpanded = it && department.isNotEmpty() }
                ) {
                    OutlinedTextField(
                        value = designation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Designation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = designationExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = department.isNotEmpty(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        isError = showValidationError,
                        supportingText = {
                            if (showValidationError) {
                                Text("Please select a valid designation for this department")
                            } else if (department.isEmpty()) {
                                Text("Select department first")
                            }
                        }
                    )
                    if (department.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = designationExpanded,
                            onDismissRequest = { designationExpanded = false }
                        ) {
                            availableDesignations.forEach { desig ->
                                DropdownMenuItem(
                                    text = { Text(desig) },
                                    onClick = {
                                        designation = desig
                                        designationExpanded = false
                                        showValidationError = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Date Picker Field
                OutlinedTextField(
                    value = DateTimeHelper.formatDate(joiningDateMillis),
                    onValueChange = { },
                    label = { Text("Joining Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, "Select Date")
                    }
                )
            }
            
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = joiningDateMillis
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    joiningDateMillis = it
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate department-designation combination
                    if (phone.length == 10 && 
                        DepartmentDesignation.isValidCombination(department, designation)) {
                        onAdd(name, email, phone, designation, department, employeeId, joiningDateMillis)
                    } else if (!DepartmentDesignation.isValidCombination(department, designation)) {
                        showValidationError = true
                    }
                },
                enabled = name.isNotEmpty() && email.isNotEmpty() && phone.length == 10 && 
                         department.isNotEmpty() && designation.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmployeeDetailsDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onEdit: (Employee) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Employee Details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailItem("Name", employee.name)
                DetailItem("Employee ID", employee.employeeId)
                DetailItem("Email", employee.email)
                DetailItem("Phone", employee.phone)
                DetailItem("Designation", employee.designation)
                DetailItem("Department", employee.department)
                DetailItem("Joining Date", DateTimeHelper.formatDate(employee.joiningDate))
                DetailItem("Status", if (employee.isActive) "Active" else "Inactive")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmployeeCredentialsDialog(
    username: String,
    password: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Employee Account Created",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Share these credentials with the employee to login:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CredentialRow(label = "Email ID", value = username)
                        HorizontalDivider()
                        CredentialRow(label = "Password", value = password)
                    }
                }
                
                Text(
                    "⚠️ Save these credentials! They won't be shown again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
fun CredentialRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
