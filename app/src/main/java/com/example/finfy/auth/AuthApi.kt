package com.example.finfy.auth

import com.example.finfy.core.ApiConfig
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("${ApiConfig.AUTH_BASE}/login")
    suspend fun login(@Body request: LoginRequest): AuthSessionResponse
}
