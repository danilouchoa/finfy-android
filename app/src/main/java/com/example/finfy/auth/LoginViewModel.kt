package com.example.finfy.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val globalError: LoginErrorState? = null,
    val feedback: Feedback? = null,
    val isSubmitting: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val googleConflictEmail: String? = null,
    val googleConflictOpen: Boolean = false
)

enum class FeedbackKind {
    Success,
    Info,
    Warning,
    Error
}

data class Feedback(
    val kind: FeedbackKind,
    val message: String
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
    private val gson = Gson()

    private var pendingGoogleCredential: String? = null

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
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        feedback = Feedback(FeedbackKind.Success, "Login realizado! Redirecionando...")
                    )
                }
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

    fun startGoogleSignIn() {
        pendingGoogleCredential = null
        _state.update {
            it.copy(
                isGoogleLoading = true,
                feedback = null,
                globalError = null,
                googleConflictOpen = false,
                googleConflictEmail = null
            )
        }
    }

    fun reportGoogleFailure(message: String) {
        _state.update {
            it.copy(
                isGoogleLoading = false,
                feedback = Feedback(FeedbackKind.Error, message),
                googleConflictOpen = false,
                googleConflictEmail = null
            )
        }
    }

    fun submitGoogle(credential: String) {
        _state.update {
            it.copy(
                isGoogleLoading = true,
                feedback = null,
                globalError = null,
                googleConflictOpen = false,
                googleConflictEmail = null
            )
        }

        viewModelScope.launch {
            try {
                repository.loginWithGoogle(credential)
                _state.update {
                    it.copy(
                        isGoogleLoading = false,
                        feedback = Feedback(FeedbackKind.Success, "Login com Google concluído! Redirecionando...")
                    )
                }
                _events.send(LoginEvent.ShowSnackbar("Login com Google concluído."))
                delay(600)
                _events.send(LoginEvent.NavigateHome)
            } catch (throwable: Throwable) {
                val backendError = parseBackendError(throwable)
                if (throwable is HttpException && throwable.code() == 409 && backendError?.error == "ACCOUNT_CONFLICT") {
                    pendingGoogleCredential = credential
                    _state.update {
                        it.copy(
                            isGoogleLoading = false,
                            feedback = Feedback(
                                FeedbackKind.Info,
                                "Encontramos uma conta local com este e-mail. Deseja unificar com Google?"
                            ),
                            googleConflictEmail = backendError.data?.email,
                            googleConflictOpen = true
                        )
                    }
                    return@launch
                }

                val message = if (throwable is IOException) {
                    LoginErrorMessages.NETWORK
                } else {
                    backendError?.message ?: "Não foi possível autenticar com Google."
                }
                _state.update {
                    it.copy(
                        isGoogleLoading = false,
                        feedback = Feedback(FeedbackKind.Error, message),
                        googleConflictOpen = false,
                        googleConflictEmail = null
                    )
                }
            }
        }
    }

    fun dismissGoogleConflict() {
        pendingGoogleCredential = null
        _state.update {
            it.copy(
                googleConflictOpen = false,
                googleConflictEmail = null
            )
        }
    }

    fun resolveGoogleConflict() {
        val credential = pendingGoogleCredential ?: return
        _state.update {
            it.copy(
                isGoogleLoading = true,
                feedback = null,
                googleConflictOpen = false
            )
        }

        viewModelScope.launch {
            try {
                repository.resolveGoogleConflict(credential)
                pendingGoogleCredential = null
                _state.update {
                    it.copy(
                        isGoogleLoading = false,
                        feedback = Feedback(FeedbackKind.Success, "Contas unificadas! Redirecionando...")
                    )
                }
                _events.send(LoginEvent.ShowSnackbar("Contas unificadas com sucesso."))
                delay(600)
                _events.send(LoginEvent.NavigateHome)
            } catch (throwable: Throwable) {
                val backendError = parseBackendError(throwable)
                val message = backendError?.message ?: "Não foi possível unificar as contas."
                _state.update {
                    it.copy(
                        isGoogleLoading = false,
                        feedback = Feedback(FeedbackKind.Error, message)
                    )
                }
            }
        }
    }

    private fun parseBackendError(throwable: Throwable): BackendErrorPayload? {
        if (throwable !is HttpException) return null
        val errorBody = throwable.response()?.errorBody() ?: return null
        return try {
            gson.fromJson(errorBody.charStream(), BackendErrorPayload::class.java)
        } catch (ignore: Exception) {
            null
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
