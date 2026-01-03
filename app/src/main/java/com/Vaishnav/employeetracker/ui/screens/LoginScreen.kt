package com.Vaishnav.employeetracker.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.Vaishnav.employeetracker.data.AuthManager
import com.Vaishnav.employeetracker.data.UserRole
import com.Vaishnav.employeetracker.data.PreferencesManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit,
    onCreateAccount: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    
    fun performLogin() {
        // Clear previous error
        errorMessage = ""
        
        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            errorMessage = "Please enter both email and password"
            return
        }
        
        // Basic email validation
        if (!username.contains('@')) {
            errorMessage = "Please enter a valid email address"
            return
        }
        
        isLoading = true
        
        // Perform Firebase authentication
        coroutineScope.launch {
            try {
                // First, try to login to get the Firebase user
                val authManager = FirebaseAuthManager.getInstance()
                authManager.initialize(context)
                
                android.util.Log.d("LoginScreen", "=== Starting login process ===")
                android.util.Log.d("LoginScreen", "Email: $username")
                
                val tempResult = authManager.login(username, password)
                
                tempResult.onSuccess { firebaseUser ->
                    android.util.Log.d("LoginScreen", "Firebase Auth successful. UID: ${firebaseUser.uid}")
                    
                    // Query employee data directly by userId
                    val employeeRepo = FirebaseManager.employeeRepository
                    
                    try {
                        val employee = employeeRepo.getEmployeeByUserId(firebaseUser.uid)
                        
                        if (employee != null) {
                            android.util.Log.d("LoginScreen", "Found matching employee: ${employee.name} (${employee.employeeId})")
                            android.util.Log.d("LoginScreen", "Employee document ID: ${employee.id}")
                            android.util.Log.d("LoginScreen", "Employee role from Firestore: '${employee.role}'")
                            android.util.Log.d("LoginScreen", "Role field length: ${employee.role.length}")
                            android.util.Log.d("LoginScreen", "Role equals ADMIN: ${employee.role.equals("ADMIN", ignoreCase = true)}")
                            
                            // Use role from Firestore employee document with case-insensitive comparison
                            val role = if (employee.role.equals("ADMIN", ignoreCase = true)) {
                                UserRole.ADMIN
                            } else {
                                UserRole.USER
                            }
                            
                            android.util.Log.d("LoginScreen", "Assigned role: $role")
                            
                            // Re-login with role and employee ID to save data properly
                            authManager.logout()
                            val finalResult = authManager.login(username, password, role.name, employee.id)
                            
                            finalResult.onSuccess {
                                android.util.Log.d("LoginScreen", "Final login successful with role and employee ID")
                                
                                // Save session
                                PreferencesManager.saveLoginSession(context, employee.email)
                                
                                // Create legacy user object for compatibility
                                val user = com.Vaishnav.employeetracker.data.User(
                                    username = employee.email,
                                    password = "",
                                    role = role,
                                    fullName = employee.name,
                                    email = employee.email,
                                    employeeId = employee.id.toIntOrNull()
                                )
                                AuthManager.login(user)
                                
                                isLoading = false
                                onLoginSuccess(role)
                            }.onFailure { e ->
                                android.util.Log.e("LoginScreen", "Final login failed", e)
                                errorMessage = "Login failed: ${e.message}"
                                isLoading = false
                            }
                        } else {
                            // User exists in Firebase Auth but no employee record
                            android.util.Log.e("LoginScreen", "No employee found with userId: ${firebaseUser.uid}")
                            authManager.logout()
                            errorMessage = "Employee profile not found. Please contact admin."
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LoginScreen", "Error fetching employee", e)
                        authManager.logout()
                        errorMessage = "Error loading employee data: ${e.message}"
                        isLoading = false
                    }
                }.onFailure { e ->
                    errorMessage = when {
                        e.message?.contains("password", ignoreCase = true) == true -> "Invalid email or password"
                        e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection."
                        e.message?.contains("user", ignoreCase = true) == true -> "User not found. Please sign up first."
                        else -> "Login failed: ${e.message ?: "Unknown error"}"
                    }
                    isLoading = false
                    android.util.Log.e("LoginScreen", "Firebase login error", e)
                }
            } catch (e: Exception) {
                errorMessage = "Login failed: ${e.message ?: "Unknown error"}"
                isLoading = false
                android.util.Log.e("LoginScreen", "Login error", e)
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // TrackHR-style Bold Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1), // Indigo
                                Color(0xFF8B5CF6)  // Purple
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                
                // TrackHR-style App Branding
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    // Modern Icon Badge
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "📊",
                                fontSize = 42.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Employee Tracker",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
            
                
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Workforce Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // TrackHR-style Floating White Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Sign in to continue",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF6B7280)
                        )                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // TrackHR-style Email Field
                        TextField(
                            value = username,
                            onValueChange = { 
                                username = it
                                errorMessage = ""
                            },
                            label = { Text("Email", color = Color(0xFF6B7280)) },
                            placeholder = { Text("Enter your email", color = Color(0xFF9CA3AF)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1)
                                )
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
                                disabledIndicatorColor = Color.Transparent,
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
                    
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // TrackHR-style Password Field
                        TextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                errorMessage = ""
                            },
                            label = { Text("Password", color = Color(0xFF6B7280)) },
                            placeholder = { Text("Enter password", color = Color(0xFF9CA3AF)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1)
                                )
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
                                disabledIndicatorColor = Color.Transparent,
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827)
                            ),
                            visualTransformation = if (passwordVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { 
                                focusManager.clearFocus()
                                performLogin()
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) 
                                        Icons.Filled.Visibility 
                                    else 
                                        Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) 
                                        "Hide password" 
                                    else 
                                        "Show password"
                                )
                            }
                        },
                        enabled = !isLoading
                    )
                    
                    // Error Message with enhanced styling
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // TrackHR-style Gradient Button
                    Button(
                        onClick = { performLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF8B5CF6)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Text(
                                    text = "Sign In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Forgot Password Link with icon
                    TextButton(
                        onClick = { showForgotPasswordDialog = true },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                        TextButton(onClick = onCreateAccount, enabled = !isLoading) {
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Demo Credentials Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Demo Credentials",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "For first-time setup:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "1. Click 'Sign Up' to create your account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "2. Use your email and choose a password",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Accounts with 'admin' in email get admin access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer
            Text(
                text = "© 2025 Employee Tracker",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
        }
    }
    
    // Forgot Password Dialog - Firebase Email-Based Reset
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf("") }
        var resetError by remember { mutableStateOf("") }
        var resetSuccess by remember { mutableStateOf(false) }
        var isResetting by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog = false
                resetSuccess = false
                resetError = ""
            },
            icon = { Icon(Icons.Default.LockReset, null) },
            title = { Text("Reset Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (resetSuccess) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                        )
                        Text(
                            "Password reset email sent!",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Please check your email inbox and follow the instructions to reset your password.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Enter your email address and we'll send you instructions to reset your password.")
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { 
                                resetEmail = it
                                resetError = ""
                            },
                            label = { Text("Email") },
                            placeholder = { Text("your.email@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, null)
                            },
                            singleLine = true,
                            isError = resetError.isNotEmpty(),
                            enabled = !isResetting,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (resetError.isNotEmpty()) {
                            Text(
                                text = resetError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (resetSuccess) {
                    Button(
                        onClick = { 
                            showForgotPasswordDialog = false
                            resetSuccess = false
                        }
                    ) {
                        Text("OK")
                    }
                } else {
                    Button(
                        onClick = {
                            if (resetEmail.isEmpty()) {
                                resetError = "Please enter your email"
                            } else if (!resetEmail.contains('@')) {
                                resetError = "Please enter a valid email address"
                            } else {
                                isResetting = true
                                coroutineScope.launch {
                                    try {
                                        val authManager = FirebaseAuthManager.getInstance()
                                        val result = authManager.sendPasswordResetEmail(resetEmail)
                                        
                                        result.onSuccess {
                                            resetSuccess = true
                                            resetError = ""
                                        }.onFailure { e ->
                                            resetError = when {
                                                e.message?.contains("user", ignoreCase = true) == true -> 
                                                    "No account found with this email"
                                                e.message?.contains("network", ignoreCase = true) == true -> 
                                                    "Network error. Please check your connection."
                                                else -> "Failed to send reset email: ${e.message}"
                                            }
                                        }
                                        isResetting = false
                                    } catch (e: Exception) {
                                        resetError = "Error: ${e.message}"
                                        isResetting = false
                                    }
                                }
                            }
                        },
                        enabled = !isResetting
                    ) {
                        if (isResetting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Reset Link")
                        }
                    }
                }
            },
            dismissButton = {
                if (!resetSuccess) {
                    TextButton(
                        onClick = { 
                            showForgotPasswordDialog = false
                            resetError = ""
                        },
                        enabled = !isResetting
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

