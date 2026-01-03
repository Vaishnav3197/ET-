package com.Vaishnav.employeetracker.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlin.math.*

object LocationHelper {
    // Office location (Replace with actual office coordinates)
    private const val OFFICE_LATITUDE = 28.6139 // New Delhi example
    private const val OFFICE_LONGITUDE = 77.2090
    private const val OFFICE_RADIUS_METERS = 200.0 // 200 meter radius
    
    /**
     * Get current location using FusedLocationProviderClient
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) {
            return null
        }
        
        val fusedLocationClient: FusedLocationProviderClient = 
            LocationServices.getFusedLocationProviderClient(context)
        
        return try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if location is within office radius
     */
    fun isWithinOfficeLocation(latitude: Double, longitude: Double): Boolean {
        val distance = calculateDistance(latitude, longitude, OFFICE_LATITUDE, OFFICE_LONGITUDE)
        return distance <= OFFICE_RADIUS_METERS
    }
    
    /**
     * Get distance from office in meters
     */
    fun getDistanceFromOffice(latitude: Double, longitude: Double): Double {
        return calculateDistance(latitude, longitude, OFFICE_LATITUDE, OFFICE_LONGITUDE)
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Format location as "lat,lng" string
     */
    fun formatLocation(location: Location): String {
        return "${location.latitude},${location.longitude}"
    }
    
    /**
     * Parse location string to Pair<latitude, longitude>
     */
    fun parseLocation(locationString: String): Pair<Double, Double>? {
        return try {
            val parts = locationString.split(",")
            if (parts.size == 2) {
                Pair(parts[0].toDouble(), parts[1].toDouble())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get office location
     */
    fun getOfficeLocation(): Pair<Double, Double> {
        return Pair(OFFICE_LATITUDE, OFFICE_LONGITUDE)
    }
}
