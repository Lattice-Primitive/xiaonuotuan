package com.nanackations2.papaya

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit

object PermissionHelper {

    private const val PREFS_NAME = "papaya_permission_prefs"
    private const val KEY_LAST_VERSION_CODE = "last_version_code"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunchOrUpdated(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastVersionCode = prefs.getLong(KEY_LAST_VERSION_CODE, -1L)
        val currentVersionCode = getCurrentVersionCode(context)
        return lastVersionCode != currentVersionCode
    }

    fun markVersionChecked(context: Context) {
        val prefs = getPrefs(context)
        val currentVersionCode = getCurrentVersionCode(context)
        prefs.edit { putLong(KEY_LAST_VERSION_CODE, currentVersionCode) }
    }

    private fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            -1L
        }
    }

    fun getRequiredRuntimePermissions(context: Context): List<String> {
        val allPermissions = mutableListOf<String>()

        allPermissions.add(android.Manifest.permission.CAMERA)
        allPermissions.add(android.Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            allPermissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            allPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            allPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return allPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }
}
