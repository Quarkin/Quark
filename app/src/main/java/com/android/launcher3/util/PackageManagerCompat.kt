package com.android.launcher3.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

object PackageManagerCompat {
    fun getPackageInfo(packageManager: PackageManager, packageName: String, flags: Long = 0): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags.toInt())
        }
    }

    fun getApplicationInfo(packageManager: PackageManager, packageName: String, flags: Long = 0): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, flags.toInt())
        }
    }
}
