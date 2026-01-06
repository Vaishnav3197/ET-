package com.Vaishnav.employeetracker.data.firebase

import android.util.Log
import kotlinx.coroutines.tasks.await

/**
 * Utility to initialize required Firebase data structures
 */
object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    
    /**
     * Creates the company-wide group chat if it doesn't exist
     */
    suspend fun ensureCompanyGroupExists(): Result<String> {
        return try {
            val groupId = "company_group"
            val messageRepo = FirebaseManager.messageRepository
            
            // Check if group already exists
            val existingGroup = messageRepo.getChatGroupById(groupId)
            
            if (existingGroup == null) {
                Log.d(TAG, "Company group doesn't exist, creating it...")
                
                val companyGroup = FirebaseChatGroup(
                    id = groupId,
                    groupName = "Company Chat",
                    groupType = "Company",
                    createdBy = "system",
                    isActive = true
                )
                
                // Create the group
                val result = messageRepo.createChatGroup(companyGroup, emptyList())
                
                if (result.isSuccess) {
                    Log.d(TAG, "Company group created successfully")
                    Result.success("Company group initialized")
                } else {
                    Log.e(TAG, "Failed to create company group: ${result.exceptionOrNull()}")
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } else {
                Log.d(TAG, "Company group already exists")
                Result.success("Company group already initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring company group exists", e)
            Result.failure(e)
        }
    }
    
    /**
     * Initialize all required Firebase structures
     * Call this on app startup
     */
    suspend fun initializeFirebase(): Result<Unit> {
        return try {
            ensureCompanyGroupExists()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
            Result.failure(e)
        }
    }
}
