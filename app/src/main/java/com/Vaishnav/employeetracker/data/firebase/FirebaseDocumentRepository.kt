package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseDocumentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val documentCollection = db.collection("documents")
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference.child("documents")

    // Upload document to Firebase Storage and save metadata to Firestore
    suspend fun addDocument(document: FirebaseDocument): Result<String> {
        return try {
            val docRef = documentCollection.add(document).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload document with file to Firebase Storage and save metadata to Firestore
    suspend fun uploadDocument(document: FirebaseDocument, fileUri: String): Result<String> {
        return try {
            // Generate unique filename
            val fileName = "${System.currentTimeMillis()}_${document.documentName}"
            val fileRef = storageRef.child(fileName)

            // Upload file to Firebase Storage
            val file = File(fileUri)
            fileRef.putFile(android.net.Uri.fromFile(file)).await()

            // Get download URL
            val downloadUrl = fileRef.downloadUrl.await().toString()

            // Save document metadata to Firestore with storage URL
            val docWithUrl = document.copy(documentUri = downloadUrl)
            val docRef = documentCollection.add(docWithUrl).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update document metadata
    suspend fun updateDocument(documentId: String, document: FirebaseDocument): Result<Unit> {
        return try {
            documentCollection.document(documentId).set(document).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete document (removes from both Firestore and Storage)
    suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            // Get document to find file URL
            val doc = documentCollection.document(documentId).get().await()
                .toObject(FirebaseDocument::class.java)

            // Delete file from Storage
            doc?.documentUri?.let { url ->
                try {
                    storage.getReferenceFromUrl(url).delete().await()
                } catch (e: Exception) {
                    // Continue even if storage deletion fails
                }
            }

            // Delete metadata from Firestore
            documentCollection.document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get document by ID
    suspend fun getDocumentById(documentId: String): FirebaseDocument? {
        return try {
            documentCollection.document(documentId).get().await()
                .toObject(FirebaseDocument::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get employee's documents
    fun getEmployeeDocuments(employeeId: String): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }

    // Get documents uploaded by user
    fun getDocumentsUploadedBy(uploadedBy: String): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .whereEqualTo("uploadedBy", uploadedBy)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }

    // Get all documents (admin)
    fun getAllDocuments(): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }

    // Get documents by type
    fun getDocumentsByType(documentType: String): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .whereEqualTo("documentType", documentType)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }

    // Get documents by category
    fun getDocumentsByCategory(category: String): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .whereEqualTo("category", category)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }

    // Search documents by name
    fun searchDocuments(query: String): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .orderBy("documentName")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }

    // Get documents for employee by type
    fun getEmployeeDocumentsByType(employeeId: String, documentType: String): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .whereEqualTo("employeeId", employeeId)
            .whereEqualTo("documentType", documentType)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }

    // Get document count for employee
    suspend fun getDocumentCount(employeeId: String): Int {
        return try {
            documentCollection
                .whereEqualTo("employeeId", employeeId)
                .get()
                .await()
                .size()
        } catch (e: Exception) {
            0
        }
    }

    // Get total storage used by employee
    suspend fun getTotalStorageUsed(employeeId: String): Long {
        return try {
            val documents = documentCollection
                .whereEqualTo("employeeId", employeeId)
                .get()
                .await()
                .toObjects(FirebaseDocument::class.java)

            documents.size.toLong()
        } catch (e: Exception) {
            0L
        }
    }

    // Download document from Firebase Storage
    suspend fun downloadDocument(documentId: String, localPath: String): Result<String> {
        return try {
            val doc = documentCollection.document(documentId).get().await()
                .toObject(FirebaseDocument::class.java)
                ?: return Result.failure(Exception("Document not found"))

            val fileRef = storage.getReferenceFromUrl(doc.documentUri)
            val localFile = File(localPath, doc.documentName)

            fileRef.getFile(localFile).await()
            Result.success(localFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get download URL for document
    suspend fun getDownloadUrl(documentId: String): String? {
        return try {
            val doc = documentCollection.document(documentId).get().await()
                .toObject(FirebaseDocument::class.java)
            doc?.documentUri
        } catch (e: Exception) {
            null
        }
    }

    // Batch delete documents
    suspend fun batchDeleteDocuments(documentIds: List<String>): Result<Int> {
        return try {
            var deletedCount = 0

            documentIds.forEach { documentId ->
                deleteDocument(documentId).getOrThrow()
                deletedCount++
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get recent documents (last 10)
    fun getRecentDocuments(limit: Int = 10): Flow<List<FirebaseDocument>> = callbackFlow {
        val listener = documentCollection
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.toObjects(FirebaseDocument::class.java) ?: emptyList()
                trySend(documents)
            }
        awaitClose { listener.remove() }
    }
}
