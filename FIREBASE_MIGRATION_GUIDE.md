# Firebase Migration Guide
**Date:** December 10, 2025  
**Project:** Employee Tracker App  
**Status:** ✅ Migration Architecture Complete

---

## Overview

This guide explains the complete migration from **Room Database (SQLite)** to **Firebase Firestore** for the Employee Tracker application.

---

## Architecture Changes

### Before (Room Database)
```
┌─────────────────┐
│   UI Screens    │
└────────┬────────┘
         │
┌────────▼────────┐
│   ViewModels    │
└────────┬────────┘
         │
┌────────▼────────┐
│      DAOs       │ (11 DAOs)
└────────┬────────┘
         │
┌────────▼────────┐
│  Room Database  │ (Local SQLite)
└─────────────────┘
```

### After (Firebase Firestore)
```
┌─────────────────┐
│   UI Screens    │
└────────┬────────┘
         │
┌────────▼────────┐
│   ViewModels    │
└────────┬────────┘
         │
┌────────▼────────┐
│  Repositories   │ (11 Firebase Repos)
└────────┬────────┘
         │
┌────────▼────────┐
│   Firestore     │ (Cloud NoSQL)
└─────────────────┘
```

---

## Completed Components

### ✅ 1. Firebase Models (16 Data Classes)
**File:** `FirebaseModels.kt`

All Room entities converted to Firebase-compatible data classes:

| Room Entity | Firebase Model | Key Changes |
|-------------|----------------|-------------|
| Employee | FirebaseEmployee | Int ID → String @DocumentId |
| Attendance | FirebaseAttendance | Long timestamp → Date @ServerTimestamp |
| Task | FirebaseTask | Foreign keys → String references |
| LeaveRequest | FirebaseLeaveRequest | Auto-increment removed |
| PerformanceRating | FirebasePerformanceRating | Timestamps converted |
| Notification | FirebaseNotification | Employee reference updated |
| Shift | FirebaseShift | Primary key changed |
| ShiftAssignment | FirebaseShiftAssignment | Multiple FK references |
| ShiftSwapRequest | FirebaseShiftSwapRequest | Request tracking |
| TimeLog | FirebaseTimeLog | Time tracking |
| BreakRecord | FirebaseBreakRecord | Break management |
| PayrollRecord | FirebasePayrollRecord | Financial data |
| Document | FirebaseDocument | File storage integration |
| Message | FirebaseMessage | Chat functionality |
| ChatGroup | FirebaseChatGroup | Group management |
| GroupMember | FirebaseGroupMember | Membership tracking |

---

### ✅ 2. Firebase Repositories (11 Classes)

All repositories implement consistent patterns:
- **Suspend functions** for write operations
- **Flow-based queries** for real-time data
- **Result<T> wrappers** for error handling
- **callbackFlow** for Firestore snapshot listeners

#### Repository List:

1. **FirebaseEmployeeRepository**
   - CRUD operations
   - Search by name
   - Filter by department
   - Active employees query

2. **FirebaseAttendanceRepository**
   - Check-in/check-out
   - Daily attendance tracking
   - Monthly statistics
   - Late arrival detection

3. **FirebaseTaskRepository**
   - Task assignment
   - Status updates (Pending, In Progress, Completed)
   - Priority filtering
   - Overdue task queries

4. **FirebaseLeaveRepository**
   - Leave request submission
   - Approval workflow
   - Leave balance calculation
   - Status tracking

5. **FirebasePerformanceRepository**
   - Performance ratings
   - Average calculations
   - Performance statistics
   - Rating history

6. **FirebaseNotificationRepository**
   - Send notifications
   - Mark as read
   - Bulk sending
   - Unread count tracking

7. **FirebaseShiftRepository**
   - Shift management
   - Shift assignments
   - Swap request handling
   - Schedule queries

8. **FirebaseTimeLogRepository**
   - Clock in/out
   - Break record management
   - Total hours calculation
   - Period-based queries

9. **FirebasePayrollRepository**
   - Payroll record management
   - Payment status updates
   - Earnings summary
   - Department payroll reports

10. **FirebaseDocumentRepository**
    - Document upload to Firebase Storage
    - Metadata management
    - File download
    - Storage usage tracking

11. **FirebaseMessageRepository**
    - Message sending
    - Chat group management
    - Group member operations
    - Real-time messaging

---

### ✅ 3. Firebase Authentication
**File:** `FirebaseAuthManager.kt`

Complete authentication system with:
- Email/password authentication
- User registration
- Password reset via email
- Profile management
- Role-based access (Admin/User)
- Employee account creation

