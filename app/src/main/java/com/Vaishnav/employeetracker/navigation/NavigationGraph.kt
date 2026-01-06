package com.Vaishnav.employeetracker.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.Vaishnav.employeetracker.ui.screens.*
import com.Vaishnav.employeetracker.ui.screens.employee.*
import com.Vaishnav.employeetracker.ui.screens.admin.*
import com.Vaishnav.employeetracker.data.AuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.utils.BiometricAuthHelper
import android.content.Context

@Composable
fun NavigationGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    isFirstTime: Boolean = false,
    context: Context,
    isLoggedIn: Boolean = false,
    userRole: String = "employee"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ===== AUTHENTICATION SCREENS =====
        composable(Screen.Splash.route) {
            SplashScreen(
                onTimeout = {
                    val destination = when {
                        isFirstTime -> Screen.Onboarding.route
                        isLoggedIn -> {
                            if (userRole == "admin") Screen.AdminDashboard.route
                            else Screen.EmployeeDashboard.route
                        }
                        else -> Screen.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val destination = if (role == com.Vaishnav.employeetracker.data.UserRole.ADMIN) {
                        Screen.AdminDashboard.route
                    } else {
                        Screen.EmployeeDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onCreateAccount = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            SignUpScreen(
                onSignUpSuccess = { role ->
                    // Navigate based on selected role
                    if (role == com.Vaishnav.employeetracker.data.UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.EmployeeDashboard.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // ===== EMPLOYEE SCREENS =====
        composable(Screen.EmployeeDashboard.route) {
            val firebaseAuthManager = remember { FirebaseAuthManager.getInstance() }
            val firebaseUserId = firebaseAuthManager.getCurrentUserId()
            
            // Get employee document ID with proper state management
            var employee by remember { mutableStateOf<com.Vaishnav.employeetracker.data.firebase.FirebaseEmployee?>(null) }
            var isLoadingEmployee by remember { mutableStateOf(true) }
            
            LaunchedEffect(firebaseUserId) {
                if (firebaseUserId != null) {
                    try {
                        employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
                    } catch (e: Exception) {
                        android.util.Log.e("NavigationGraph", "Error loading employee", e)
                    } finally {
                        isLoadingEmployee = false
                    }
                } else {
                    isLoadingEmployee = false
                }
            }
            
            EmployeeDashboard(
                onNavigateToAttendance = {
                    navController.navigate(Screen.Attendance.route)
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.MyTasks.route)
                },
                onNavigateToLeave = {
                    navController.navigate(Screen.LeaveManagement.route)  // Employee uses LeaveManagement
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.EmployeeProfile.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToPayroll = {
                    if (!isLoadingEmployee && employee?.id != null) {
                        navController.navigate(Screen.Payroll.createRoute(employee!!.id))
                    }
                },
                onNavigateToShiftManagement = {
                    if (!isLoadingEmployee && employee?.id != null) {
                        navController.navigate(Screen.ShiftManagement.createRoute(employee!!.id))
                    }
                },
                onNavigateToDocuments = {
                    if (!isLoadingEmployee) {
                        navController.navigate(Screen.Documents.createRoute(employee?.id ?: ""))
                    }
                },
                onNavigateToMessaging = {
                    navController.navigate(Screen.Messaging.route)
                },
                onLogout = {
                    AuthManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Attendance.route) {
            val firebaseUserId = FirebaseAuthManager.getInstance().getCurrentUserId()
            var employee by remember { mutableStateOf<com.Vaishnav.employeetracker.data.firebase.FirebaseEmployee?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            
            LaunchedEffect(firebaseUserId) {
                if (firebaseUserId != null) {
                    try {
                        employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
                        if (employee == null) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    } catch (e: Exception) {
                        navController.popBackStack()
                    } finally {
                        isLoading = false
                    }
                } else {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            
            if (!isLoading && employee != null) {
                AttendanceScreen(
                    employeeId = employee!!.id,
                    employeeName = employee!!.name,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else if (!isLoading) {
                // Show error or redirect
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        composable(Screen.MyTasks.route) {
            val firebaseUserId = FirebaseAuthManager.getInstance().getCurrentUserId()
            var employee by remember { mutableStateOf<com.Vaishnav.employeetracker.data.firebase.FirebaseEmployee?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            
            LaunchedEffect(firebaseUserId) {
                if (firebaseUserId != null) {
                    try {
                        employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
                    } catch (e: Exception) {
                        navController.popBackStack()
                    } finally {
                        isLoading = false
                    }
                } else {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            
            if (!isLoading && employee != null) {
                MyTasksScreen(
                    employeeId = employee!!.id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        composable(Screen.LeaveManagement.route) {
            val firebaseUserId = FirebaseAuthManager.getInstance().getCurrentUserId()
            var employee by remember { mutableStateOf<com.Vaishnav.employeetracker.data.firebase.FirebaseEmployee?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var errorOccurred by remember { mutableStateOf(false) }
            
            LaunchedEffect(firebaseUserId) {
                if (firebaseUserId != null) {
                    try {
                        employee = FirebaseManager.employeeRepository.getEmployeeByUserId(firebaseUserId)
                        if (employee == null) {
                            errorOccurred = true
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NavigationGraph", "Error loading employee for leave", e)
                        errorOccurred = true
                    } finally {
                        isLoading = false
                    }
                } else {
                    errorOccurred = true
                    isLoading = false
                }
            }
            
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorOccurred || employee == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error loading employee data")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                else -> {
                    LeaveManagementScreen(
                        employeeId = employee!!.id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
        
        // Employee Profile Screen
        composable(Screen.EmployeeProfile.route) {
            com.Vaishnav.employeetracker.ui.screens.employee.ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    com.Vaishnav.employeetracker.data.AuthManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // ===== ADMIN SCREENS =====
        composable(Screen.AdminDashboard.route) {
            AdminDashboard(
                onNavigateToAttendance = {
                    navController.navigate(Screen.AttendanceMonitoring.route)
                },
                onNavigateToEmployees = {
                    navController.navigate(Screen.EmployeeDirectory.route)
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.TaskAssignment.route)
                },
                onNavigateToLeave = {
                    navController.navigate(Screen.LeaveApproval.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToPayroll = {
                    // Admin views all employees' payroll
                    navController.navigate(Screen.Payroll.createRoute("all"))
                },
                onNavigateToShiftManagement = {
                    // Admin views all shifts
                    navController.navigate(Screen.ShiftManagement.createRoute("all"))
                },
                onNavigateToDocuments = {
                    // Admin views all documents
                    navController.navigate(Screen.Documents.createRoute())
                },
                onNavigateToMessaging = {
                    navController.navigate(Screen.Messaging.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.EmployeeProfile.route)
                },
                onLogout = {
                    AuthManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.AttendanceMonitoring.route) {
            AttendanceMonitoringScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.EmployeeDirectory.route) {
            EmployeeDirectoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.TaskAssignment.route) {
            TaskAssignmentScreen(
                adminId = AuthManager.getCurrentUserId().takeIf { it > 0 } ?: 1,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.LeaveApproval.route) {
            // Admin Leave Approval - no employee data needed
            val firebaseUserId = FirebaseAuthManager.getInstance().getCurrentUserId()
            
            if (firebaseUserId != null) {
                LeaveApprovalScreen(
                    adminId = firebaseUserId,  // Use Firebase auth UID directly
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        
        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // New Feature Screens
        composable(Screen.BiometricSetup.route) {
            BiometricSetupScreen(
                activity = context as androidx.fragment.app.FragmentActivity,
                onSuccess = {
                    navController.navigate(Screen.EmployeeDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.EmployeeDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                isAdmin = AuthManager.isAdmin(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Payroll.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val employeeIdParam = backStackEntry.arguments?.getString("employeeId") ?: ""
            val isAdmin = AuthManager.isAdmin()
            
            PayrollScreen(
                employeeId = employeeIdParam,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ShiftManagement.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val employeeIdParam = backStackEntry.arguments?.getString("employeeId") ?: ""
            val isAdmin = AuthManager.isAdmin()
            
            ShiftManagementScreen(
                employeeId = employeeIdParam,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Documents.route,
            arguments = listOf(navArgument("employeeId") { 
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) { backStackEntry ->
            val employeeIdParam = backStackEntry.arguments?.getString("employeeId") ?: ""
            val isAdmin = AuthManager.isAdmin()
            
            DocumentsScreen(
                employeeId = employeeIdParam,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Messaging.route) {
            val firebaseUserId = FirebaseAuthManager.getInstance().getCurrentUserId()
            if (firebaseUserId != null) {
                // Navigate to company-wide group chat for all users
                GroupChatScreen(
                    groupId = "company_group",
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // User not authenticated, redirect to login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        
        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            val firebaseUserId = FirebaseAuthManager.getInstance().getCurrentUserId()
            
            if (firebaseUserId != null && otherUserId.isNotEmpty()) {
                ConversationScreen(
                    currentUserId = firebaseUserId,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // User not authenticated, redirect to login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}
