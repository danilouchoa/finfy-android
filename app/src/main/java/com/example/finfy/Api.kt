package com.example.finfy

import com.example.finfy.core.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object Api {

    // IMPORTANTE:
    // Como você está usando "adb reverse", o celular enxerga a API do PC em 127.0.0.1:4000
    // Se tiver problema com cookie/domínio depois, troque para "http://localhost:4000/api"
    private const val BASE_URL = "${ApiConfig.BASE_URL}api"
    private const val HOST_FOR_COOKIE_DEBUG = "127.0.0.1"

    private val cookieJar = SimpleCookieJar()

    // access token em memória (como no seu backend)
    var accessToken: String? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()

            accessToken?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }

            chain.proceed(builder.build())
        }
        .build()

    fun dumpCookies(): String = cookieJar.dump(HOST_FOR_COOKIE_DEBUG)

    suspend fun status(): String = request(
        method = "GET",
        url = "$BASE_URL/status"
    )

    suspend fun login(email: String, password: String): String {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()

        val body = request(
            method = "POST",
            url = "$BASE_URL/auth/login",
            jsonBody = payload
        )

        val token = extractAccessToken(body)
        accessToken = token
        return token
    }

    suspend fun refresh(): String {
        val body = request(
            method = "POST",
            url = "$BASE_URL/auth/refresh"
        )

        val token = extractAccessToken(body)
        accessToken = token
        return token
    }

    suspend fun logout(): String {
        val body = request(
            method = "POST",
            url = "$BASE_URL/auth/logout"
        )

        accessToken = null
        cookieJar.clear()
        return body
    }

    // -------------------- helpers --------------------

    private suspend fun request(
        method: String,
        url: String,
        jsonBody: String? = null
    ): String = withContext(Dispatchers.IO) {

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBuilder = Request.Builder().url(url)

        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                val body = (jsonBody ?: "").toRequestBody(mediaType)
                requestBuilder.post(body)
            }
            else -> error("Método não suportado: $method")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}: $responseBody")
            }
            responseBody
        }
    }

    private fun extractAccessToken(body: String): String {
        // Espera JSON tipo: { "accessToken": "..." }
        val json = JSONObject(body)

        val token = json.optString("accessToken")
            .ifBlank { json.optString("token") }
            .ifBlank { json.optString("access_token") }

        if (token.isBlank()) {
            throw RuntimeException("Não encontrei accessToken no response: $body")
        }
        return token
    }
}
