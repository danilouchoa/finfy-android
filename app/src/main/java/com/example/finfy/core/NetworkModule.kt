package com.example.finfy.core

import com.example.finfy.SimpleCookieJar
import com.example.finfy.auth.AuthApi
import com.example.finfy.auth.AuthRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private val cookieJar = SimpleCookieJar()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(AuthInterceptor { TokenStore.getAccessToken() })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    val authRepository: AuthRepository = AuthRepository(authApi, TokenStore)
}
