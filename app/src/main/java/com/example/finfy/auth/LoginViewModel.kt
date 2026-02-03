package com.example.finfy.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val globalError: LoginErrorState? = null,
    val feedback: String? = null,
    val isSubmitting: Boolean = false
)

sealed class LoginEvent {
    data class ShowSnackbar(val message: String) : LoginEvent()
    object NavigateHome : LoginEvent()
    object FocusEmail : LoginEvent()
    object FocusPassword : LoginEvent()
}

class LoginViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        _state.update { current ->
            current.copy(
                email = value,
                emailError = null,
                globalError = null,
                feedback = null
            )
        }
    }

    fun onPasswordChange(value: String) {
        _state.update { current ->
            current.copy(
                password = value,
                passwordError = null,
                globalError = null,
                feedback = null
            )
        }
    }

    fun submit() {
        if (_state.value.isSubmitting) return

        val current = _state.value
        val trimmedEmail = current.email.trim()
        val validation = LoginValidator.validate(trimmedEmail, current.password)

        if (!validation.isValid) {
            _state.update {
                it.copy(
                    emailError = validation.emailError,
                    passwordError = validation.passwordError,
                    globalError = null,
                    feedback = null
                )
            }

            viewModelScope.launch {
                if (validation.emailError != null) {
                    _events.send(LoginEvent.FocusEmail)
                } else if (validation.passwordError != null) {
                    _events.send(LoginEvent.FocusPassword)
                }
            }
            return
        }

        if (trimmedEmail != current.email) {
            _state.update { it.copy(email = trimmedEmail) }
        }

        _state.update {
            it.copy(
                isSubmitting = true,
                emailError = null,
                passwordError = null,
                globalError = null,
                feedback = null
            )
        }

        viewModelScope.launch {
            try {
                repository.login(trimmedEmail, current.password)
                _state.update { it.copy(isSubmitting = false, feedback = "Login realizado! Redirecionando...") }
                _events.send(LoginEvent.ShowSnackbar("Login realizado! Bem-vindo de volta."))
                delay(600)
                _events.send(LoginEvent.NavigateHome)
            } catch (throwable: Throwable) {
                val mapped = LoginErrorMapper.map(throwable)
                if (mapped.kind == LoginErrorKind.INVALID_CREDENTIALS) {
                    val message = mapped.message ?: LoginErrorMessages.INVALID_CREDENTIALS
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = message,
                            passwordError = message,
                            globalError = null,
                            feedback = null
                        )
                    }
                    _events.send(LoginEvent.FocusEmail)
                } else {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = null,
                            passwordError = null,
                            globalError = mapped,
                            feedback = null
                        )
                    }
                }
            }
        }
    }
}

class LoginViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
