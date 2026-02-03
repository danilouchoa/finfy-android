package com.example.finfy.auth

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class LoginErrorMapperTest {
    @Test
    fun `network errors map to NETWORK`() {
        val result = LoginErrorMapper.map(IOException("timeout"))
        assertEquals(LoginErrorKind.NETWORK, result.kind)
        assertEquals(LoginErrorMessages.NETWORK, result.message)
    }

    @Test
    fun `invalid credentials maps correctly`() {
        val error = httpException(401, """{"error":"INVALID_CREDENTIALS","message":"Credenciais inválidas"}""")
        val result = LoginErrorMapper.map(error)
        assertEquals(LoginErrorKind.INVALID_CREDENTIALS, result.kind)
        assertEquals(LoginErrorMessages.INVALID_CREDENTIALS, result.message)
    }

    @Test
    fun `session expired maps correctly by status`() {
        val error = httpException(419, """{"error":"SESSION_EXPIRED","message":"Sessão expirada"}""")
        val result = LoginErrorMapper.map(error)
        assertEquals(LoginErrorKind.SESSION_EXPIRED, result.kind)
        assertEquals(LoginErrorMessages.SESSION_EXPIRED, result.message)
    }

    @Test
    fun `server errors map correctly`() {
        val error = httpException(500, """{"error":"INTERNAL_ERROR","message":"Erro interno"}""")
        val result = LoginErrorMapper.map(error)
        assertEquals(LoginErrorKind.SERVER, result.kind)
        assertEquals(LoginErrorMessages.SERVER, result.message)
    }

    @Test
    fun `unknown errors use payload message when available`() {
        val error = httpException(400, """{"error":"SOMETHING_ELSE","message":"Mensagem específica"}""")
        val result = LoginErrorMapper.map(error)
        assertEquals(LoginErrorKind.UNKNOWN, result.kind)
        assertEquals("Mensagem específica", result.message)
    }

    private fun httpException(code: Int, json: String): HttpException {
        val body = json.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(code, body)
        return HttpException(response)
    }
}
