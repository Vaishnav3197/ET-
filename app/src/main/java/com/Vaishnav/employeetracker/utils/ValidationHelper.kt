package com.Vaishnav.employeetracker.utils

object ValidationHelper {
    
    /**
     * Validates phone number format
     * @param phone The phone number to validate
     * @return true if valid, false otherwise
     */
    fun isValidPhone(phone: String): Boolean {
        return phone.length == 10 && phone.all { it.isDigit() }
    }
    
    /**
     * Filters phone number input to digits only and limits to 10 characters
     * @param input The raw input string
     * @return Filtered string with digits only, max 10 chars
     */
    fun filterPhoneInput(input: String): String {
        return input.filter { it.isDigit() }.take(10)
    }
    
    /**
     * Gets validation error message for phone number
     * @param phone The phone number to validate
     * @return Error message if invalid, null if valid
     */
    fun getPhoneErrorMessage(phone: String): String? {
        return when {
            phone.isEmpty() -> "Phone number is required"
            phone.length < 10 -> "Phone number must be 10 digits"
            !phone.all { it.isDigit() } -> "Phone number must contain only digits"
            else -> null
        }
    }
}
