package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NavigationUtils {

    fun launchNeshan(context: Context, lat: Double, lng: Double, address: String) {
        try {
            val uri = Uri.parse("nshn:$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("org.neshan.maps")
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web or general geo intent
            launchGenericGeo(context, lat, lng, address)
        }
    }

    fun launchBalad(context: Context, lat: Double, lng: Double, address: String) {
        try {
            val uri = Uri.parse("balad://location?latitude=$lat&longitude=$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("ir.balad.app")
            context.startActivity(intent)
        } catch (e: Exception) {
            launchGenericGeo(context, lat, lng, address)
        }
    }

    fun launchGoogleMaps(context: Context, lat: Double, lng: Double, address: String) {
        try {
            val uri = Uri.parse("google.navigation:q=$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        } catch (e: Exception) {
            launchGenericGeo(context, lat, lng, address)
        }
    }

    private fun launchGenericGeo(context: Context, lat: Double, lng: Double, label: String) {
        try {
            val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
            val intent = Intent(Intent.ACTION_VIEW, geoUri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "برنامه مسیریاب یافت نشد", Toast.LENGTH_SHORT).show()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "امکان برقراری تماس وجود ندارد", Toast.LENGTH_SHORT).show()
        }
    }
}