#### Key Methods:
```kotlin
// Authentication
suspend fun login(email: String, password: String): Result<FirebaseUser>
suspend fun registerUser(email: String, password: String, displayName: String): Result<FirebaseUser>
fun logout()

// Password Management
suspend fun sendPasswordResetEmail(email: String): Result<Unit>
suspend fun changePassword(newPassword: String): Result<Unit>

// Profile Management
suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit>
suspend fun updateEmail(newEmail: String): Result<Unit>

// Admin Functions
suspend fun createEmployeeAccount(employeeId: String, fullName: String, email: String): Result<Pair<String, String>>

// User Info
fun getCurrentUser(): FirebaseUser?
fun getCurrentUserId(): String?
fun getUserRole(): String
fun isAdmin(): Boolean
```

---

### ✅ 4. Data Migration Utility
**File:** `DataMigrationUtility.kt`

Automated migration from Room to Firestore:
- Preserves data relationships
- Maps Room Int IDs to Firebase String IDs
- Handles all 16 entity types
- Provides detailed migration report
- Error tracking per entity

#### Migration Process:
1. Migrate Employees (base entity)
2. Migrate Attendance records
3. Migrate Tasks
4. Migrate Leave requests
5. Migrate Performance ratings
6. Migrate Notifications
7. Migrate Shifts & Assignments
8. Migrate Time logs
9. Migrate Payroll records
10. Migrate Documents (metadata only)
11. Migrate Messages & Chat groups

#### Usage:
```kotlin
val migrationUtility = DataMigrationUtility(context)
val result = migrationUtility.migrateAllData()

result.onSuccess { report ->
    println("Migrated: ${report.getTotalSuccess()}/${report.getTotalRecords()}")
}
```

---

### ✅ 5. Migration Admin Screen
**File:** `MigrationScreen.kt`

UI for administrators to trigger migration:
- Warning messages
- Migration progress indicator
- Detailed migration report
- Success/error feedback
- Entity-by-entity status

---

## Next Steps: ViewModel Updates

### ViewModels to Update (10 files)

Each ViewModel needs to replace Room DAO with Firebase Repository:

#### 1. **EmployeeViewModel.kt**
```kotlin
// Before
private val employeeDao = database.employeeDao()

// After
private val employeeRepo = FirebaseEmployeeRepository()
```

#### 2. **AttendanceViewModel.kt**
```kotlin
// Before
private val attendanceDao = database.attendanceDao()

// After
private val attendanceRepo = FirebaseAttendanceRepository()
```

#### 3. **TaskViewModel.kt**
- Replace `taskDao` with `FirebaseTaskRepository`
- Update Flow queries to use repository methods
- Handle Result<T> wrappers

#### 4. **LeaveViewModel.kt**
- Replace `leaveRequestDao` with `FirebaseLeaveRepository`
- Update approval workflow
- Handle leave balance calculations

#### 5. **PerformanceViewModel.kt**
- Replace `performanceRatingDao` with `FirebasePerformanceRepository`
- Update rating queries

#### 6. **NotificationViewModel.kt**
- Replace `notificationDao` with `FirebaseNotificationRepository`
- Update real-time notification handling

#### 7. **ShiftViewModel.kt**
- Replace `shiftDao` with `FirebaseShiftRepository`
- Update shift assignment logic
- Handle swap requests

#### 8. **TimeLogViewModel.kt**
- Replace `timeLogDao` with `FirebaseTimeLogRepository`
- Update clock in/out operations

#### 9. **PayrollViewModel.kt**
- Replace `payrollDao` with `FirebasePayrollRepository`
- Update payroll calculations

#### 10. **DocumentViewModel.kt**
- Replace `documentDao` with `FirebaseDocumentRepository`
- Integrate Firebase Storage for file uploads

---

## Firebase Configuration Required

### 1. Add google-services.json
Place your Firebase project's `google-services.json` in:
```
app/google-services.json
```

### 2. Firebase Console Setup
1. Create Firebase project
2. Enable **Firebase Authentication** (Email/Password)
3. Enable **Cloud Firestore**
4. Enable **Firebase Storage**
5. Enable **Firebase Cloud Messaging** (optional)

