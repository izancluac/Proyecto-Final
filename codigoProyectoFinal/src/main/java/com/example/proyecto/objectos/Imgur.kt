package com.example.proyecto.objectos
import com.example.proyecto.interfaces.ImgurApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.jvm.java


data class ImgurResponse(
    val data: Data,
    val success: Boolean,
    val status: Int
)

data class Data(
    val link: String
)

data class ImgurTokenResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String,
    val refresh_token: String,
    val account_username: String
)

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.imgur.com/")  // IMPORTANTE: Verifica la URL base
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ImgurApiService by lazy {
        retrofit.create(ImgurApiService::class.java)
    }
}
