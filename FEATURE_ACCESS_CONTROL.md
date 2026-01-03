# Feature Access Control - Employee vs Admin

## Overview
This document clarifies the access control and feature visibility for **Employees** and **Admins** in the Employee Tracker application.

---

## 🔐 Role-Based Access Summary

### Employee Features (Limited to Own Data)
Employees have access to **personal** features only. They can view and manage their own information but cannot access other employees' data.

### Admin Features (Full Access)
Admins have access to **all employees' data** and can perform management operations across the entire organization.

---

## 📊 Feature-by-Feature Breakdown

### 1. **Analytics** 📈
- **Route**: `analytics`
- **Parameter**: `isAdmin` (automatically detected via `AuthManager.isAdmin()`)

#### Employee View:
- Personal performance metrics
- Own attendance trends
- Personal task completion stats
- Individual monthly trends

#### Admin View:
- Organization-wide analytics
- All employee performance metrics
- Department-wise attendance statistics
- Aggregate task completion rates
- Monthly attendance trends across all employees
- Department distribution charts

---

### 2. **Payroll** 💰
- **Route**: `payroll/{employeeId}`
- **Parameters**: `employeeId`, `isAdmin`
- **Admin View All**: Use `employeeId = 0` to view all employees' payroll records

#### Employee View:
- View **own** salary details (navigates with own employeeId)
- See **own** payroll history
- View breakdown of: Base salary, overtime, bonuses, deductions
- Check payment status (Pending/Paid)
- **Cannot generate payroll**
- **Cannot mark as paid**

#### Admin View:
- When accessed from Admin Dashboard: Shows **all employees' payroll records** (employeeId = 0)
- Can navigate from Employee Directory to view specific employee's payroll
- Generate new payroll records for any employee
- Mark payroll as paid
- Full access to all payroll operations
- Can specify: month, year, base salary, working days, bonuses, deductions

---

### 3. **Shift Management** 🗓️
- **Route**: `shift_management/{employeeId}`
- **Parameters**: `employeeId`, `isAdmin`
- **Admin View All**: Use `employeeId = 0` to view all shifts across organization

#### Employee View:
- View **own** shift schedule (navigates with own employeeId)
- Request shift swaps
- View swap request status
- See calendar with own shifts highlighted
- **Cannot create new shifts**
- **Cannot approve/reject swap requests**

#### Admin View:
- When accessed from Admin Dashboard: Shows **all employee shifts** (employeeId = 0)
- View organization-wide shift calendar with all employees
- Create new shifts for any employee
- Approve or reject shift swap requests
- Add admin remarks to swap decisions
- Full calendar management
- Manage shift assignments organization-wide

---

### 4. **Documents** 📄
- **Route**: `documents/{employeeId}`
- **Parameters**: `employeeId`, `isAdmin`
- **Note**: Admin should use Employee Directory to select employee first

#### Employee View:
- View **own** documents only (navigates with own employeeId)
- Upload personal documents (ID Proof, Certificates, Contracts, Resume)
- Delete own documents
- View document expiry dates
- Filter by document type
- **Cannot view other employees' documents**

#### Admin View:
- Click "Documents" in Admin Dashboard → Redirects to Employee Directory
- Select employee from directory → View that employee's documents
- Upload documents for any employee
- Delete any employee's documents
- Manage document expiry tracking
- Full access to all document operations across organization
- Can mark documents as expired

---

### 5. **Messaging** 💬
- **Route**: `messaging`
- **Parameter**: `currentUserId` (automatically set via `AuthManager.getCurrentUserId()`)

#### Employee View:
- Send messages to admins and other employees
- View own conversations
- See unread message counts
- Mark messages as read
- Group chat participation (if enabled)

#### Admin View:
- Same messaging capabilities as employees
- Can broadcast to multiple employees (future feature)
- Access to all conversations (if needed for monitoring)

---

### 6. **Biometric Authentication** 🔐
- **Route**: `biometric_setup`
- **Parameters**: None (uses device context)

#### Both Employee and Admin:
- Set up fingerprint authentication
- Enable face recognition
- Configure biometric security
- Skip setup if preferred

---

## 🎯 Core Application Features

### Employee-Only Features:
1. **Employee Dashboard** - Personal overview
   - Attendance summary (own)
   - Leave balance (own)
   - Task list (assigned to them)
   - Quick check-in/check-out

2. **Mark Attendance** - Self check-in/out
3. **Apply for Leave** - Submit leave requests
4. **View Tasks** - Assigned tasks only

### Admin-Only Features:
1. **Admin Dashboard** - Organization overview
   - Live attendance counter (all employees)
   - Department statistics
   - Leave approval queue
   - Task assignment overview

2. **Attendance Monitoring** - View all employee attendance
3. **Employee Directory** - Manage all employees
4. **Task Assignment** - Create and assign tasks
5. **Leave Approval** - Approve/reject leave requests
6. **Reports** - Generate organization reports

---

## 🛠️ Implementation Details

### How `isAdmin` Flag Works:

