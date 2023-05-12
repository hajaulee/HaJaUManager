package com.hajaulee.anytv.hajaumanager

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager


class ExtensionsLoader {

    companion object {
        fun getPackageContext(context: Context, packageName: String?): Context? {
            return try {
                context.applicationContext.createPackageContext(
                    packageName,
                    Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
                )
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }

        fun getVersion(pm: PackageManager, packageName: String): String? {
            return try {
                val pInfo: PackageInfo =  pm.getPackageInfo(
                    packageName,
                    PackageManager.GET_META_DATA
                )
                pInfo.versionName
            } catch (e1: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}