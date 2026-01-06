package com.Vaishnav.employeetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.Vaishnav.employeetracker.navigation.NavigationGraph
import com.Vaishnav.employeetracker.navigation.Screen
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseEmployee
import com.Vaishnav.employeetracker.data.firebase.FirebaseAuthManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseInitializer
import com.Vaishnav.employeetracker.ui.theme.EmployeeTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Configure Firestore settings
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
        
        android.util.Log.d("MainActivity", "Firebase initialized: ${FirebaseApp.getInstance().name}")
        android.util.Log.d("MainActivity", "Firestore instance created successfully")
        
        // TEST: Direct Firebase write to verify connectivity
        testFirebaseConnection()
        
        // TEST: Read back employees from Firebase
        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "========== TESTING FIREBASE READ ==========")
                val employeesSnapshot = firestore.collection("employees").get().await()
                android.util.Log.d("MainActivity", "Found ${employeesSnapshot.documents.size} documents in employees collection")
                employeesSnapshot.documents.forEachIndexed { index, doc ->
                    android.util.Log.d("MainActivity", "Document $index:")
                    android.util.Log.d("MainActivity", "  ID: ${doc.id}")
                    android.util.Log.d("MainActivity", "  Data: ${doc.data}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error reading employees", e)
            }
        }
        
        // Initialize FirebaseManager
        FirebaseManager.initialize(this)
        
        // Enable Firebase (Room has been removed)
        FirebaseManager.enableFirebase()
        
        android.util.Log.d("MainActivity", "FirebaseManager enabled: ${FirebaseManager.useFirebase}")
        
        // Initialize AuthManager with context
        AuthManager.initialize(applicationContext)
        
        // Initialize FirebaseAuthManager
        FirebaseAuthManager.getInstance().initialize(applicationContext)
        
        // Initialize Firebase structures (company group, etc.)
        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "Initializing Firebase structures...")
                FirebaseInitializer.ensureCompanyGroupExists()
                android.util.Log.d("MainActivity", "Firebase structures initialized")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error initializing Firebase structures", e)
            }
        }
        
        // Initialize default Firebase employees if needed
        initializeDefaultEmployees()
        
        // Check if first time user
        val isFirstTime = PreferencesManager.isFirstTime(this)
        if (isFirstTime) {
            PreferencesManager.setFirstTimeDone(this)
        }
        
        // Check if user is already logged in
        val isLoggedIn = PreferencesManager.isLoggedIn(this)
        val loggedInUsername = PreferencesManager.getLoggedInUsername(this)
        
        // Restore user session
        if (isLoggedIn && loggedInUsername != null) {
            val user = AuthManager.getUserByUsername(loggedInUsername)
            if (user != null) {
                AuthManager.login(user)
            }
        }
        
        setContent {
            EmployeeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavigationGraph(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        isFirstTime = isFirstTime,
                        isLoggedIn = isLoggedIn,
                        userRole = AuthManager.getUserRole(),
                        context = this@MainActivity
                    )
                }
            }
        }
    }
    
    private fun testFirebaseConnection() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("FirebaseTest", "========== FIREBASE CONNECTION TEST START ==========")
                
                val testData = hashMapOf(
                    "test" to "Firebase Connection Test",
                    "timestamp" to System.currentTimeMillis(),
                    "device" to "Android"
                )
                
                val firestore = FirebaseFirestore.getInstance()
                android.util.Log.d("FirebaseTest", "Firestore instance: ${firestore.app.name}")
                
                // Try to write to a test collection
                firestore.collection("test_connection")
                    .add(testData)
                    .addOnSuccessListener { documentReference ->
                        android.util.Log.d("FirebaseTest", "✅ SUCCESS! Document written with ID: ${documentReference.id}")
                        android.util.Log.d("FirebaseTest", "Document path: ${documentReference.path}")
                        android.util.Log.d("FirebaseTest", "========== FIREBASE IS WORKING! ==========")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FirebaseTest", "❌ FAILED! Error writing document", e)
                        android.util.Log.e("FirebaseTest", "Error type: ${e.javaClass.simpleName}")
                        android.util.Log.e("FirebaseTest", "Error message: ${e.message}")
                        android.util.Log.e("FirebaseTest", "========== FIREBASE CONNECTION FAILED ==========")
                    }
                
            } catch (e: Exception) {
                android.util.Log.e("FirebaseTest", "❌ Exception during Firebase test", e)
            }
        }
    }
    
    private fun initializeDefaultEmployees() {
        lifecycleScope.launch {
            // Initialize default employees in Firebase if not exists
            try {
                val employeeRepo = FirebaseManager.employeeRepository
                
                // Get all employees - take first emission only
                val employees = employeeRepo.getAllActiveEmployees().first()
                
                android.util.Log.d("MainActivity", "Found ${employees.size} employees in Firebase")
                
                if (employees.isEmpty()) {
                    android.util.Log.d("MainActivity", "No employees found - checking Firebase Auth users...")
                    
                    // Try to recover employees from Firebase Auth users
                    val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val currentUser = firebaseAuth.currentUser
                    
                    if (currentUser != null) {
                        android.util.Log.d("MainActivity", "Found logged-in user: ${currentUser.email}")
                        android.util.Log.d("MainActivity", "Creating employee document for UID: ${currentUser.uid}")
                        
                        // Create employee document for current logged-in user
                        val employee = FirebaseEmployee(
                            name = currentUser.displayName ?: "User",
                            email = currentUser.email ?: "",
                            phone = currentUser.phoneNumber ?: "",
                            designation = "Employee",
                            department = "General",
                            joiningDate = System.currentTimeMillis(),
                            employeeId = "EMP${System.currentTimeMillis() % 10000}",
                            userId = currentUser.uid, // Link to Firebase Auth user
                            role = "USER", // Default role
                            addedBy = "", // Self-registered
                            isActive = true,
                            createdAt = com.google.firebase.Timestamp.now().toDate(),
                            updatedAt = com.google.firebase.Timestamp.now().toDate()
                        )
                        
                        val result = employeeRepo.addEmployee(employee)
                        if (result.isSuccess) {
                            android.util.Log.d("MainActivity", "✅ Employee document recreated with ID: ${result.getOrNull()}")
                            android.util.Log.d("MainActivity", "You can now use the app normally!")
                        } else {
                            android.util.Log.e("MainActivity", "❌ Failed to create employee document", result.exceptionOrNull())
                        }
                    } else {
                        android.util.Log.d("MainActivity", "No user logged in. Please sign up again to create employee documents.")
                    }
                } else {
                    android.util.Log.d("MainActivity", "Employees already exist, skipping initialization")
                    employees.forEach { emp ->
                        android.util.Log.d("MainActivity", "Employee: ${emp.name} (${emp.email}) - UID: ${emp.userId}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error initializing Firebase employees", e)
            }
        }
    }
}