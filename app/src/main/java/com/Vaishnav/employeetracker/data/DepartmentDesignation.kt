package com.Vaishnav.employeetracker.data

/**
 * Department and Designation structure for the organization
 */
object DepartmentDesignation {
    
    data class Department(
        val name: String,
        val designations: List<String>
    )
    
    val departments = listOf(
        Department(
            name = "Engineering",
            designations = listOf(
                "Software Engineer",
                "Senior Software Engineer",
                "Android Developer",
                "Backend Developer",
                "Team Lead"
            )
        ),
        Department(
            name = "HR",
            designations = listOf(
                "HR Executive",
                "Recruiter",
                "HR Manager"
            )
        ),
        Department(
            name = "Finance",
            designations = listOf(
                "Accountant",
                "Finance Executive",
                "Finance Manager"
            )
        ),
        Department(
            name = "Sales",
            designations = listOf(
                "Sales Executive",
                "Sales Manager"
            )
        ),
        Department(
            name = "Operations",
            designations = listOf(
                "Operations Executive",
                "Operations Manager"
            )
        ),
        Department(
            name = "Marketing",
            designations = listOf(
                "Marketing Executive",
                "Digital Marketing Executive",
                "Marketing Manager"
            )
        )
    )
    
    fun getDepartmentNames(): List<String> = departments.map { it.name }
    
    fun getDesignationsForDepartment(departmentName: String): List<String> {
        return departments.find { it.name == departmentName }?.designations ?: emptyList()
    }
    
    fun isValidCombination(department: String, designation: String): Boolean {
        return departments.find { it.name == department }?.designations?.contains(designation) == true
    }
}
