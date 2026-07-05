package com.example.agro.api

import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String
)

data class Main(
    val temp: Double,
    val humidity: Int
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

interface WeatherService {
    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}
