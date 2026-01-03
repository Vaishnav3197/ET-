package com.Vaishnav.employeetracker.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class User(
    val username: String,
    val password: String,
    val role: UserRole,
    val fullName: String = "",
    val email: String = "",
    val employeeId: Int? = null,  // Link to Employee table
    val securityQuestion: String? = "What is your favorite color?",
    val securityAnswer: String? = "blue"
)

enum class UserRole {
    USER,
    ADMIN
}

object AuthManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val USERS_KEY = "users"
    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    
    private val defaultUsers = listOf(
        User("user", "user123", UserRole.USER, "Demo User", "user@example.com", employeeId = 1),
        User("admin", "admin123", UserRole.ADMIN, "Admin User", "admin@example.com", employeeId = 2)
    )
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Load users from SharedPreferences or use defaults
        if (getUsers().isEmpty()) {
            saveUsers(defaultUsers)
        }
    }
    
    private fun getUsers(): List<User> {
        val json = prefs?.getString(USERS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<User>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveUsers(users: List<User>) {
        val json = gson.toJson(users)
        prefs?.edit()?.putString(USERS_KEY, json)?.apply()
    }
    
    fun authenticate(username: String, password: String): User? {
        return getUsers().find { it.username == username && it.password == password }
    }
    
    fun createEmployeeAccount(
        employeeId: String,
        fullName: String,
        email: String,
        linkedEmployeeId: Int? = null
    ): Pair<String, String> {
        // Auto-generate username and password
        val username = employeeId.lowercase() // Use employee ID as username (e.g., "emp001")
        val password = "pass@${employeeId.takeLast(3)}" // Generate password (e.g., "pass@001")
        
        val user = User(
            username = username,
            password = password,
            role = UserRole.USER,
            fullName = fullName,
            email = email,
            employeeId = linkedEmployeeId
        )
        
        val currentUsers = getUsers().toMutableList()
        currentUsers.add(user)
        saveUsers(currentUsers)
        
        // Return username and password so admin can share with employee
        return Pair(username, password)
    }
    
    fun registerUser(
        username: String,
        password: String,
        fullName: String,
        email: String,
        role: UserRole,
        employeeId: Int? = null
    ): Boolean {
        val currentUsers = getUsers()
        // Check if username already exists
        if (currentUsers.any { it.username == username }) {
            return false
        }
        
        // Add new user
        val newUser = User(
            username = username,
            password = password,
            role = role,
            fullName = fullName,
            email = email,
            employeeId = employeeId
        )
        saveUsers(currentUsers + newUser)
        return true
    }
    
    fun getAllUsers(): List<User> {
        return getUsers()
    }
    
    fun changePassword(username: String, oldPassword: String, newPassword: String): Boolean {
        val currentUsers = getUsers()
        val user = currentUsers.find { it.username == username && it.password == oldPassword }
        if (user != null) {
            val updatedUsers = currentUsers.map {
                if (it.username == username) it.copy(password = newPassword) else it
            }
            saveUsers(updatedUsers)
            // Update current user if it's the same user
            if (currentUser?.username == username) {
                currentUser = updatedUsers.find { it.username == username }
            }
            return true
        }
        return false
    }
    
    fun getUserByUsername(username: String): User? {
        return getUsers().find { it.username == username }
    }
    
    fun updateUserProfile(oldUsername: String, newUsername: String, fullName: String, email: String): Boolean {
        val currentUsers = getUsers()
        // Check if new username is taken by another user
        if (newUsername != oldUsername && currentUsers.any { it.username == newUsername }) {
            return false
        }
        
        val user = currentUsers.find { it.username == oldUsername }
        if (user != null) {
            val updatedUsers = currentUsers.map {
                if (it.username == oldUsername) it.copy(username = newUsername, fullName = fullName, email = email) else it
            }
            saveUsers(updatedUsers)
            // Update current user if it's the same user
            if (currentUser?.username == oldUsername) {
                currentUser = updatedUsers.find { it.username == newUsername }
            }
            return true
        }
        return false
    }
    
    var currentUser: User? = null
        private set
    
    fun login(user: User) {
        currentUser = user
    }
    
    fun logout() {
        currentUser = null
    }
    
    fun isAdmin(): Boolean {
        return currentUser?.role == UserRole.ADMIN
    }
    
    fun getCurrentUserId(): Int {
        // Return the linked employee ID, or fallback to 1
        return currentUser?.employeeId ?: 1
    }
    
    fun getCurrentUserName(): String {
        return currentUser?.fullName ?: "User"
    }
    
    fun getUserRole(): String {
        return if (currentUser?.role == UserRole.ADMIN) "admin" else "employee"
    }
    
    fun resetPassword(username: String, newPassword: String): Boolean {
        val currentUsers = getUsers().toMutableList()
        val userIndex = currentUsers.indexOfFirst { it.username == username }
        
        if (userIndex != -1) {
            currentUsers[userIndex] = currentUsers[userIndex].copy(password = newPassword)
            saveUsers(currentUsers)
            return true
        }
        return false
    }
}

