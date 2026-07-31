package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RealGpsManager(private val context: Context) {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive

    private var onLocationReceived: ((lat: Double, lng: Double, speedKmh: Float) -> Unit)? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _currentLocation.value = location
            val speedKmh = location.speed * 3.6f
            onLocationReceived?.invoke(location.latitude, location.longitude, speedKmh)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun setLocationCallback(callback: (lat: Double, lng: Double, speedKmh: Float) -> Unit) {
        this.onLocationReceived = callback
    }

    @SuppressLint("MissingPermission")
    fun startTracking(): Boolean {
        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                _isGpsActive.value = false
                return false
            }

            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds interval for real updates
                    5f,   // 5 meters min distance
                    locationListener
                )
            }

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    locationListener
                )
            }

            // Get last known location immediately
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastGps ?: lastNetwork
            if (bestLocation != null) {
                _currentLocation.value = bestLocation
                val speedKmh = bestLocation.speed * 3.6f
                onLocationReceived?.invoke(bestLocation.latitude, bestLocation.longitude, speedKmh)
            }

            _isGpsActive.value = true
            return true
        } catch (e: SecurityException) {
            Log.e("RealGpsManager", "Location permission missing: ${e.message}")
            _isGpsActive.value = false
            return false
        } catch (e: Exception) {
            Log.e("RealGpsManager", "Error starting location tracking: ${e.message}")
            _isGpsActive.value = false
            return false
        }
    }

    fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.e("RealGpsManager", "Error stopping location tracking: ${e.message}")
        } finally {
            _isGpsActive.value = false
        }
    }
}
