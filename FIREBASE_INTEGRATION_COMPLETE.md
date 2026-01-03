# Firebase Integration Guide

## Overview
Your Employee Tracker app now has complete Firebase integration! The app uses a **hybrid approach** that allows switching between Room (local database) and Firebase (cloud database).

## Current Status

### ✅ Completed
- Firebase SDK initialized in MainActivity
- FirebaseManager singleton created with all repositories
- All Firebase models aligned with Room entities
- 11 Firebase repositories with full CRUD operations
- Data migration utility (611 lines)
- Migration admin screen with UI
- Firestore persistence and caching configured
- Auto-enable Firebase after successful migration

### 🔄 Hybrid Database Architecture

The app uses **both Room and Firebase** simultaneously:

**Room Database (Local)**
- Default data source
- Fast, reliable, works offline
- Used until migration is complete

**Firebase Firestore (Cloud)**
- Optional data source
- Real-time sync, cloud backup
- Enabled after migration

## How to Use Firebase

### Option 1: Migrate Existing Data (Recommended)

1. **Run the app** and login as Admin
2. **Go to Admin Dashboard** → "Data Migration"
3. **Click "Start Migration"** to transfer all Room data to Firebase
4. **Wait for completion** - Firebase will be automatically enabled
5. **Done!** The app now uses Firebase for all operations

### Option 2: Start Fresh with Firebase

If you want to start with an empty Firebase database:

```kotlin
// In MainActivity.onCreate() or anywhere before using the app
FirebaseManager.enableFirebase()
```

## Switching Between Room and Firebase

You can toggle between Room and Firebase at runtime:

```kotlin
// Use Firebase
FirebaseManager.enableFirebase()

// Use Room
FirebaseManager.disableFirebase()

// Check current status
val isUsingFirebase = FirebaseManager.isFirebaseEnabled()
```

## Firebase Console Setup

Before using Firebase, ensure these services are enabled:

1. **Firestore Database**
   - Go to Firebase Console → Firestore Database
   - Click "Create Database"
   - Choose "Start in Test Mode" (for development)
   - Select a location

2. **Authentication**
   - Go to Firebase Console → Authentication
   - Click "Get Started"
   - Enable "Email/Password" provider

3. **Cloud Storage**
   - Go to Firebase Console → Storage
   - Click "Get Started"
   - Choose "Start in Test Mode" (for development)

