# Employee Performance Tracker

## Overview
Employee Performance Tracker is a comprehensive Android application designed to help organizations track, evaluate, and analyze employee performance across various roles and departments.

## Features

### 1. Employee Management
- ✅ Add, edit, and delete employee profiles
- ✅ Store comprehensive employee information (name, role, department, contact, etc.)
- ✅ View all employees with search functionality
- ✅ Detailed employee profiles

### 2. Task Assignment & Tracking
- ✅ Assign tasks to employees
- ✅ Track task status (Pending, In Progress, Completed, Reviewed)
- ✅ Set priorities (Low, Medium, High)
- ✅ Monitor deadlines
- ✅ View all tasks across the organization

### 3. Performance Evaluation
- ✅ Rate employees on multiple metrics:
  - Quality of Work
  - Timeliness
  - Attendance
  - Communication
  - Innovation/Initiative
- ✅ Overall rating calculation (1-5 stars)
- ✅ Add comments and remarks
- ✅ Performance history tracking

### 4. Analytics & Insights
- ✅ Dashboard with quick statistics
- ✅ Task completion rates
- ✅ Top performers identification
- ✅ Employee-wise performance overview
- ✅ Average rating calculations

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Database
- **Navigation**: Jetpack Navigation Compose
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Project Structure

```
com.Vaishnav.employeetracker/
├── data/
│   ├── Entities.kt           # Data models (Employee, Task, Performance)
│   ├── EmployeeDao.kt        # Database operations for employees
│   ├── TaskDao.kt            # Database operations for tasks
│   ├── PerformanceDao.kt     # Database operations for performance
│   ├── AppDatabase.kt        # Room database configuration
│   └── EmployeeRepository.kt # Data access layer
├── viewmodel/
│   └── EmployeeViewModel.kt  # Business logic & state management
├── navigation/
│   ├── Screen.kt             # Navigation routes
│   └── NavigationGraph.kt    # Navigation configuration
├── ui/
│   ├── screens/
│   │   ├── DashboardScreen.kt
│   │   ├── EmployeeListScreen.kt
│   │   ├── EmployeeDetailScreen.kt
│   │   ├── AddEmployeeScreen.kt
│   │   ├── EditEmployeeScreen.kt
│   │   ├── AddTaskScreen.kt
│   │   ├── TaskListScreen.kt
│   │   ├── AddPerformanceScreen.kt
│   │   └── AnalyticsScreen.kt
│   └── theme/                # Material3 theming
└── MainActivity.kt           # App entry point
```

## Database Schema

### Employee Table
- id (Primary Key)
- name
- role
- department
- joiningDate
- email
- contact
- profileImageUri (optional)

### Task Table
- id (Primary Key)
- employeeId (Foreign Key → Employee)
- description
- status
- deadline
- priority
- assignedDate

### Performance Table
- id (Primary Key)
- employeeId (Foreign Key → Employee)
- date
- qualityScore
- timelinessScore
- attendanceScore
- communicationScore
- innovationScore
- overallRating
- remarks

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or later
- Android SDK 24+

### Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/EmployeeTracker.git
```

2. Open the project in Android Studio

3. Sync Gradle dependencies

4. Run the app on an emulator or physical device

## Usage Guide

### Adding an Employee
1. Navigate to Dashboard
2. Click "View All Employees"
3. Click the floating action button (+)
4. Fill in employee details
5. Click "Add Employee"

### Assigning a Task
1. View employee details
2. Click "Add Task" button
3. Enter task description, priority, and status
4. Click "Add Task"

### Performance Review
1. View employee details
2. Click "Add Performance Review"
3. Rate on various metrics (1-5 scale)
4. Add optional remarks
5. Click "Save Performance Review"

### Viewing Analytics
1. From Dashboard, click "Performance Analytics"
2. View task completion rates
3. See top performers
4. Review all employees' performance statistics

## Key Features Implementation

### MVVM Architecture
- **Model**: Data classes and Room database entities
- **View**: Jetpack Compose UI screens
- **ViewModel**: Business logic and state management with coroutines

### Room Database
- Local data persistence
- Reactive queries using Flow
- Foreign key relationships
- Cascade delete operations

### Material Design 3
- Modern UI components
- Dynamic color theming
- Responsive layouts
- Intuitive navigation

## Future Enhancements

- [ ] Cloud sync with Firebase
- [ ] Export reports to PDF/CSV
- [ ] Charts visualization (using MPAndroidChart)
- [ ] Role-based access control
- [ ] Push notifications for deadlines
- [ ] Biometric attendance integration
- [ ] Performance trend analysis
- [ ] Department-wise analytics
- [ ] Gamification features

## Development Timeline (6 Weeks)

- **Week 1**: Project setup, data models, wireframes ✅
- **Week 2**: Database & Repository layer ✅
- **Week 3**: Core UI screens (Dashboard, Employees) ✅
- **Week 4**: Task & Performance modules ✅
- **Week 5**: Analytics & refinements ✅
- **Week 6**: Testing, documentation, APK build

## Contributing

This is a student internship project. For contributions:
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is created for educational purposes as part of an internship program.

## Contact

For questions or feedback about this project:
- Project Team: [Your Team Name]
- Email: [your.email@example.com]

## Acknowledgments

- Android Jetpack team for excellent libraries
- Material Design team for UI guidelines
- Mentors and instructors for project guidance

---

**Made with ❤️ using Kotlin & Jetpack Compose**
