package com.example.finfy.auth

import com.example.finfy.core.TokenStore

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String): AuthSessionResponse {
        val response = authApi.login(LoginRequest(email = email, password = password))
        tokenStore.setAccessToken(response.accessToken)
        return response
    }

    suspend fun loginWithGoogle(credential: String): AuthSessionResponse {
        val response = authApi.googleLogin(GoogleCredentialRequest(credential = credential))
        tokenStore.setAccessToken(response.accessToken)
        return response
    }

    suspend fun resolveGoogleConflict(credential: String): AuthSessionResponse {
        val response = authApi.resolveGoogleConflict(GoogleResolveConflictRequest(credential = credential))
        tokenStore.setAccessToken(response.accessToken)
        return response
    }

    suspend fun logout() {
        try {
            authApi.logout()
        } finally {
            tokenStore.clear()
        }
    }
}
