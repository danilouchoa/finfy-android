package com.example.finfy.core

object TokenStore {
    @Volatile
    private var accessToken: String? = null

    fun getAccessToken(): String? = accessToken

    fun setAccessToken(token: String?) {
        accessToken = token
    }

    fun clear() {
        accessToken = null
    }
}
