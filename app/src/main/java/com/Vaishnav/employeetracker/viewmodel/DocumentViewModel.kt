package com.Vaishnav.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.Vaishnav.employeetracker.data.*
import com.Vaishnav.employeetracker.data.firebase.FirebaseManager
import com.Vaishnav.employeetracker.data.firebase.FirebaseDocument
import com.Vaishnav.employeetracker.utils.DateTimeHelper
import kotlinx.coroutines.flow.*

class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val documentRepo = FirebaseManager.documentRepository
    
    fun getAllDocuments() = documentRepo.getAllDocuments()
    
    fun getEmployeeDocuments(employeeId: String) = documentRepo.getEmployeeDocuments(employeeId)
    
    fun getDocumentsByType(employeeId: String, type: String) = flow<List<FirebaseDocument>> { 
        documentRepo.getEmployeeDocuments(employeeId).collect { docs ->
            emit(docs.filter { it.documentType == type })
        }
    }
    
    suspend fun uploadDocument(
        employeeId: String,
        documentType: String,
        documentName: String,
        documentUri: String,
        uploadedBy: String,
        expiryDate: Long? = null,
        notes: String? = null
    ): Result<Long> {
        return try {
            val document = FirebaseDocument(
                id = "",
                employeeId = employeeId,
                documentType = documentType,
                documentName = documentName,
                documentUri = documentUri,
                uploadedAt = null, // ServerTimestamp
                uploadedBy = uploadedBy,
                expiryDate = expiryDate,
                notes = notes
            )
            documentRepo.addDocument(document)
            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteDocument(documentId: String): Result<String> {
        return try {
            val documents = documentRepo.getAllDocuments().first()
            val document = documents.find { it.id == documentId }
            if (document != null) {
                documentRepo.deleteDocument(document.id)
                Result.success("Document deleted")
            } else {
                Result.failure(Exception("Document not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getExpiredDocuments(currentDate: Long) = flow<List<FirebaseDocument>> {
        documentRepo.getAllDocuments().collect { docs ->
            emit(docs.filter { it.expiryDate != null && it.expiryDate < currentDate })
        }
    }
    
    fun getExpiringDocuments(currentDate: Long, warningDate: Long) = flow<List<FirebaseDocument>> {
        documentRepo.getAllDocuments().collect { docs ->
            emit(docs.filter { it.expiryDate != null && it.expiryDate in currentDate..warningDate })
        }
    }
}
