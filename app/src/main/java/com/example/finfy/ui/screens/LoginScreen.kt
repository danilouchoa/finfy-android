package com.example.finfy.ui.screens

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finfy.BuildConfig
import com.example.finfy.auth.FeedbackKind
import com.example.finfy.auth.GoogleAuthClient
import com.example.finfy.auth.GoogleAuthResult
import com.example.finfy.auth.LoginEvent
import com.example.finfy.auth.LoginErrorKind
import com.example.finfy.auth.LoginViewModel
import com.example.finfy.auth.LoginViewModelFactory
import com.example.finfy.auth.isGlobalLoginError
import com.example.finfy.core.NetworkModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateRegister: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateDebug: (() -> Unit)?
) {
    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(NetworkModule.authRepository)
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    var passwordVisible by remember { mutableStateOf(false) }
    val googleClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    val hasGoogleClientId = googleClientId.isNotEmpty()
    val activity = context as? Activity
    val googleAuthClient = remember(hasGoogleClientId, activity) {
        if (hasGoogleClientId && activity != null) GoogleAuthClient.create(activity, googleClientId) else null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LoginEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                LoginEvent.NavigateHome -> onNavigateHome()
                LoginEvent.FocusEmail -> emailFocusRequester.requestFocus()
                LoginEvent.FocusPassword -> passwordFocusRequester.requestFocus()
            }
        }
    }

    val titleModifier = if (BuildConfig.DEBUG && onNavigateDebug != null) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = { onNavigateDebug() }
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Controle sua vida financeira de forma simples",
            style = MaterialTheme.typography.headlineMedium,
            modifier = titleModifier
        )
        Text(
            text = "Acesse com seu e-mail, senha ou Google.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { /* stay on login */ }
            ) {
                Text("Entrar")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onNavigateRegister
            ) {
                Text("Criar conta")
            }
        }

        uiState.feedback?.let { feedback ->
            val (variant, title) = when (feedback.kind) {
                FeedbackKind.Success -> AlertVariant.Success to null
                FeedbackKind.Info -> AlertVariant.Info to null
                FeedbackKind.Warning -> AlertVariant.Warning to null
                FeedbackKind.Error -> AlertVariant.Error to "Erro"
            }
            InlineAlert(
                variant = variant,
                title = title,
                message = feedback.message
            )
        }
 
        val globalError = uiState.globalError
        if (globalError != null && isGlobalLoginError(globalError.kind) && globalError.message != null) {
            val title = when (globalError.kind) {
                LoginErrorKind.NETWORK -> "Problema de conexão"
                LoginErrorKind.SESSION_EXPIRED -> "Erro"
                else -> "Erro ao entrar"
            }
            val variant = if (globalError.kind == LoginErrorKind.NETWORK) {
                AlertVariant.Warning
            } else {
                AlertVariant.Error
            }
            InlineAlert(
                variant = variant,
                title = title,
                message = globalError.message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("E-mail") },
            placeholder = { Text("voce@finfy.com") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester),
            isError = uiState.emailError != null,
            supportingText = {
                uiState.emailError?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            )
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Senha") },
            placeholder = { Text("••••••••") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            isError = uiState.passwordError != null,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Mostrar ou ocultar senha"
                    )
                }
            },
            supportingText = {
                uiState.passwordError?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error)
                } ?: Text(
                    text = "Use sua senha cadastrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.submit()
                }
            )
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.submit() },
            enabled = !uiState.isSubmitting
        ) {
            Text(if (uiState.isSubmitting) "Autenticando..." else "Entrar")
        }

        Text(
            text = "ou continue com",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!hasGoogleClientId) {
                        viewModel.reportGoogleFailure("Google OAuth não configurado.")
                        return@OutlinedButton
                    }
                    if (activity == null) {
                        viewModel.reportGoogleFailure("Não foi possível iniciar o login com Google.")
                        return@OutlinedButton
                    }
                    val authClient = googleAuthClient
                    if (authClient == null) {
                        viewModel.reportGoogleFailure("Google OAuth não configurado.")
                        return@OutlinedButton
                    }
                    viewModel.startGoogleSignIn()
                    scope.launch {
                        when (val result = authClient.getIdToken(activity)) {
                            is GoogleAuthResult.Success -> viewModel.submitGoogle(result.idToken)
                            is GoogleAuthResult.Failure -> viewModel.reportGoogleFailure(result.message)
                        }
                    }
                },
                enabled = hasGoogleClientId && !uiState.isGoogleLoading
            ) {
                Text("Continuar com Google")
            }
            if (uiState.isGoogleLoading) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.matchParentSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sincronizando com Google...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (uiState.googleConflictOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGoogleConflict() },
            title = { Text("Unificar contas com Google") },
            text = {
                val emailLabel = uiState.googleConflictEmail ?: "este e-mail"
                Text("Encontramos uma conta local para $emailLabel. Deseja unificar usando o Google como login principal?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resolveGoogleConflict() },
                    enabled = !uiState.isGoogleLoading
                ) { Text("Unificar") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissGoogleConflict() },
                    enabled = !uiState.isGoogleLoading
                ) { Text("Cancelar") }
            }
        )
    }
}

private enum class AlertVariant {
    Success,
    Warning,
    Error,
    Info
}

@Composable
private fun InlineAlert(
    variant: AlertVariant,
    title: String?,
    message: String,
    modifier: Modifier = Modifier
) {
    val (container, content) = when (variant) {
        AlertVariant.Success -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        AlertVariant.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        AlertVariant.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AlertVariant.Info -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = container,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = content)
            }
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}
