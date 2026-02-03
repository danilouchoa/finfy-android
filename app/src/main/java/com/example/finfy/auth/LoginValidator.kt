package com.example.finfy.auth

data class LoginValidationResult(
    val emailError: String? = null,
    val passwordError: String? = null
) {
    val isValid: Boolean
        get() = emailError == null && passwordError == null
}

object LoginValidator {
    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun validate(email: String, password: String): LoginValidationResult {
        if (!emailRegex.matches(email)) {
            return LoginValidationResult(emailError = "Digite um e-mail válido.")
        }

        if (password.trim().isEmpty()) {
            return LoginValidationResult(passwordError = "Informe e-mail e senha para continuar.")
        }

        return LoginValidationResult()
    }
}
