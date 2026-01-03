package com.Vaishnav.employeetracker.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePerformanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val performanceCollection = db.collection("performance_ratings")

    // Add performance rating
    suspend fun addPerformanceRating(rating: FirebasePerformanceRating): Result<String> {
        return try {
            val docRef = performanceCollection.add(rating).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add performance rating (alias for compatibility)
    suspend fun addRating(rating: FirebasePerformanceRating): Result<String> {
        return addPerformanceRating(rating)
    }

    // Update performance rating
    suspend fun updateRating(ratingId: String, rating: FirebasePerformanceRating): Result<Unit> {
        return try {
            performanceCollection.document(ratingId).set(rating).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete performance rating
    suspend fun deleteRating(ratingId: String): Result<Unit> {
        return try {
            performanceCollection.document(ratingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get rating by ID
    suspend fun getRatingById(ratingId: String): FirebasePerformanceRating? {
        return try {
            performanceCollection.document(ratingId).get().await()
                .toObject(FirebasePerformanceRating::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get employee's performance ratings
    fun getEmployeeRatings(employeeId: String): Flow<List<FirebasePerformanceRating>> = callbackFlow {
        val listener = performanceCollection
            .whereEqualTo("employeeId", employeeId)
            .orderBy("ratingDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ratings = snapshot?.toObjects(FirebasePerformanceRating::class.java) ?: emptyList()
                trySend(ratings)
            }
        awaitClose { listener.remove() }
    }

    // Get ratings by reviewer
    fun getRatingsByReviewer(reviewerId: String): Flow<List<FirebasePerformanceRating>> = callbackFlow {
        val listener = performanceCollection
            .whereEqualTo("reviewerId", reviewerId)
            .orderBy("ratingDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ratings = snapshot?.toObjects(FirebasePerformanceRating::class.java) ?: emptyList()
                trySend(ratings)
            }
        awaitClose { listener.remove() }
    }

    // Get all performance ratings (admin)
    fun getAllRatings(): Flow<List<FirebasePerformanceRating>> = callbackFlow {
        val listener = performanceCollection
            .orderBy("ratingDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ratings = snapshot?.toObjects(FirebasePerformanceRating::class.java) ?: emptyList()
                trySend(ratings)
            }
        awaitClose { listener.remove() }
    }

    // Get ratings for a specific period
    fun getRatingsForPeriod(startDate: Long, endDate: Long): Flow<List<FirebasePerformanceRating>> = callbackFlow {
        val listener = performanceCollection
            .whereGreaterThanOrEqualTo("ratingDate", startDate)
            .whereLessThanOrEqualTo("ratingDate", endDate)
            .orderBy("ratingDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ratings = snapshot?.toObjects(FirebasePerformanceRating::class.java) ?: emptyList()
                trySend(ratings)
            }
        awaitClose { listener.remove() }
    }

    // Get average rating for employee
    suspend fun getAverageRating(employeeId: String): Float? {
        return try {
            val ratings = performanceCollection
                .whereEqualTo("employeeId", employeeId)
                .get()
                .await()
                .toObjects(FirebasePerformanceRating::class.java)

            if (ratings.isEmpty()) return null

            ratings.map { it.rating }.average().toFloat()
        } catch (e: Exception) {
            null
        }
    }

    // Get performance statistics
    suspend fun getPerformanceStats(employeeId: String): PerformanceStats? {
        return try {
            val ratings = performanceCollection
                .whereEqualTo("employeeId", employeeId)
                .get()
                .await()
                .toObjects(FirebasePerformanceRating::class.java)

            if (ratings.isEmpty()) return null

            val avgRating = ratings.map { it.rating }.average().toFloat()
            val totalReviews = ratings.size

            PerformanceStats(
                averageOverall = avgRating,
                averageQuality = avgRating,
                averageProductivity = avgRating,
                averageCommunication = avgRating,
                averageTeamwork = avgRating,
                totalReviews = totalReviews
            )
        } catch (e: Exception) {
            null
        }
    }

    // Get latest rating for employee
    suspend fun getLatestRating(employeeId: String): FirebasePerformanceRating? {
        return try {
            performanceCollection
                .whereEqualTo("employeeId", employeeId)
                .orderBy("ratingDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .toObjects(FirebasePerformanceRating::class.java)
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    data class PerformanceStats(
        val averageOverall: Float,
        val averageQuality: Float,
        val averageProductivity: Float,
        val averageCommunication: Float,
        val averageTeamwork: Float,
        val totalReviews: Int
    )
}
