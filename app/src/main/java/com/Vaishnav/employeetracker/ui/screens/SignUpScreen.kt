package com.Vaishnav.employeetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.Vaishnav.employeetracker.data.UserRole
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseEmployee
import java.util.Date

@Composable
fun SignUpScreen(
    onSignUpSuccess: (UserRole) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("EMPLOYEE") } // EMPLOYEE or ADMIN
    
    fun performSignUp() {
        errorMessage = ""
        
        // Validate inputs
        when {
            fullName.isEmpty() -> {
                errorMessage = "Please enter your full name"
                return
            }
            email.isEmpty() -> {
                errorMessage = "Please enter your email"
                return
            }
            !email.contains('@') -> {
                errorMessage = "Please enter a valid email address"
                return
            }
            phone.isEmpty() -> {
                errorMessage = "Please enter your phone number"
                return
            }
            phone.length < 10 -> {
                errorMessage = "Please enter a valid phone number"
                return
            }
            designation.isEmpty() -> {
                errorMessage = "Please enter your designation"
                return
            }
            department.isEmpty() -> {
                errorMessage = "Please enter your department"
                return
            }
            password.isEmpty() -> {
                errorMessage = "Please enter a password"
                return
            }
            password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters"
                return
            }
            confirmPassword.isEmpty() -> {
                errorMessage = "Please confirm your password"
                return
            }
            password != confirmPassword -> {
                errorMessage = "Passwords do not match"
                return
            }
        }
        
        isLoading = true
        
        coroutineScope.launch {
            try {
                android.util.Log.d("SignUpScreen", "=== Starting signup process ===")
                android.util.Log.d("SignUpScreen", "Email: $email")
                android.util.Log.d("SignUpScreen", "Name: $fullName")
                android.util.Log.d("SignUpScreen", "Phone: $phone")
                android.util.Log.d("SignUpScreen", "Designation: $designation")
                android.util.Log.d("SignUpScreen", "Department: $department")
                
                val authManager = FirebaseAuthManager.getInstance()
                authManager.initialize(context)
                
                android.util.Log.d("SignUpScreen", "FirebaseAuthManager initialized")
                
                // Use the selected role from UI
                val role = if (selectedRole == "ADMIN") {
                    UserRole.ADMIN
                } else {
                    UserRole.USER
                }
                
                android.util.Log.d("SignUpScreen", "Selected role: ${role.name}")
                
                // Register user with Firebase Auth
                val result = authManager.registerUser(
                    email = email,
                    password = password,
                    displayName = fullName,
                    role = role.name,
                    employeeId = "" // Will be set from Firestore document ID
                )
                
                result.onSuccess { firebaseUser ->
                    // Create employee record in Firestore
                    val employeeRepo = FirebaseManager.employeeRepository
                    
                    // Generate employee ID (EMP + timestamp suffix for uniqueness)
                    val empIdSuffix = System.currentTimeMillis() % 10000
                    val employeeId = "EMP${empIdSuffix.toString().padStart(4, '0')}"
                    
                    val newEmployee = FirebaseEmployee(
                        id = "", // Will be auto-generated by Firestore
                        name = fullName,
                        email = email,
                        phone = phone,
                        designation = designation,
                        department = department,
                        joiningDate = System.currentTimeMillis(),
                        employeeId = employeeId,
                        userId = firebaseUser.uid, // Link to Firebase Auth user
                        role = role.name, // Store selected role
                        addedBy = "", // Self-registered, not added by admin
                        isActive = true,
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    
                    val addResult = employeeRepo.addEmployee(newEmployee)
                    
                    addResult.onSuccess { docId ->
                        android.util.Log.d("SignUpScreen", "Employee document created with ID: $docId")
                        
                        // Verify the document was created with userId
                        kotlinx.coroutines.delay(500) // Small delay to ensure Firestore has persisted the data
                        
                        // Verify employee was created properly
                        val verifyEmployee = employeeRepo.getEmployeeByUserId(firebaseUser.uid)
                        if (verifyEmployee != null) {
                            android.util.Log.d("SignUpScreen", "Verified employee exists with userId: ${verifyEmployee.userId}")
                            android.util.Log.d("SignUpScreen", "Employee doc ID: ${verifyEmployee.id}, Name: ${verifyEmployee.name}")
                            
                            // Login with role and employee ID to save data
                            val loginResult = authManager.login(email, password, role.name, verifyEmployee.id)
                            
                            loginResult.onSuccess {
                                isLoading = false
                                onSignUpSuccess(role)
                            }.onFailure { e ->
                                errorMessage = "Login after signup failed: ${e.message}"
                                isLoading = false
                            }
                        } else {
                            android.util.Log.e("SignUpScreen", "Employee verification failed - document not found by userId")
                            errorMessage = "Account created but profile setup incomplete. Please try logging in."
                            isLoading = false
                        }
                    }.onFailure { e ->
                        errorMessage = "Failed to create employee profile: ${e.message}"
                        isLoading = false
                        android.util.Log.e("SignUpScreen", "Employee creation error", e)
                    }
                }.onFailure { e ->
                    android.util.Log.e("SignUpScreen", "Firebase signup error", e)
                    android.util.Log.e("SignUpScreen", "Error type: ${e.javaClass.simpleName}")
                    android.util.Log.e("SignUpScreen", "Error message: ${e.message}")
                    android.util.Log.e("SignUpScreen", "Error cause: ${e.cause?.message}")
                    
                    errorMessage = when {
                        e.message?.contains("already in use", ignoreCase = true) == true -> 
                            "This email is already registered"
                        e.message?.contains("weak-password", ignoreCase = true) == true -> 
                            "Password is too weak"
                        e.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your connection."
                        e.message?.contains("configuration", ignoreCase = true) == true ->
                            "Firebase Auth not configured. Please enable Email/Password sign-in in Firebase Console."
                        e.message?.contains("INTERNAL", ignoreCase = true) == true ->
                            "Firebase configuration error. Check if Authentication is enabled in Firebase Console."
                        else -> "Sign up failed: ${e.message ?: "Unknown error"}"
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Sign up failed: ${e.message ?: "Unknown error"}"
                isLoading = false
                android.util.Log.e("SignUpScreen", "Signup error", e)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFA855F7)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding
            Icon(
                Icons.Default.Badge,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Employee Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Create Your Account",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sign Up Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    
                    // Role Selection
                    Text(
                        text = "Select Your Role",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Employee Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = "EMPLOYEE" },
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (selectedRole == "EMPLOYEE") 4.dp else 0.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedRole == "EMPLOYEE") 
                                    Color(0xFF6366F1) 
                                else 
                                    Color(0xFFF3F4F6)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (selectedRole == "EMPLOYEE") 
                                    Color(0xFF6366F1) 
                                else 
                                    Color(0xFFE5E7EB)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Employee",
                                    tint = if (selectedRole == "EMPLOYEE") 
                                        Color.White 
                                    else 
                                        Color(0xFF6B7280),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Employee",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedRole == "EMPLOYEE") 
                                        Color.White 
                                    else 
                                        Color(0xFF1F2937)
                                )
                            }
                        }
                        
                        // HR/Admin Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = "ADMIN" },
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (selectedRole == "ADMIN") 4.dp else 0.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedRole == "ADMIN") 
                                    Color(0xFF6366F1) 
                                else 
                                    Color(0xFFF3F4F6)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (selectedRole == "ADMIN") 
                                    Color(0xFF6366F1) 
                                else 
                                    Color(0xFFE5E7EB)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "HR/Admin",
                                    tint = if (selectedRole == "ADMIN") 
                                        Color.White 
                                    else 
                                        Color(0xFF6B7280),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "HR/Admin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedRole == "ADMIN") 
                                        Color.White 
                                    else 
                                        Color(0xFF1F2937)
                                )
                            }
                        }
                    }
                    
                    // Full Name Field
                    TextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            errorMessage = ""
                        },
                        label = { Text("Full Name", color = Color(0xFF6B7280)) },
                        placeholder = { Text("Enter your full name", color = Color(0xFF9CA3AF)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF6366F1))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !isLoading
                    )
                    
                    // Email Field
                    TextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = ""
                        },
                        label = { Text("Email", color = Color(0xFF6B7280)) },
                        placeholder = { Text("your.email@example.com", color = Color(0xFF9CA3AF)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, null, tint = Color(0xFF6366F1))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !isLoading
                    )
                    
                    // Phone Field
                    TextField(
                        value = phone,
                        onValueChange = {
                            phone = it.filter { char -> char.isDigit() }.take(10)
                            errorMessage = ""
                        },
                        label = { Text("Phone Number", color = Color(0xFF6B7280)) },
                        placeholder = { Text("Enter 10 digit phone", color = Color(0xFF9CA3AF)) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, null, tint = Color(0xFF6366F1))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = phone.isNotEmpty() && phone.length != 10,
                        supportingText = {
                            if (phone.isNotEmpty() && phone.length != 10) {
                                Text("Phone must be 10 digits", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        enabled = !isLoading
                    )
                    
                    // Designation Field
                    TextField(
                        value = designation,
                        onValueChange = {
                            designation = it
                            errorMessage = ""
                        },
                        label = { Text("Designation", color = Color(0xFF6B7280)) },
                        placeholder = { Text("e.g. Software Engineer", color = Color(0xFF9CA3AF)) },
                        leadingIcon = {
                            Icon(Icons.Default.Work, null, tint = Color(0xFF6366F1))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !isLoading
                    )
                    
                    // Department Field
                    TextField(
                        value = department,
                        onValueChange = {
                            department = it
                            errorMessage = ""
                        },
                        label = { Text("Department", color = Color(0xFF6B7280)) },
                        placeholder = { Text("e.g. Engineering", color = Color(0xFF9CA3AF)) },
                        leadingIcon = {
                            Icon(Icons.Default.Business, null, tint = Color(0xFF6366F1))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !isLoading
                    )
                    
                    // Password Field
                    TextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                        },
                        label = { Text("Password", color = Color(0xFF6B7280)) },
                        placeholder = { Text("Enter password", color = Color(0xFF9CA3AF)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = Color(0xFF6366F1))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !isLoading
                    )
                    
                    // Confirm Password Field
                    TextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = ""
                        },
                        label = { Text("Confirm Password", color = Color(0xFF6B7280)) },
                        placeholder = { Text("Re-enter password", color = Color(0xFF9CA3AF)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = Color(0xFF6366F1))
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { performSignUp() }
                        ),
                        enabled = !isLoading
                    )
                    
                    // Error Message
                    if (errorMessage.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    // Sign Up Button
                    Button(
                        onClick = { performSignUp() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF8B5CF6)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Create Account",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Navigate to Login
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                        TextButton(onClick = onNavigateToLogin) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )
                        }
                    }
                    
                    // Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3F4F6)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Role Assignment:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Emails containing 'admin' automatically get admin privileges. All other accounts are created as regular users.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer
            Text(
                text = "© 2025 Employee Tracker",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
