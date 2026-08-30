package com.bluebyte.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppUtils {
    fun fetchApps(context: Context): List<AppInfo> {
        val appsList = mutableListOf<AppInfo>()
        val pManager = context.packageManager
        val i = Intent(Intent.ACTION_MAIN, null)
        i.addCategory(Intent.CATEGORY_LAUNCHER)

        val allApps = pManager.queryIntentActivities(i, 0)
        for (ri in allApps) {
            val app = AppInfo(
                label = ri.loadLabel(pManager),
                packageName = ri.activityInfo.packageName,
                icon = ri.activityInfo.loadIcon(pManager)
            )
            appsList.add(app)
        }
        return appsList.sortedBy { it.label.toString().lowercase() }
    }
}