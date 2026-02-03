package com.example.finfy.auth

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthSessionResponse(
    val user: UserDto? = null,
    val accessToken: String
)

data class UserDto(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val provider: String? = null,
    val googleLinked: Boolean? = null,
    val emailVerifiedAt: String? = null,
    val preferences: Map<String, Any>? = null,
    val onboarding: Map<String, Any>? = null
)

data class BackendErrorPayload(
    val error: String? = null,
    val message: String? = null
)
