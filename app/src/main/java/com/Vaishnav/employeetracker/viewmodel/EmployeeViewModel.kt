package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.*
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmployeeViewModel(application: Application) : AndroidViewModel(application) {
    private val employeeRepo = FirebaseManager.employeeRepository
    private val notificationRepo = FirebaseManager.notificationRepository
    
    fun getAllActiveEmployees(): Flow<List<Employee>> {
        return employeeRepo.getAllActiveEmployees().map { firebaseEmployees ->
            firebaseEmployees.map { it.toRoomEmployee() }
        }
    }
    
    suspend fun getEmployeeById(employeeId: Int): Employee? {
        val result = employeeRepo.getEmployeeById(employeeId.toString())
        return result?.toRoomEmployee()
    }
    
    suspend fun getEmployeeByUserId(userId: Int): Employee? {
        // Get all employees and filter by userId since repository doesn't have this method
        val employees = employeeRepo.getAllActiveEmployees().first()
        return employees.firstOrNull { it.userId == userId.toString() }?.toRoomEmployee()
    }
    
    suspend fun getEmployeeByFirebaseUserId(firebaseUserId: String): Employee? {
        // Use the repository's getEmployeeByUserId method which queries by Firebase Auth UID
        val firebaseEmployee = employeeRepo.getEmployeeByUserId(firebaseUserId)
        return firebaseEmployee?.toRoomEmployee()
    }
    
    fun getEmployeesByDepartment(department: String): Flow<List<Employee>> {
        return employeeRepo.getEmployeesByDepartment(department).map { firebaseEmployees ->
            firebaseEmployees.map { it.toRoomEmployee() }
        }
    }
    
    fun searchEmployees(query: String): Flow<List<Employee>> {
        return employeeRepo.searchEmployees(query).map { firebaseEmployees ->
            firebaseEmployees.map { it.toRoomEmployee() }
        }
    }
    
    fun getEmployeesByAdminId(adminId: String): Flow<List<Employee>> {
        return FirebaseManager.employeeRepository.getEmployeesByAdminId(adminId).map { firebaseEmployees ->
            firebaseEmployees.map { it.toRoomEmployee() }
        }
    }
    
    suspend fun getActiveEmployeeCount(): Int {
        return employeeRepo.getAllActiveEmployees().first().count { it.isActive }
    }
    
    suspend fun addEmployee(
        name: String,
        email: String,
        phone: String,
        designation: String,
        department: String,
        joiningDate: Long,
        employeeId: String,
        adminId: String = "",
        profilePhotoUri: String? = null
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("EmployeeViewModel", "Starting addEmployee for: $name ($employeeId)")
            
            // Validate required fields
            if (name.isBlank()) {
                return@withContext Result.failure(Exception("Name is required"))
            }
            if (email.isBlank() || !isValidEmail(email)) {
                return@withContext Result.failure(Exception("Valid email is required"))
            }
            if (phone.isBlank() || phone.length < 10) {
                return@withContext Result.failure(Exception("Valid phone number is required"))
            }
            if (employeeId.isBlank()) {
                return@withContext Result.failure(Exception("Employee ID is required"))
            }
            
            // Generate password: first 4 letters of name + @ + last 4 digits of phone
            val namePrefix = name.take(4).lowercase()
            val phoneSuffix = phone.takeLast(4)
            val generatedPassword = "$namePrefix@$phoneSuffix"
            
            android.util.Log.d("EmployeeViewModel", "Generated credentials - Email: $email, Password: $generatedPassword")
            
            // Register employee in Firebase Authentication
            val firebaseAuthManager = com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager.getInstance()
            val registerResult = firebaseAuthManager.registerUser(
                email = email,
                password = generatedPassword,
                displayName = name,
                role = "USER",
                employeeId = ""
            )
            
            if (registerResult.isFailure) {
                val error = registerResult.exceptionOrNull()
                android.util.Log.e("EmployeeViewModel", "Firebase Auth registration FAILED: ${error?.message}", error)
                return@withContext Result.failure(error ?: Exception("Failed to register employee in Firebase Auth"))
            }
            
            val firebaseUser = registerResult.getOrNull()
            android.util.Log.d("EmployeeViewModel", "Firebase Auth user created with UID: ${firebaseUser?.uid}")
            
            // Create user account in legacy AuthManager
            val (username, password) = AuthManager.createEmployeeAccount(
                employeeId = employeeId,
                fullName = name,
                email = email
            )
            
            android.util.Log.d("EmployeeViewModel", "Legacy user account created: $username")
            
            val firebaseEmployee = FirebaseEmployee(
                name = name,
                email = email,
                phone = phone,
                designation = designation,
                department = department,
                joiningDate = joiningDate,
                profilePhotoUri = profilePhotoUri,
                employeeId = employeeId,
                userId = firebaseUser?.uid ?: "",
                addedBy = adminId,
                isActive = true
            )
            
            android.util.Log.d("EmployeeViewModel", "Attempting Firebase write for employee: $employeeId")
            
            // Add employee to Firebase and check for errors
            val addResult = employeeRepo.addEmployee(firebaseEmployee)
            if (addResult.isFailure) {
                val error = addResult.exceptionOrNull()
                android.util.Log.e("EmployeeViewModel", "Firebase write FAILED: ${error?.message}", error)
                return@withContext Result.failure(error ?: Exception("Failed to add employee to Firebase"))
            }
            
            val firebaseDocId = addResult.getOrNull()
            android.util.Log.d("EmployeeViewModel", "Firebase write SUCCESS! Document ID: $firebaseDocId")
            
            // Return email and generated password as credentials
            Result.success(Pair(email, generatedPassword))
        } catch (e: Exception) {
            android.util.Log.e("EmployeeViewModel", "Exception in addEmployee", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateEmployee(
        id: Int,
        name: String,
        email: String,
        phone: String,
        designation: String,
        department: String,
        profilePhotoUri: String? = null
    ): Result<String> {
        return try {
            val employee = employeeRepo.getEmployeeById(id.toString())
            if (employee == null) {
                return Result.failure(Exception("Employee not found"))
            }
            
            val updatedEmployee = employee.copy(
                name = name,
                email = email,
                phone = phone,
                designation = designation,
                department = department,
                profilePhotoUri = profilePhotoUri ?: employee.profilePhotoUri
            )
            
            employeeRepo.updateEmployee(id.toString(), updatedEmployee)
            Result.success("Employee updated successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deactivateEmployee(employeeId: Int): Result<String> {
        return try {
            val employee = employeeRepo.getEmployeeById(employeeId.toString())
            if (employee == null) {
                return Result.failure(Exception("Employee not found"))
            }
            
            val updatedEmployee = employee.copy(isActive = false)
            employeeRepo.updateEmployee(employeeId.toString(), updatedEmployee)
            
            // Send notification to employee
            val notification = FirebaseNotification(
                userId = employee.userId,
                title = "Account Deactivated",
                message = "Your employee account has been deactivated. Please contact HR for details.",
                type = "System",
                isRead = false
            )
            notificationRepo.sendNotification(notification)
            
            Result.success("Employee deactivated successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun activateEmployee(employeeId: Int): Result<String> {
        return try {
            val employee = employeeRepo.getEmployeeById(employeeId.toString())
            if (employee == null) {
                return Result.failure(Exception("Employee not found"))
            }
            
            val updatedEmployee = employee.copy(isActive = true)
            employeeRepo.updateEmployee(employeeId.toString(), updatedEmployee)
            
            Result.success("Employee activated successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteEmployee(employeeId: Int): Result<String> {
        return try {
            employeeRepo.deleteEmployee(employeeId.toString())
            Result.success("Employee deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAllEmployees(): Result<String> {
        return try {
            val result = FirebaseManager.employeeRepository.deleteAllEmployees()
            result.fold(
                onSuccess = { count ->
                    Result.success("Successfully deleted $count employees")
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length >= 5
    }
}
