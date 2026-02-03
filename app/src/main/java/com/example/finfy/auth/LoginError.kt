package com.example.finfy.auth

import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException

enum class LoginErrorKind {
    NONE,
    INVALID_CREDENTIALS,
    SESSION_EXPIRED,
    NETWORK,
    SERVER,
    UNKNOWN
}

data class LoginErrorState(
    val kind: LoginErrorKind,
    val message: String?
)

object LoginErrorMessages {
    const val INVALID_CREDENTIALS = "E-mail ou senha incorretos."
    const val SESSION_EXPIRED = "Sessão expirada. Faça login novamente."
    const val NETWORK = "Falha de conexão com o servidor. Verifique se o backend está em execução."
    const val SERVER = "Ocorreu um erro inesperado. Tente novamente em alguns instantes."
    const val UNKNOWN = "Não foi possível concluir o login. Tente novamente."
}

object LoginErrorMapper {
    private val gson = Gson()
    private val sessionExpiredErrors = setOf(
        "SESSION_EXPIRED",
        "INVALID_REFRESH_TOKEN",
        "INVALID_TOKEN_PAYLOAD",
        "NO_REFRESH_TOKEN"
    )

    fun map(error: Throwable): LoginErrorState {
        if (error is HttpException) {
            val status = error.code()
            val payload = parsePayload(error)
            val payloadError = payload?.error
            val payloadMessage = payload?.message.orEmpty()
            val normalizedMessage = payloadMessage.lowercase()

            if (status == 401 && payloadError == "INVALID_CREDENTIALS") {
                return LoginErrorState(LoginErrorKind.INVALID_CREDENTIALS, LoginErrorMessages.INVALID_CREDENTIALS)
            }

            val isSessionExpired = status == 419 ||
                status == 440 ||
                (status == 401 && sessionExpiredErrors.contains(payloadError)) ||
                normalizedMessage.contains("sessão expirada")

            if (isSessionExpired) {
                return LoginErrorState(LoginErrorKind.SESSION_EXPIRED, LoginErrorMessages.SESSION_EXPIRED)
            }

            if (status >= 500 || payloadError == "INTERNAL_ERROR") {
                return LoginErrorState(LoginErrorKind.SERVER, LoginErrorMessages.SERVER)
            }

            return LoginErrorState(LoginErrorKind.UNKNOWN, payload?.message ?: LoginErrorMessages.UNKNOWN)
        }

        if (error is IOException) {
            return LoginErrorState(LoginErrorKind.NETWORK, LoginErrorMessages.NETWORK)
        }

        return LoginErrorState(LoginErrorKind.UNKNOWN, LoginErrorMessages.UNKNOWN)
    }

    private fun parsePayload(error: HttpException): BackendErrorPayload? {
        val errorBody = error.response()?.errorBody() ?: return null
        return try {
            gson.fromJson(errorBody.charStream(), BackendErrorPayload::class.java)
        } catch (ignore: Exception) {
            null
        }
    }
}

fun isGlobalLoginError(kind: LoginErrorKind): Boolean =
    kind == LoginErrorKind.NETWORK ||
        kind == LoginErrorKind.SERVER ||
        kind == LoginErrorKind.UNKNOWN ||
        kind == LoginErrorKind.SESSION_EXPIRED
