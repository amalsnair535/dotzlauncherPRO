package com.dotz.launcherpro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream

class IconCacheManager(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "icons")
    private var currentIconPack: String? = null
    private val iconMap = mutableMapOf<String, String>() // Component -> Drawable Name

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    private fun loadAppFilter(iconPackPackage: String) {
        if (currentIconPack == iconPackPackage) return
        
        currentIconPack = iconPackPackage
        iconMap.clear()
        
        try {
            val res = context.packageManager.getResourcesForApplication(iconPackPackage)
            val assetManager = res.assets
            val inputStream = try {
                assetManager.open("appfilter.xml")
            } catch (e: Exception) {
                // Try to find it in xml resources if not in assets
                val resId = res.getIdentifier("appfilter", "xml", iconPackPackage)
                if (resId != 0) res.getXml(resId) else null
            }

            if (inputStream != null) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                
                if (inputStream is XmlPullParser) {
                    // It's already a parser from res.getXml()
                    parseXml(inputStream)
                } else {
                    parser.setInput(inputStream as java.io.InputStream, "UTF-8")
                    parseXml(parser)
                }
            }
            Log.d("DotzIcon", "Loaded ${iconMap.size} mappings from $iconPackPackage")
        } catch (e: Exception) {
            Log.e("DotzIcon", "Error loading appfilter: ${e.message}")
        }
    }

    private fun parseXml(parser: XmlPullParser) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) {
                    // component is usually "ComponentInfo{pkg/activity}"
                    iconMap[component] = drawable
                }
            }
            eventType = parser.next()
        }
    }

    fun getDrawableName(componentName: String, iconPackPackage: String): String? {
        loadAppFilter(iconPackPackage)
        return iconMap[componentName] ?: iconMap["ComponentInfo{$componentName}"]
    }

    private fun getCacheFile(packageName: String, iconPackPackage: String?, grayscale: Boolean): File {
        val fileName = "${packageName}_${iconPackPackage ?: "default"}_${if (grayscale) "gs" else "clr"}.png"
            .replace(".", "_")
        return File(cacheDir, fileName)
    }

    fun getIcon(
        packageName: String, 
        iconPackPackage: String?, 
        grayscale: Boolean,
        reqWidth: Int = 192,
        reqHeight: Int = 192
    ): Bitmap? {
        val file = getCacheFile(packageName, iconPackPackage, grayscale)
        if (file.exists()) {
            return try {
                decodeSampledBitmapFromFile(file.absolutePath, reqWidth, reqHeight)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun saveIcon(packageName: String, iconPackPackage: String?, grayscale: Boolean, drawable: Drawable) {
        val file = getCacheFile(packageName, iconPackPackage, grayscale)
        try {
            // Cap icon size to 192x192 to save memory and disk space
            val targetSize = 192
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(targetSize) else targetSize
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(targetSize) else targetSize
            
            var bitmap = drawable.toBitmap(width, height)

            if (grayscale) {
                bitmap = convertToGrayscale(bitmap)
            }

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun convertToGrayscale(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
    
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
}
