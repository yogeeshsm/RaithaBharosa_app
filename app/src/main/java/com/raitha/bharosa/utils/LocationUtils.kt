package com.raitha.bharosa.utils

import com.raitha.bharosa.data.LatLng
import kotlin.math.*

/**
 * Location utility functions for distance calculations
 * Task 10.1: Create LocationUtils.kt
 * Requirement 6: Search and Filter Labourers
 * Requirement 17: Location-Based Search
 * 
 * This utility provides distance calculation using the Haversine formula,
 * which calculates the great-circle distance between two points on a sphere
 * given their longitudes and latitudes.
 */
object LocationUtils {
    
    /**
     * Earth's radius in kilometers
     */
    private const val EARTH_RADIUS_KM = 6371.0
    
    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * 
     * The Haversine formula determines the great-circle distance between two points
     * on a sphere given their longitudes and latitudes. This is useful for calculating
     * distances between farmers and labourers for location-based search.
     * 
     * Formula:
     * a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
     * c = 2 ⋅ atan2(√a, √(1−a))
     * d = R ⋅ c
     * 
     * where:
     * - φ is latitude
     * - λ is longitude
     * - R is earth's radius (6371 km)
     * - Δφ is the difference in latitude
     * - Δλ is the difference in longitude
     * 
     * @param point1 First GPS coordinate (LatLng)
     * @param point2 Second GPS coordinate (LatLng)
     * @return Distance in kilometers (always non-negative)
     * 
     * Properties:
     * - Symmetry: distance(A, B) == distance(B, A)
     * - Non-negativity: distance(A, B) >= 0
     * - Identity: distance(A, A) == 0
     * 
     * Example:
     * ```
     * val bangalore = LatLng(12.9716, 77.5946)
     * val mysore = LatLng(12.2958, 76.6394)
     * val distance = LocationUtils.calculateDistance(bangalore, mysore)
     * // distance ≈ 139.6 km
     * ```
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        // Convert latitude and longitude from degrees to radians
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val lon1Rad = Math.toRadians(point1.longitude)
        val lon2Rad = Math.toRadians(point2.longitude)
        
        // Calculate differences
        val deltaLat = lat2Rad - lat1Rad
        val deltaLon = lon2Rad - lon1Rad
        
        // Haversine formula
        val a = sin(deltaLat / 2).pow(2) + 
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        // Calculate distance
        val distance = EARTH_RADIUS_KM * c
        
        // Ensure non-negative result (handle floating point precision issues)
        return abs(distance)
    }
    
    /**
     * Calculate distance between two GPS coordinates and return in meters
     * 
     * @param point1 First GPS coordinate (LatLng)
     * @param point2 Second GPS coordinate (LatLng)
     * @return Distance in meters (always non-negative)
     */
    fun calculateDistanceInMeters(point1: LatLng, point2: LatLng): Double {
        return calculateDistance(point1, point2) * 1000.0
    }
    
    /**
     * Check if a point is within a given radius of another point
     * 
     * @param center Center point (LatLng)
     * @param point Point to check (LatLng)
     * @param radiusKm Radius in kilometers
     * @return true if point is within radius, false otherwise
     */
    fun isWithinRadius(center: LatLng, point: LatLng, radiusKm: Double): Boolean {
        return calculateDistance(center, point) <= radiusKm
    }
    
    /**
     * Format distance for display
     * 
     * @param distanceKm Distance in kilometers
     * @return Formatted string (e.g., "5.2 km" or "850 m")
     */
    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).roundToInt()} m"
        } else {
            "%.1f km".format(distanceKm)
        }
    }
}
