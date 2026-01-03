package com.Vaishnav.employeetracker.data.firebase

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager private constructor() {
    
    private val auth = FirebaseAuth.getInstance()
    private lateinit var prefs: SharedPreferences
    
    companion object {
        private const val PREFS_NAME = "firebase_auth_prefs"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_EMPLOYEE_ID = "employee_id"
        
        @Volatile
        private var INSTANCE: FirebaseAuthManager? = null
        
        fun getInstance(): FirebaseAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseAuthManager().also { INSTANCE = it }
            }
        }
    }
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Get current Firebase user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    // Get current user ID (Firebase UID)
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    // Get current user email
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    
    // Get current user display name
    fun getCurrentUserName(): String? = auth.currentUser?.displayName
    
    // Check if user is logged in
    fun isLoggedIn(): Boolean = auth.currentUser != null
    
    // Get user role from SharedPreferences
    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "USER") ?: "USER"
    }
    
    // Check if current user is admin
    fun isAdmin(): Boolean = getUserRole() == "ADMIN"
    
    // Get linked employee ID
    fun getEmployeeId(): String? {
        return prefs.getString(KEY_EMPLOYEE_ID, null)
    }
    
    // Save user role and employee ID
    private fun saveUserData(role: String, employeeId: String?) {
        prefs.edit().apply {
            putString(KEY_USER_ROLE, role)
            employeeId?.let { putString(KEY_EMPLOYEE_ID, it) }
            apply()
        }
    }
    
    // Clear user data
    private fun clearUserData() {
        prefs.edit().clear().apply()
    }
    
    // Register new user with email and password
    suspend fun registerUser(
        email: String,
        password: String,
        displayName: String,
        role: String = "USER",
        employeeId: String? = null
    ): Result<FirebaseUser> {
        return try {
            android.util.Log.d("FirebaseAuthManager", "Attempting to register user: $email")
            android.util.Log.d("FirebaseAuthManager", "Firebase Auth instance: ${auth.app.name}")
            
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("User creation failed"))
            
            android.util.Log.d("FirebaseAuthManager", "User created successfully: ${user.uid}")
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()
            
            android.util.Log.d("FirebaseAuthManager", "Profile updated with name: $displayName")
            
            // Save user role and employee ID
            saveUserData(role, employeeId)
            
            android.util.Log.d("FirebaseAuthManager", "User data saved - Role: $role, EmployeeId: $employeeId")
            
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthManager", "Registration failed", e)
            android.util.Log.e("FirebaseAuthManager", "Error type: ${e.javaClass.simpleName}")
            android.util.Log.e("FirebaseAuthManager", "Error message: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Login with email and password
    suspend fun login(
        email: String,
        password: String,
        role: String = "USER",
        employeeId: String? = null
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Login failed"))
            
            // Save user role and employee ID
            saveUserData(role, employeeId)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Logout
    fun logout() {
        auth.signOut()
        clearUserData()
    }
    
    // Send password reset email
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Change password
    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update user profile
    suspend fun updateProfile(displayName: String? = null, photoUrl: String? = null): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }.build()
            
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update email
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.updateEmail(newEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Re-authenticate user (required for sensitive operations)
    suspend fun reauthenticate(email: String, password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete user account
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.delete().await()
            clearUserData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create employee account (for admin)
    suspend fun createEmployeeAccount(
        employeeId: String,
        fullName: String,
        email: String
    ): Result<Pair<String, String>> {
        return try {
            // Generate password
            val password = "EmpTrack@${employeeId.takeLast(4)}"
            
            // Create Firebase user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("User creation failed"))
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // Sign out immediately (admin should remain logged in)
            auth.signOut()
            
            Result.success(Pair(email, password))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Verify email
    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check if email is verified
    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true
    
    // Reload user data
    suspend fun reloadUser(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.reload().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
