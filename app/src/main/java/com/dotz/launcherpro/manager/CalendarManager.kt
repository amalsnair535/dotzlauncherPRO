package com.dotz.launcherpro.manager

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.util.*

data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?
)

class CalendarManager(private val context: Context) {

    fun getUpcomingEvents(hours: Int = 24): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val now = System.currentTimeMillis()
        val end = now + (hours * 60 * 60 * 1000L)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            title = it.getString(0) ?: "Meeting",
                            startTime = it.getLong(1),
                            endTime = it.getLong(2),
                            location = it.getString(3)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }
}
