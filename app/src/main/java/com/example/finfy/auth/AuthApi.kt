package com.example.finfy.auth

import com.example.finfy.core.ApiConfig
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("${ApiConfig.AUTH_BASE}/login")
    suspend fun login(@Body request: LoginRequest): AuthSessionResponse

    @POST("${ApiConfig.AUTH_BASE}/google")
    suspend fun googleLogin(@Body request: GoogleCredentialRequest): AuthSessionResponse

    @POST("${ApiConfig.AUTH_BASE}/google/resolve-conflict")
    suspend fun resolveGoogleConflict(@Body request: GoogleResolveConflictRequest): AuthSessionResponse

    @POST("${ApiConfig.AUTH_BASE}/logout")
    suspend fun logout()
}
