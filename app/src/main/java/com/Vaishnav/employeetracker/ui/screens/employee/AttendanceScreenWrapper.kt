package com.Vaishnav.employeetracker.ui.screens.employee

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.viewmodel.AttendanceViewModel

/**
 * Wrapper for AttendanceScreen that automatically fetches employee data
 * Follows MVVM: UI → ViewModel → Repository → Firebase
 */
@Composable
fun AttendanceScreenWrapper(
    onNavigateBack: () -> Unit,
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    var employeeId by remember { mutableStateOf<String?>(null) }
    var employeeName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Get Firebase Auth UID
    val firebaseUserId = remember { FirebaseAuthManager.getInstance().getCurrentUserId() }
    
    // Load employee data
    LaunchedEffect(firebaseUserId) {
        if (firebaseUserId.isNullOrEmpty()) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            android.util.Log.d("AttendanceWrapper", "Loading employee for userId: $firebaseUserId")
            val employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
            
            if (employee != null) {
                employeeId = employee.id
                employeeName = employee.name
                android.util.Log.d("AttendanceWrapper", "Loaded employee: ${employee.name} (ID: ${employee.id})")
            } else {
                android.util.Log.e("AttendanceWrapper", "Employee not found for userId: $firebaseUserId")
                errorMessage = "Employee profile not found"
            }
        } catch (e: Exception) {
            android.util.Log.e("AttendanceWrapper", "Error loading employee", e)
            errorMessage = "Error loading profile: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    // Show loading state
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Show error state
    if (errorMessage != null || employeeId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = errorMessage ?: "Unable to load attendance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }
    
    // Show attendance screen with valid employee data
    AttendanceScreen(
        employeeId = employeeId!!,
        employeeName = employeeName,
        onNavigateBack = onNavigateBack,
        attendanceViewModel = attendanceViewModel
    )
}
