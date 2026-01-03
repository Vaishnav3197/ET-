package com.Vaishnav.employeetracker.data.firebase

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Singleton manager for Firebase repositories.
 * Provides centralized access to all Firebase repositories.
 * 
 * Usage:
 * - Call FirebaseManager.initialize(context) in MainActivity
 * - Access repositories via FirebaseManager.employeeRepository, etc.
 * - Toggle useFirebase flag to switch between Room and Firebase
 */
object FirebaseManager {
    
    // Firebase is now the only database (Room removed)
    var useFirebase: Boolean = true
    
    private var isInitialized = false
    
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    
    // Repository instances
    val employeeRepository: FirebaseEmployeeRepository by lazy {
        FirebaseEmployeeRepository()
    }
    
    val taskRepository: FirebaseTaskRepository by lazy {
        FirebaseTaskRepository()
    }
    
    val attendanceRepository: FirebaseAttendanceRepository by lazy {
        FirebaseAttendanceRepository()
    }
    
    val leaveRepository: FirebaseLeaveRepository by lazy {
        FirebaseLeaveRepository()
    }
    
    val notificationRepository: FirebaseNotificationRepository by lazy {
        FirebaseNotificationRepository()
    }
    
    val shiftRepository: FirebaseShiftRepository by lazy {
        FirebaseShiftRepository()
    }
    
    val timeLogRepository: FirebaseTimeLogRepository by lazy {
        FirebaseTimeLogRepository()
    }
    
    val payrollRepository: FirebasePayrollRepository by lazy {
        FirebasePayrollRepository()
    }
    
    val documentRepository: FirebaseDocumentRepository by lazy {
        FirebaseDocumentRepository()
    }
    
    val performanceRepository: FirebasePerformanceRepository by lazy {
        FirebasePerformanceRepository()
    }
    
    val messageRepository: FirebaseMessageRepository by lazy {
        FirebaseMessageRepository()
    }
    
    val authManager: FirebaseAuthManager by lazy {
        FirebaseAuthManager.getInstance()
    }
    
    /**
     * Initialize Firebase Manager
     * Call this from Application or MainActivity onCreate
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            isInitialized = true
            // Initialize FirebaseAuthManager
            authManager.initialize(context)
        }
    }
    
    /**
     * Enable Firebase mode - switches all operations to use Firebase
     */
    fun enableFirebase() {
        useFirebase = true
    }
    
    /**
     * Disable Firebase mode - switches all operations to use Room
     */
    fun disableFirebase() {
        useFirebase = false
    }
    
    /**
     * Check if Firebase is currently enabled
     */
    fun isFirebaseEnabled(): Boolean = useFirebase
}
