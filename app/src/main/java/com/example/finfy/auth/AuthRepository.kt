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
}
