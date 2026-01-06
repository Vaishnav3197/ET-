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
import com.Vaishnav.employeetracker.viewmodel.TaskViewModel

/**
 * Wrapper for MyTasksScreen that automatically fetches employee data
 * Follows MVVM: UI → ViewModel → Repository → Firebase
 */
@Composable
fun MyTasksScreenWrapper(
    onNavigateBack: () -> Unit,
    taskViewModel: TaskViewModel = viewModel()
) {
    var employeeId by remember { mutableStateOf<String?>(null) }
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
            val employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
            if (employee != null) {
                employeeId = employee.id
            } else {
                errorMessage = "Employee profile not found"
            }
        } catch (e: Exception) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = errorMessage ?: "Failed to load employee data",
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
    
    // Show the actual screen with valid data
    MyTasksScreen(
        employeeId = employeeId!!,
        onNavigateBack = onNavigateBack,
        taskViewModel = taskViewModel
    )
}