```kotlin
// In NavigationGraph.kt
composable(Screen.Analytics.route) {
    AnalyticsScreen(
        isAdmin = AuthManager.isAdmin(),
        onNavigateBack = { navController.popBackStack() }
    )
}

composable(
    route = Screen.Payroll.route,
    arguments = listOf(navArgument("employeeId") { type = NavType.IntType })
) { backStackEntry ->
    val employeeId = backStackEntry.arguments?.getInt("employeeId") ?: 0
    PayrollScreen(
        employeeId = employeeId,
        isAdmin = AuthManager.isAdmin(),
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### Navigation Pattern:

**From Employee Dashboard:**
```kotlin
// Employee navigates with their own ID
onNavigateToPayroll = { 
    navController.navigate(Screen.Payroll.createRoute(employeeId)) 
}

onNavigateToShiftManagement = { 
    navController.navigate(Screen.ShiftManagement.createRoute(employeeId)) 
}

onNavigateToDocuments = { 
    navController.navigate(Screen.Documents.createRoute(employeeId)) 
}
```

**From Admin Dashboard:**
```kotlin
// Admin views all payroll records (employeeId = 0)
onNavigateToPayroll = { 
    navController.navigate(Screen.Payroll.createRoute(0)) 
}

// Admin views all shifts (employeeId = 0)
onNavigateToShiftManagement = { 
    navController.navigate(Screen.ShiftManagement.createRoute(0)) 
}

// Admin goes to Employee Directory to select employee for documents
onNavigateToDocuments = { 
    navController.navigate(Screen.EmployeeDirectory.route) 
}

// Admin can also view their own:
onNavigateToPayroll = { 
    navController.navigate(Screen.Payroll.createRoute(AuthManager.getCurrentUserId())) 
}
```

---

## 🔒 Security Considerations

1. **Backend Validation**: Always validate user permissions on the server side, not just in UI
2. **Token-Based Auth**: Use JWT or similar for API calls
3. **Role Checking**: `AuthManager.isAdmin()` checks the logged-in user's role
4. **Data Filtering**: ViewModels should filter data based on user role
5. **UI Hiding**: Admin-only buttons hidden when `isAdmin = false`

---

## 📱 User Interface Differences

### Employee Dashboard Advanced Features Section:
```
┌─────────────────────────────────────┐
│     Advanced Features               │
├─────────────────┬───────────────────┤
│  📊 Analytics   │  💰 Payroll       │
│  (Personal)     │  (View Only)      │
├─────────────────┼───────────────────┤
│  🗓️ Shifts      │  📄 Documents     │
│  (Request Swap) │  (Own Docs)       │
├─────────────────┼───────────────────┤
│  💬 Messages    │                   │
│  (Send/Receive) │                   │
└─────────────────┴───────────────────┘
```

### Admin Dashboard Advanced Features Section:
```
┌─────────────────────────────────────┐
│     Advanced Features               │
├─────────────────┬───────────────────┤
│  📊 Analytics   │  💰 Payroll       │
│  (All Employees)│  (Generate/Pay)   │
├─────────────────┼───────────────────┤
│  🗓️ Shifts      │  📄 Documents     │
│  (Create/Manage)│  (All Employees)  │
├─────────────────┼───────────────────┤
│  💬 Messages    │                   │
│  (Send/Receive) │                   │
└─────────────────┴───────────────────┘
```

---

## ✅ Testing Checklist

### Employee Login Testing:
- [ ] Can view only own analytics
- [ ] Can view only own payroll
- [ ] Cannot generate payroll
- [ ] Cannot mark payroll as paid
- [ ] Can view only own shifts
- [ ] Can request shift swaps
- [ ] Cannot approve shift swaps
- [ ] Can view only own documents
- [ ] Can send messages

### Admin Login Testing:
- [ ] Can view all employee analytics
- [ ] Can view any employee's payroll
- [ ] Can generate payroll for any employee
- [ ] Can mark payroll as paid
- [ ] Can view all shifts
- [ ] Can create shifts for any employee
- [ ] Can approve/reject shift swaps
- [ ] Can view all employees' documents
- [ ] Can send messages

---

## 🚀 Future Enhancements

1. **Role Hierarchy**: Supervisor role between Employee and Admin
2. **Permission Granularity**: Fine-grained permissions (e.g., "can_view_payroll", "can_edit_shifts")
3. **Audit Logs**: Track who accessed what data
4. **Data Masking**: Hide sensitive salary info for non-admins
5. **Delegation**: Allow admins to delegate certain permissions temporarily

---

## 📞 Summary

| Feature | Employee Access | Admin Access |
|---------|----------------|--------------|
| Analytics | Personal only | Organization-wide |
| Payroll | View own | View all (employeeId=0), Generate, Mark paid |
| Shifts | View own, Request swap | View all (employeeId=0), Create, Approve swaps |
| Documents | View/Upload own | Via Employee Directory |
| Messaging | Send/Receive | Send/Receive (+ Broadcast) |
| Biometric | Setup own | Setup own |
| Attendance | Mark own | View all, Monitor |
| Leave | Apply | Approve/Reject |
| Tasks | View assigned | Assign to anyone |

**Key Principle**: Employees have **self-service** access, Admins have **management** access.

**Special Admin Navigation**:
- Payroll: employeeId = 0 shows all employee records
- Shifts: employeeId = 0 shows organization-wide calendar
- Documents: Redirects to Employee Directory for selection
