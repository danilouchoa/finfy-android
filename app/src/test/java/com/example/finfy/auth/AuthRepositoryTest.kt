package com.example.finfy.auth

import com.example.finfy.core.TokenStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {
    private class FakeAuthApi : AuthApi {
        override suspend fun login(request: LoginRequest): AuthSessionResponse {
            return AuthSessionResponse(accessToken = "token-login", user = null)
        }

        override suspend fun googleLogin(request: GoogleCredentialRequest): AuthSessionResponse {
            return AuthSessionResponse(accessToken = "token-google", user = null)
        }

        override suspend fun resolveGoogleConflict(request: GoogleResolveConflictRequest): AuthSessionResponse {
            return AuthSessionResponse(accessToken = "token-merge", user = null)
        }

        override suspend fun logout() {
            // no-op
        }
    }

    @Test
    fun `login stores access token`() = runBlocking {
        TokenStore.clear()
        val repo = AuthRepository(FakeAuthApi(), TokenStore)
        repo.login("user@finfy.com", "secret")
        assertEquals("token-login", TokenStore.getAccessToken())
    }

    @Test
    fun `google login stores access token`() = runBlocking {
        TokenStore.clear()
        val repo = AuthRepository(FakeAuthApi(), TokenStore)
        repo.loginWithGoogle("google-token")
        assertEquals("token-google", TokenStore.getAccessToken())
    }

    @Test
    fun `logout clears access token`() = runBlocking {
        TokenStore.setAccessToken("token")
        val repo = AuthRepository(FakeAuthApi(), TokenStore)
        repo.logout()
        assertNull(TokenStore.getAccessToken())
    }
}
