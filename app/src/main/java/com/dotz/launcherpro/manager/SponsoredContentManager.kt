package com.dotz.launcherpro.manager

import com.dotz.launcherpro.data.TimelineItem
import com.dotz.launcherpro.data.TimelineType

object SponsoredContentManager {

    private val adPool = listOf(
        TimelineItem(
            id = "sponsored_weather",
            type = TimelineType.SPONSORED,
            title = "Weather Companion",
            subtitle = "Get minimalist hourly forecasts synced with Dotz.",
            timestamp = 0L,
            packageName = "com.google.android.apps.magellan"
        ),
        TimelineItem(
            id = "sponsored_focus",
            type = TimelineType.SPONSORED,
            title = "Deep Work Mode",
            subtitle = "Enhance your focus score with this Pomodoro tool.",
            timestamp = 0L,
            packageName = "com.google.android.calendar"
        ),
        TimelineItem(
            id = "sponsored_music",
            type = TimelineType.SPONSORED,
            title = "Soundscape Discovery",
            subtitle = "Find high-fidelity tracks that match your minimalist vibe.",
            timestamp = 0L,
            packageName = "com.spotify.music"
        )
    )

    fun getRecommendedAd(topPackageName: String?, currentTime: Long): TimelineItem {
        val selectedAd = when {
            topPackageName?.contains("music") == true || topPackageName?.contains("spotify") == true -> adPool[2]
            topPackageName?.contains("calendar") == true || topPackageName?.contains("notes") == true -> adPool[1]
            else -> adPool[0]
        }
        return selectedAd.copy(timestamp = currentTime)
    }
}
