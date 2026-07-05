package com.example.agro.api

import retrofit2.http.GET
import retrofit2.http.Query

data class NewsResponse(
    val data: List<Article>?
)

data class Article(
    val title: String?,
    val description: String?,
    val url: String?,
    val image_url: String?,
    val source: String?,
    val published_at: String?
)

interface NewsService {
    @GET("v1/news/all")
    suspend fun getNews(
        @Query("api_token") apiToken: String,
        @Query("search") search: String = "agriculture",
        @Query("language") language: String = "en",
        @Query("limit") limit: Int = 10
    ): NewsResponse
}