### 3. Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own data
    match /employees/{employeeId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        (request.auth.token.admin == true || request.auth.uid == employeeId);
    }
    
    match /attendance/{attendanceId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    match /tasks/{taskId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    match /leave_requests/{leaveId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    // Add similar rules for other collections
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 4. Firebase Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /documents/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    match /profile_photos/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

---

## Migration Checklist

### Pre-Migration
- [ ] Set up Firebase project
- [ ] Add google-services.json
- [ ] Enable Authentication, Firestore, Storage
- [ ] Configure security rules
- [ ] Test Firebase connection

### Migration
- [ ] Backup existing Room database
- [ ] Run migration utility from admin screen
- [ ] Verify migration report (check success/failure counts)
- [ ] Manually review critical data in Firebase Console

### Post-Migration
- [ ] Update all ViewModels to use Firebase repositories
- [ ] Test CRUD operations for each entity
- [ ] Test real-time data updates
- [ ] Test authentication flow
- [ ] Test file uploads to Storage
- [ ] Remove Room database dependencies (optional)

---

## Key Differences: Room vs Firebase

| Aspect | Room | Firebase |
|--------|------|----------|
| **Database Type** | SQLite (Relational) | NoSQL (Document-based) |
| **Data Location** | Local device | Cloud |
| **Primary Keys** | Auto-increment Int | Auto-generated String |
| **Relationships** | Foreign keys | Document references |
| **Queries** | SQL with @Query | Firestore queries |
| **Offline Support** | Built-in | Requires enabling |
| **Real-time Updates** | Flow with Room | Snapshot listeners |
| **Data Sync** | Manual | Automatic |
| **Scalability** | Limited to device | Unlimited cloud storage |

---

## Benefits of Firebase Migration

### 1. **Real-time Synchronization**
- Data updates instantly across all devices
- No manual sync required
- Live updates in UI

### 2. **Cloud Backup**
- Data stored in Google Cloud
- No data loss if device is damaged
- Accessible from anywhere

### 3. **Scalability**
- Handles millions of records
- No device storage constraints
- Automatic scaling

### 4. **Multi-device Access**
- Same account accessible from multiple devices
- Seamless data sync
- Cross-platform support

### 5. **Advanced Features**
- Cloud Functions for server-side logic
- Firebase Analytics integration
- Push notifications via FCM
- Machine Learning integration

---

## Testing Guidelines

### 1. Authentication Testing
```kotlin
// Test login
val result = FirebaseAuthManager.getInstance().login(
    email = "test@example.com",
    password = "password123"
)

// Test registration
val registerResult = FirebaseAuthManager.getInstance().registerUser(
    email = "new@example.com",
    password = "newpass123",
    displayName = "Test User"
)
```

### 2. CRUD Testing
```kotlin
// Test employee creation
val employee = FirebaseEmployee(name = "John Doe", email = "john@example.com")
val result = FirebaseEmployeeRepository().addEmployee(employee)

result.onSuccess { employeeId ->
    println("Created employee with ID: $employeeId")
}

// Test real-time query
FirebaseEmployeeRepository().getAllActiveEmployees()
    .collect { employees ->
        println("Active employees: ${employees.size}")
    }
```

### 3. Migration Testing
- Create test data in Room database
- Run migration utility
- Verify all records in Firebase Console
- Check ID mappings are correct
- Test queries return expected data

---

## Troubleshooting

### Issue: "Google services not found"
**Solution:** Ensure `google-services.json` is in `app/` directory and plugin is applied

### Issue: "Permission denied" errors
**Solution:** Update Firestore security rules to allow authenticated users

### Issue: "Migration fails for some records"
**Solution:** Check migration report errors, verify employee ID mapping exists

### Issue: "Real-time updates not working"
**Solution:** Ensure snapshot listeners are properly set up in repositories

---

## Performance Considerations

### 1. **Indexing**
Create Firestore indexes for frequently queried fields:
- `employees` collection: index on `department`, `isActive`
- `tasks` collection: index on `assignedToId`, `status`, `dueDate`
- `attendance` collection: index on `employeeId`, `date`

### 2. **Offline Persistence**
Enable offline persistence for better UX:
```kotlin
val settings = FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .build()
FirebaseFirestore.getInstance().firestoreSettings = settings
```

### 3. **Query Optimization**
- Use `limit()` for large result sets
- Use pagination with `startAfter()`
- Avoid deep queries with multiple `whereEqualTo()`

---

## Future Enhancements

1. **Cloud Functions**
   - Automatic payroll calculations
   - Email notifications
   - Data validation

2. **Firebase ML**
   - Face recognition for attendance
   - Document classification

3. **Analytics**
   - User behavior tracking
   - Feature usage analytics

4. **A/B Testing**
   - UI variations testing
   - Feature rollouts

---

## Summary

✅ **Migration Architecture: Complete**
- 16 Firebase data models created
- 11 Firebase repositories implemented
- Firebase Authentication integrated
- Data migration utility built
- Migration admin screen created

🔄 **Next Phase: ViewModel Updates**
- Update 10 ViewModels to use Firebase repositories
- Replace Room DAOs with repository pattern
- Test all CRUD operations
- Verify real-time updates

📊 **Expected Migration Time:**
- Small dataset (< 1000 records): 2-5 minutes
- Medium dataset (1000-10000 records): 10-20 minutes
- Large dataset (> 10000 records): 30+ minutes

---

**Document Version:** 1.0  
**Last Updated:** December 10, 2025  
**Status:** Ready for ViewModel migration phase