4. **Security Rules** (Production)
   
   **Firestore Rules:**
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       // Allow authenticated users to read/write their own data
       match /{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```
   
   **Storage Rules:**
   ```javascript
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /{allPaths=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

## Using Firebase Repositories

### Example: Access Firebase Repositories

```kotlin
// In your Activity or ViewModel
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager

// Get repository instance
val employeeRepo = FirebaseManager.employeeRepository

// Use repository methods
lifecycleScope.launch {
    // Get all employees
    employeeRepo.getAllEmployees().collect { employees ->
        // Update UI
    }
    
    // Add new employee
    val result = employeeRepo.addEmployee(firebaseEmployee)
    result.onSuccess { id ->
        // Success
    }.onFailure { error ->
        // Handle error
    }
}
```

## Available Firebase Repositories

All repositories are available through `FirebaseManager`:

```kotlin
FirebaseManager.employeeRepository      // Employee operations
FirebaseManager.taskRepository          // Task management
FirebaseManager.attendanceRepository    // Attendance tracking
FirebaseManager.leaveRepository         // Leave requests
FirebaseManager.notificationRepository  // Notifications
FirebaseManager.shiftRepository         // Shift management
FirebaseManager.timeLogRepository       // Time tracking
FirebaseManager.payrollRepository       // Payroll records
FirebaseManager.documentRepository      // Document storage
FirebaseManager.performanceRepository   // Performance ratings
FirebaseManager.messageRepository       // Chat & messaging
FirebaseManager.authManager             // Firebase Authentication
```

## Migration Details

The migration utility transfers these entities:

1. **Employees** - All employee records with accounts
2. **Attendance** - Complete attendance history
3. **Tasks** - All tasks and assignments
4. **Leave Requests** - Leave history and status
5. **Performance Ratings** - All performance reviews
6. **Notifications** - All notifications
7. **Shifts & Assignments** - Shift schedules
8. **Time Logs** - Work hour tracking
9. **Payroll** - Payment records
10. **Documents** - Document metadata (files uploaded separately)
11. **Chat Groups & Messages** - All conversations

### Migration Process

```kotlin
val migrationUtility = DataMigrationUtility(context)

// Migrate all data
val result = migrationUtility.migrateAllData()

result.onSuccess { report ->
    println("Migrated: ${report.getTotalSuccess()} records")
    println("Failed: ${report.getTotalFailed()} records")
    
    // View detailed report
    println("Employees: ${report.employeesResult.success}/${report.employeesResult.total}")
    println("Tasks: ${report.tasksResult.success}/${report.tasksResult.total}")
    // ... etc
}
```

## Updating ViewModels to Use Firebase

Currently, ViewModels use Room DAOs. To use Firebase, you have two options:

### Option A: Check FirebaseManager.useFirebase flag

```kotlin
class EmployeeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val employeeDao = database.employeeDao()
    
    fun getAllActiveEmployees(): Flow<List<Employee>> {
        return if (FirebaseManager.useFirebase) {
            // Use Firebase
            FirebaseManager.employeeRepository.getAllEmployees()
        } else {
            // Use Room
            employeeDao.getAllActiveEmployees()
        }
    }
}
```

### Option B: Replace Room with Firebase

```kotlin
class EmployeeViewModel(application: Application) : AndroidViewModel(application) {
    // Remove Room
    // private val database = AppDatabase.getDatabase(application)
    // private val employeeDao = database.employeeDao()
    
    // Use Firebase
    private val employeeRepo = FirebaseManager.employeeRepository
    
    fun getAllActiveEmployees(): Flow<List<Employee>> {
        return employeeRepo.getAllEmployees()
    }
}
```

## Real-time Updates

Firebase provides real-time updates automatically:

```kotlin
// This Flow updates automatically when Firebase data changes
FirebaseManager.employeeRepository.getAllEmployees()
    .collect { employees ->
        // UI updates automatically when data changes in Firebase
        updateUI(employees)
    }
```

## Offline Support

Firebase has built-in offline persistence:

- ✅ Writes are cached locally when offline
- ✅ Reads come from cache when offline
- ✅ Syncs automatically when back online
- ✅ Conflicts resolved automatically

## Best Practices

1. **Always check migration status** before enabling Firebase
2. **Test with small dataset first** before migrating production data
3. **Keep Room database** as backup (don't delete)
4. **Monitor Firebase usage** in Firebase Console (quotas, costs)
5. **Use security rules** in production
6. **Enable authentication** before production deployment

## Troubleshooting

### "Firebase not initialized"
- Ensure `google-services.json` is in `app/` directory
- Clean and rebuild project

### "Permission denied" errors
- Check Firestore security rules
- Ensure user is authenticated

### "Offline mode" issues
- Firebase persistence is already enabled
- Data should work offline automatically

### Migration fails
- Check Firebase Console for errors
- Ensure all Firebase services are enabled
- Check internet connection
- Review logcat for detailed errors

## Next Steps

1. ✅ Firebase is initialized
2. ⏳ Run migration from Admin Dashboard
3. ⏳ Test CRUD operations
4. ⏳ Update ViewModels to use Firebase (optional)
5. ⏳ Configure production security rules
6. ⏳ Enable Firebase Authentication
7. ⏳ Test real-time sync across devices

## Firebase Console Links

- **Project Overview:** https://console.firebase.google.com/project/employee-tracker-6972f
- **Firestore Database:** https://console.firebase.google.com/project/employee-tracker-6972f/firestore
- **Authentication:** https://console.firebase.google.com/project/employee-tracker-6972f/authentication
- **Storage:** https://console.firebase.google.com/project/employee-tracker-6972f/storage

## Code References

- **FirebaseManager:** `app/src/main/java/com/Vaishnav/employeetracker/data/firebase/FirebaseManager.kt`
- **Migration Utility:** `app/src/main/java/com/Vaishnav/employeetracker/data/firebase/DataMigrationUtility.kt`
- **Migration Screen:** `app/src/main/java/com/Vaishnav/employeetracker/ui/screens/admin/MigrationScreen.kt`
- **Firebase Models:** `app/src/main/java/com/Vaishnav/employeetracker/data/firebase/FirebaseModels.kt`
- **All Repositories:** `app/src/main/java/com/Vaishnav/employeetracker/data/firebase/Firebase*Repository.kt`

---

**Status:** Firebase is initialized and ready. Run migration from Admin Dashboard to start using Firebase! 🚀
