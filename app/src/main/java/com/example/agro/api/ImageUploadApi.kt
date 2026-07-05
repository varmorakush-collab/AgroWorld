package com.example.agro.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

data class ImgBBResponse(
    val data: ImgBBData?,
    val success: Boolean
)

data class ImgBBData(
    val url: String
)

interface ImageUploadService {
    @Multipart
    @POST("1/upload")
    suspend fun uploadImage(
        @Query("key") apiKey: String,
        @Part image: MultipartBody.Part
    ): ImgBBResponse
}
