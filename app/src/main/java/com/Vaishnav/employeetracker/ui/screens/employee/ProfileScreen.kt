package com.Vaishnav.employeetracker.ui.screens.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Vaishnav.employeetracker.data.firebase.FirebaseEmployee
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val firebaseAuthManager = remember { FirebaseAuthManager.getInstance() }
    val firebaseUserId = firebaseAuthManager.getCurrentUserId()
    val coroutineScope = rememberCoroutineScope()
    
    var employee by remember { mutableStateOf<FirebaseEmployee?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    // Editable fields
    var editedName by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    
    // Load employee data
    LaunchedEffect(firebaseUserId) {
        isLoading = true
        errorMessage = null
        
        if (firebaseUserId == null) {
            errorMessage = "User not logged in"
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
            if (employee == null) {
                errorMessage = "Employee profile not found"
            } else {
                // Initialize editable fields
                editedName = employee!!.name
                editedPhone = employee!!.phone
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load profile: ${e.message}"
            android.util.Log.e("ProfileScreen", "Error loading employee", e)
        } finally {
            isLoading = false
        }
    }
    
    // Save profile changes
    fun saveProfile() {
        if (employee == null) return
        
        coroutineScope.launch {
            isSaving = true
            errorMessage = null
            
            try {
                // Validate inputs
                if (editedName.isBlank()) {
                    errorMessage = "Name cannot be empty"
                    isSaving = false
                    return@launch
                }
                
                if (editedPhone.isNotBlank() && !editedPhone.matches(Regex("^[0-9]{10,15}$"))) {
                    errorMessage = "Please enter a valid phone number (10-15 digits)"
                    isSaving = false
                    return@launch
                }
                
                // Update employee with new data
                val updatedEmployee = employee!!.copy(
                    name = editedName.trim(),
                    phone = editedPhone.trim()
                )
                
                val result = FirebaseManager.employeeRepository.updateEmployee(
                    employee!!.id, 
                    updatedEmployee
                )
                
                if (result.isSuccess) {
                    employee = updatedEmployee
                    isEditMode = false
                    showSuccessMessage = true
                } else {
                    errorMessage = "Failed to save changes: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                errorMessage = "Error saving profile: ${e.message}"
                android.util.Log.e("ProfileScreen", "Error saving", e)
            } finally {
                isSaving = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Profile" else "My Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            // Cancel edit mode
                            isEditMode = false
                            // Reset fields
                            employee?.let {
                                editedName = it.name
                                editedPhone = it.phone
                            }
                            errorMessage = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isLoading && employee != null && !isEditMode) {
                        // Edit button
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White
                            )
                        }
                    }
                    if (isEditMode) {
                        // Save button
                        IconButton(
                            onClick = { saveProfile() },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            if (showSuccessMessage) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showSuccessMessage = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text("Profile updated successfully")
                }
            }
            if (errorMessage != null && !isLoading) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage ?: "")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading profile...")
                        }
                    }
                }
                errorMessage != null -> {
                    // Error State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                employee != null -> {
                    // Success State - Show Profile
                    ProfileContent(
                        employee = employee!!,
                        isEditMode = isEditMode,
                        editedName = editedName,
                        editedPhone = editedPhone,
                        onNameChange = { editedName = it },
                        onPhoneChange = { editedPhone = it },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    employee: FirebaseEmployee,
    isEditMode: Boolean,
    editedName: String,
    editedPhone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Header with Avatar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Employee ID
                Text(
                    text = employee.employeeId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Role Badge
                Surface(
                    color = if (employee.role == "ADMIN") {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = employee.role,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Personal Information
        SectionCard(title = "Personal Information") {
            // Name field (editable)
            if (isEditMode) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                ProfileInfoItem(
                    icon = Icons.Default.Person,
                    label = "Name",
                    value = employee.name
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Email (never editable)
            ProfileInfoItem(
                icon = Icons.Default.Email,
                label = "Email",
                value = employee.email
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Phone field (editable)
            if (isEditMode) {
                OutlinedTextField(
                    value = editedPhone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            } else {
                ProfileInfoItem(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = employee.phone.ifEmpty { "Not provided" }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Work Information
        SectionCard(title = "Work Information") {
            ProfileInfoItem(
                icon = Icons.Default.Business,
                label = "Department",
                value = employee.department.ifEmpty { "Not assigned" }
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(
                icon = Icons.Default.Work,
                label = "Designation",
                value = employee.designation.ifEmpty { "Not assigned" }
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoItem(
                icon = Icons.Default.CalendarToday,
                label = "Joining Date",
                value = formatDate(employee.joiningDate)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Account Status
        SectionCard(title = "Account Status") {
            ProfileInfoItem(
                icon = if (employee.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                label = "Status",
                value = if (employee.isActive) "Active" else "Inactive",
                valueColor = if (employee.isActive) {
                    Color(0xFF4CAF50)
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            if (employee.createdAt != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ProfileInfoItem(
                    icon = Icons.Default.DateRange,
                    label = "Account Created",
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(employee.createdAt)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Edit mode info card
        if (isEditMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Email, Role, Department, and Designation cannot be changed. Contact admin for these updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Logout Button (only show when not in edit mode)
        if (!isEditMode) {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Not available"
    val date = Date(timestamp)
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
}
