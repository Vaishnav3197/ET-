package com.Vaishnav.employeetracker.navigation

sealed class Screen(val route: String) {
    // Authentication
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    
    // Employee Screens
    object EmployeeDashboard : Screen("employee_dashboard")
    object Attendance : Screen("attendance")
    object MyTasks : Screen("my_tasks")
    object LeaveManagement : Screen("leave_management")
    object EmployeeProfile : Screen("employee_profile")
    
    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard")
    object AttendanceMonitoring : Screen("attendance_monitoring")
    object EmployeeDirectory : Screen("employee_directory")
    object TaskAssignment : Screen("task_assignment")
    object LeaveApproval : Screen("leave_approval")
    object Reports : Screen("reports")
    
    // New Feature Screens
    object BiometricSetup : Screen("biometric_setup")
    object Analytics : Screen("analytics")
    object Payroll : Screen("payroll/{employeeId}") {
        fun createRoute(employeeId: String) = "payroll/$employeeId"
    }
    object ShiftManagement : Screen("shift_management/{employeeId}") {
        fun createRoute(employeeId: String) = "shift_management/$employeeId"
    }
    object Documents : Screen("documents?employeeId={employeeId}") {
        fun createRoute(employeeId: String = "") = if (employeeId.isEmpty()) "documents" else "documents?employeeId=$employeeId"
    }
    object Messaging : Screen("messaging")
    object Conversation : Screen("conversation/{otherUserId}/{otherUserName}") {
        fun createRoute(otherUserId: String, otherUserName: String) = "conversation/$otherUserId/$otherUserName"
    }
}
