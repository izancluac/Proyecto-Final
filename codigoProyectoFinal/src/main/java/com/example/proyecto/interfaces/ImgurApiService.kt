package com.example.proyecto.interfaces

import com.example.proyecto.objectos.ImgurResponse
import com.example.proyecto.objectos.ImgurTokenResponse
import retrofit2.Call
import okhttp3.MultipartBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Multipart


//En la API de Imgur, el endpoint para subir una imagen es POST /3/image o refrescar el token
interface ImgurApiService {
    @Multipart
    @POST("3/image") // Endpoint de Imgur para subir imágenes
    fun uploadImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Call<ImgurResponse>
    @POST("oauth2/token")
    @FormUrlEncoded
    fun refreshAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Call<ImgurTokenResponse>

}