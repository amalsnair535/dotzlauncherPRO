package com.dotz.launcherpro.utils

import android.content.Context
import android.graphics.drawable.Drawable
import com.dotz.launcherpro.data.IconCacheManager

object IconUtils {
    fun loadAppIcon(
        context: Context,
        packageName: String,
        iconPackPackage: String?,
        iconCache: IconCacheManager
    ): Drawable? {
        val pm = context.packageManager

        // 1. Try to load from icon pack if selected
        if (iconPackPackage != null) {
            try {
                val iconPackRes = pm.getResourcesForApplication(iconPackPackage)
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                val component = launchIntent?.component
                
                // 1a. Try appfilter.xml mapping
                if (component != null) {
                    val componentStr = component.flattenToString()
                    val drawableName = iconCache.getDrawableName(componentStr, iconPackPackage)
                    if (drawableName != null) {
                        val resId = iconPackRes.getIdentifier(drawableName, "drawable", iconPackPackage)
                        if (resId != 0) return iconPackRes.getDrawable(resId, null)
                    }
                }

                // 1b. Try to find entry by package name with underscores
                val resId = iconPackRes.getIdentifier(packageName.replace(".", "_"), "drawable", iconPackPackage)
                if (resId != 0) return iconPackRes.getDrawable(resId, null)

                // 1c. Try to find entry by lowercase package name
                val resIdLower = iconPackRes.getIdentifier(packageName.lowercase().replace(".", "_"), "drawable", iconPackPackage)
                if (resIdLower != 0) return iconPackRes.getDrawable(resIdLower, null)
                
                // 1d. Try several naming conventions based on component
                if (component != null) {
                    val fullComp = component.flattenToString().replace(".", "_").replace("/", "_")
                    val resId2 = iconPackRes.getIdentifier(fullComp, "drawable", iconPackPackage)
                    if (resId2 != 0) return iconPackRes.getDrawable(resId2, null)

                    val className = component.className.replace(".", "_")
                    val resId3 = iconPackRes.getIdentifier(className, "drawable", iconPackPackage)
                    if (resId3 != 0) return iconPackRes.getDrawable(resId3, null)

                    val shortClassName = component.className.substringAfterLast(".").lowercase()
                    val resId4 = iconPackRes.getIdentifier(shortClassName, "drawable", iconPackPackage)
                    if (resId4 != 0) return iconPackRes.getDrawable(resId4, null)
                }
            } catch (_: Exception) {}
        }

        // 2. Fallback to system default app icon
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: Exception) { null }
    }
}
