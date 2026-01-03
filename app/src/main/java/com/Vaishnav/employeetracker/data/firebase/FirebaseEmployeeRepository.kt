package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseEmployeeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val employeesCollection = db.collection("employees")

    // Add new employee
    suspend fun addEmployee(employee: FirebaseEmployee): Result<String> {
        return try {
            // Verify Firebase is initialized
            val firebaseApp = FirebaseApp.getInstance()
            android.util.Log.d("FirebaseEmployeeRepo", "Firebase App: ${firebaseApp.name}, Options: ${firebaseApp.options.projectId}")
            
            android.util.Log.d("FirebaseEmployeeRepo", "Attempting to add employee: ${employee.name} (${employee.employeeId})")
            android.util.Log.d("FirebaseEmployeeRepo", "Employee data: userId=${employee.userId}, email=${employee.email}, active=${employee.isActive}")
            
            // Create a map to ensure all fields are saved
            val employeeData = hashMapOf(
                "name" to employee.name,
                "email" to employee.email,
                "phone" to employee.phone,
                "designation" to employee.designation,
                "department" to employee.department,
                "joiningDate" to employee.joiningDate,
                "employeeId" to employee.employeeId,
                "userId" to employee.userId,
                "role" to employee.role,
                "addedBy" to employee.addedBy,
                "isActive" to employee.isActive,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            android.util.Log.d("FirebaseEmployeeRepo", "Employee data map: $employeeData")
            
            val docRef = employeesCollection.add(employeeData).await()
            
            android.util.Log.d("FirebaseEmployeeRepo", "Successfully added employee with Firestore document ID: ${docRef.id}")
            android.util.Log.d("FirebaseEmployeeRepo", "Document path: ${docRef.path}")
            
            // Verify the document was created
            val verifyDoc = docRef.get().await()
            android.util.Log.d("FirebaseEmployeeRepo", "Verification - Document exists: ${verifyDoc.exists()}")
            android.util.Log.d("FirebaseEmployeeRepo", "Verification - userId field: ${verifyDoc.getString("userId")}")
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseEmployeeRepo", "Failed to add employee to Firebase", e)
            android.util.Log.e("FirebaseEmployeeRepo", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("FirebaseEmployeeRepo", "Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    // Update employee
    suspend fun updateEmployee(employeeId: String, employee: FirebaseEmployee): Result<Unit> {
        return try {
            employeesCollection.document(employeeId).set(employee).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete employee
    suspend fun deleteEmployee(employeeId: String): Result<Unit> {
        return try {
            employeesCollection.document(employeeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete all employees
    suspend fun deleteAllEmployees(): Result<Int> {
        return try {
            android.util.Log.d("FirebaseEmployeeRepo", "Starting to delete all employees")
            val snapshot = employeesCollection.get().await()
            val deletedCount = snapshot.documents.size
            
            android.util.Log.d("FirebaseEmployeeRepo", "Found $deletedCount employees to delete")
            
            // Delete each employee document
            snapshot.documents.forEach { document ->
                document.reference.delete().await()
                android.util.Log.d("FirebaseEmployeeRepo", "Deleted employee: ${document.id}")
            }
            
            android.util.Log.d("FirebaseEmployeeRepo", "Successfully deleted all $deletedCount employees")
            Result.success(deletedCount)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseEmployeeRepo", "Failed to delete all employees", e)
            Result.failure(e)
        }
    }

    // Get employee by ID
    suspend fun getEmployeeById(employeeId: String): FirebaseEmployee? {
        return try {
            employeesCollection.document(employeeId).get().await()
                .toObject(FirebaseEmployee::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get employee by Firebase Auth userId
    suspend fun getEmployeeByUserId(userId: String): FirebaseEmployee? {
        return try {
            android.util.Log.d("FirebaseEmployeeRepo", "Querying employee with userId: $userId")
            val snapshot = employeesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                android.util.Log.e("FirebaseEmployeeRepo", "No employee found with userId: $userId")
                null
            } else {
                val doc = snapshot.documents.first()
                val employee = doc.toObject(FirebaseEmployee::class.java)?.copy(id = doc.id)
                android.util.Log.d("FirebaseEmployeeRepo", "Found employee: ${employee?.name} (${employee?.employeeId}) - Doc ID: ${employee?.id}")
                employee
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseEmployeeRepo", "Error getting employee by userId", e)
            null
        }
    }

    // Get all active employees as Flow
    fun getAllActiveEmployees(): Flow<List<FirebaseEmployee>> = callbackFlow {
        android.util.Log.d("FirebaseEmployeeRepo", "Setting up getAllActiveEmployees listener...")
        val listener = employeesCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseEmployeeRepo", "❌ Error in getAllActiveEmployees listener", error)
                    android.util.Log.e("FirebaseEmployeeRepo", "Error type: ${error.javaClass.simpleName}")
                    android.util.Log.e("FirebaseEmployeeRepo", "Error message: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                val employees = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseEmployee::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                android.util.Log.d("FirebaseEmployeeRepo", "✅ Loaded ${employees.size} active employees from Firebase")
                employees.forEachIndexed { index, emp ->
                    android.util.Log.d("FirebaseEmployeeRepo", "  Employee $index: ${emp.name} (${emp.employeeId}) - Doc ID: ${emp.id}, UserId: ${emp.userId}")
                }
                trySend(employees)
            }
        awaitClose { listener.remove() }
    }

    // Get employees added by specific admin
    fun getEmployeesByAdminId(adminId: String): Flow<List<FirebaseEmployee>> = callbackFlow {
        android.util.Log.d("FirebaseEmployeeRepo", "Setting up getEmployeesByAdminId listener for admin: $adminId")
        val listener = employeesCollection
            .whereEqualTo("isActive", true)
            .whereEqualTo("addedBy", adminId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseEmployeeRepo", "❌ Error in getEmployeesByAdminId listener", error)
                    close(error)
                    return@addSnapshotListener
                }
                val employees = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseEmployee::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                android.util.Log.d("FirebaseEmployeeRepo", "✅ Loaded ${employees.size} employees for admin $adminId")
                employees.forEachIndexed { index, emp ->
                    android.util.Log.d("FirebaseEmployeeRepo", "  Employee $index: ${emp.name} (${emp.employeeId}) - AddedBy: ${emp.addedBy}")
                }
                trySend(employees)
            }
        awaitClose { listener.remove() }
    }

    // Search employees by name
    fun searchEmployees(query: String): Flow<List<FirebaseEmployee>> = callbackFlow {
        android.util.Log.d("FirebaseEmployeeRepo", "Searching employees with query: $query")
        val listener = employeesCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseEmployeeRepo", "Search error", error)
                    close(error)
                    return@addSnapshotListener
                }
                val employees = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseEmployee::class.java)?.copy(id = doc.id)
                }?.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true) ||
                    it.employeeId.contains(query, ignoreCase = true) ||
                    it.department.contains(query, ignoreCase = true)
                } ?: emptyList()
                android.util.Log.d("FirebaseEmployeeRepo", "Found ${employees.size} matching employees")
                trySend(employees)
            }
        awaitClose { listener.remove() }
    }

    // Get employees by department
    fun getEmployeesByDepartment(department: String): Flow<List<FirebaseEmployee>> = callbackFlow {
        val listener = employeesCollection
            .whereEqualTo("department", department)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val employees = snapshot?.toObjects(FirebaseEmployee::class.java) ?: emptyList()
                trySend(employees)
            }
        awaitClose { listener.remove() }
    }
}
