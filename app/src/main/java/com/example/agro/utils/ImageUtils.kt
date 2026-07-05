package com.example.agro.utils

import android.content.Context
import android.net.Uri
import com.example.agro.api.NetworkClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    // ImgBB API Key - GET YOUR OWN AT https://api.imgbb.com/
    private const val IMGBB_API_KEY = "e66b922a4470317d0cb01da1b58533ec"

    suspend fun uploadToCloud(context: Context, uri: Uri): String {
        return try {
            val file = uriToFile(context, uri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            
            val response = NetworkClient.imageService.uploadImage(IMGBB_API_KEY, body)
            if (response.success) {
                response.data?.url ?: ""
            } else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "upload_image.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }
}
