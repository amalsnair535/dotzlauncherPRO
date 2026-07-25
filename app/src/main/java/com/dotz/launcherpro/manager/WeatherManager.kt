package com.dotz.launcherpro.manager

import com.dotz.launcherpro.data.DotzPreferencesRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class WeatherManager(
    private val prefs: DotzPreferencesRepository,
    private val locationManager: LocationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val WEATHER_REFRESH_INTERVAL = 30 * 60 * 1000L

    private val _weatherTemp = MutableStateFlow<String?>(null)
    val weatherTemp = _weatherTemp.asStateFlow()

    private val _weatherCondition = MutableStateFlow<String?>(null)
    val weatherCondition = _weatherCondition.asStateFlow()

    private val _weatherFeelsLike = MutableStateFlow<String?>(null)
    val weatherFeelsLike = _weatherFeelsLike.asStateFlow()

    private val _weatherSummary = MutableStateFlow<String?>(null)
    val weatherSummary = _weatherSummary.asStateFlow()

    private val _weatherAqi = MutableStateFlow<String?>(null)
    val weatherAqi = _weatherAqi.asStateFlow()

    private val _weatherAqiLabel = MutableStateFlow<String?>(null)
    val weatherAqiLabel = _weatherAqiLabel.asStateFlow()

    private val _weatherLow = MutableStateFlow<String?>(null)
    val weatherLow = _weatherLow.asStateFlow()

    private val _weatherHigh = MutableStateFlow<String?>(null)
    val weatherHigh = _weatherHigh.asStateFlow()

    fun refreshWeather(force: Boolean = false) {
        scope.launch {
            val settings = prefs.settingsFlow.first()
            if (!force && !settings.showWeatherInfo) return@launch
            
            val now = System.currentTimeMillis()
            if (!force && (now - settings.lastWeatherFetchTime < WEATHER_REFRESH_INTERVAL)) return@launch

            prefs.setLastWeatherFetchTime(now)
            locationManager.getCurrentLocation(
                callback = { lat, lon -> fetchWeather(lat, lon) },
                fallback = { fetchWeather() }
            )
        }
    }

    private fun fetchWeather(lat: Double = 51.5074, lon: Double = 0.1278) {
        scope.launch {
            try {
                val weatherDeferred = async(Dispatchers.IO) {
                    try {
                        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,apparent_temperature,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
                        java.net.URL(url).readText()
                    } catch (e: Exception) { null }
                }

                val aqiDeferred = async(Dispatchers.IO) {
                    try {
                        val url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi"
                        java.net.URL(url).readText()
                    } catch (e: Exception) { null }
                }

                val weatherResult = weatherDeferred.await()
                val aqiResult = aqiDeferred.await()

                if (weatherResult != null) {
                    val weatherJson = Gson().fromJson(weatherResult, JsonObject::class.java)
                    val current = weatherJson.getAsJsonObject("current")
                    val daily = weatherJson.getAsJsonObject("daily")

                    if (current != null) {
                        val temp = current.get("temperature_2m").asDouble
                        val feelsLike = current.get("apparent_temperature").asDouble
                        val weatherCode = current.get("weather_code").asInt
                        
                        val low = daily?.get("temperature_2m_min")?.asJsonArray?.get(0)?.asDouble ?: 0.0
                        val high = daily?.get("temperature_2m_max")?.asJsonArray?.get(0)?.asDouble ?: 0.0

                        val condition = mapWmoCode(weatherCode)
                        
                        _weatherTemp.value = "${temp.toInt()}°"
                        _weatherCondition.value = condition
                        _weatherFeelsLike.value = "Feels like ${feelsLike.toInt()}°"
                        _weatherLow.value = "${low.toInt()}°"
                        _weatherHigh.value = "${high.toInt()}°"
                        _weatherSummary.value = "The skies will be ${condition.lowercase()}. The low will be ${low.toInt()}°."
                    }
                }

                if (aqiResult != null) {
                    val aqiJson = Gson().fromJson(aqiResult, JsonObject::class.java)
                    val current = aqiJson.getAsJsonObject("current")
                    if (current != null) {
                        val aqiValue = current.get("us_aqi").asInt
                        _weatherAqi.value = aqiValue.toString()
                        _weatherAqiLabel.value = getAqiLabel(aqiValue)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun mapWmoCode(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Fair"
        }
    }

    private fun getAqiLabel(aqi: Int): String {
        return when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Unhealthy for Sensitive Groups"
            aqi <= 200 -> "Unhealthy"
            aqi <= 300 -> "Very Unhealthy"
            else -> "Hazardous"
        }
    }
}
