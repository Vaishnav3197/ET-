package com.Vaishnav.employeetracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.ui.screens.LoginScreen
import com.Vaishnav.employeetracker.ui.screens.SplashScreen
import com.Vaishnav.employeetracker.ui.screens.admin.AdminDashboard
import com.Vaishnav.employeetracker.ui.screens.employee.EmployeeDashboard
import com.Vaishnav.employeetracker.ui.screens.employee.AttendanceScreenWrapper
import com.Vaishnav.employeetracker.ui.screens.employee.MyTasksScreenWrapper
import com.Vaishnav.employeetracker.ui.screens.employee.LeaveManagementScreenWrapper
import com.Vaishnav.employeetracker.ui.screens.GroupChatScreen
import kotlinx.coroutines.launch

sealed class AppScreen(val route: String) {
    object Splash : AppScreen("splash")
    object Login : AppScreen("login")
    object AdminDashboard : AppScreen("admin_dashboard")
    object EmployeeDashboard : AppScreen("employee_dashboard")
    object Attendance : AppScreen("attendance")
    object Tasks : AppScreen("tasks")
    object Leave : AppScreen("leave")
    object GroupChat : AppScreen("group_chat")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager.getInstance() }
    
    NavHost(
        navController = navController,
        startDestination = AppScreen.Splash.route
    ) {
        // Splash Screen
        composable(AppScreen.Splash.route) {
            SplashScreen(
                onTimeout = {
                    // Check if user is logged in
                    scope.launch {
                        val userId = authManager.getCurrentUserId()
                        if (userId != null) {
                            // Check user role
                            try {
                                val employee = FirebaseManager.employeeRepository.getEmployeeByUserId(userId)
                                if (employee?.role == "Admin") {
                                    navController.navigate(AppScreen.AdminDashboard.route) {
                                        popUpTo(AppScreen.Splash.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(AppScreen.EmployeeDashboard.route) {
                                        popUpTo(AppScreen.Splash.route) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                navController.navigate(AppScreen.Login.route) {
                                    popUpTo(AppScreen.Splash.route) { inclusive = true }
                                }
                            }
                        } else {
                            navController.navigate(AppScreen.Login.route) {
                                popUpTo(AppScreen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
        
        // Login Screen
        composable(AppScreen.Login.route) {
            LoginScreen(
                onLoginSuccess = { userRole ->
                    val destination = if (userRole.name == "Admin") {
                        AppScreen.AdminDashboard.route
                    } else {
                        AppScreen.EmployeeDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(AppScreen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Admin Dashboard
        composable(AppScreen.AdminDashboard.route) {
            AdminDashboard(
                onNavigateToAttendance = {
                    // Admin attendance monitoring - would need separate screen
                },
                onNavigateToEmployees = {
                    // Navigate to employee directory
                },
                onNavigateToTasks = {
                    // Navigate to task assignment
                },
                onNavigateToLeave = {
                    // Navigate to leave approval
                },
                onNavigateToReports = {
                    // Navigate to reports
                },
                onNavigateToAnalytics = {
                    // Navigate to analytics
                },
                onNavigateToPayroll = {
                    // Navigate to payroll
                },
                onNavigateToShiftManagement = {
                    // Navigate to shift management
                },
                onNavigateToDocuments = {
                    // Navigate to documents
                },
                onNavigateToMessaging = {
                    navController.navigate(AppScreen.GroupChat.route)
                },
                onNavigateToProfile = {
                    // Navigate to profile
                },
                onLogout = {
                    scope.launch {
                        authManager.logout()
                        navController.navigate(AppScreen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Employee Dashboard
        composable(AppScreen.EmployeeDashboard.route) {
            EmployeeDashboard(
                onNavigateToAttendance = {
                    navController.navigate(AppScreen.Attendance.route)
                },
                onNavigateToTasks = {
                    navController.navigate(AppScreen.Tasks.route)
                },
                onNavigateToLeave = {
                    navController.navigate(AppScreen.Leave.route)
                },
                onNavigateToProfile = {
                    // Navigate to profile
                },
                onNavigateToAnalytics = {
                    // Navigate to employee analytics
                },
                onNavigateToPayroll = {
                    // Navigate to payroll view
                },
                onNavigateToShiftManagement = {
                    // Navigate to shift view
                },
                onNavigateToDocuments = {
                    // Navigate to documents
                },
                onNavigateToMessaging = {
                    navController.navigate(AppScreen.GroupChat.route)
                },
                onLogout = {
                    scope.launch {
                        authManager.logout()
                        navController.navigate(AppScreen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Attendance Screen - self-contained, gets employee from Firebase Auth
        composable(AppScreen.Attendance.route) {
            // AttendanceScreenWrapper loads employee data before showing the screen
            AttendanceScreenWrapper(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Tasks Screen - self-contained, gets employee from Firebase Auth
        composable(AppScreen.Tasks.route) {
            MyTasksScreenWrapper(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Leave Management Screen - self-contained, gets employee from Firebase Auth
        composable(AppScreen.Leave.route) {
            LeaveManagementScreenWrapper(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Group Chat Screen
        composable(AppScreen.GroupChat.route) {
            var groupId by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            
            LaunchedEffect(Unit) {
                scope.launch {
                    try {
                        val userId = authManager.getCurrentUserId()
                        if (userId != null) {
                            val employee = FirebaseManager.employeeRepository.getEmployeeByUserId(userId)
                            if (employee != null) {
                                // Get or create group chat for this employee's organization
                                // For now, use a default group ID
                                groupId = "company_group"
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppNavigation", "Error loading group chat", e)
                    } finally {
                        isLoading = false
                    }
                }
            }
            
            if (!isLoading && groupId != null) {
                GroupChatScreen(
                    groupId = groupId!!,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
