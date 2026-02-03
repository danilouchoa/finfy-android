package com.example.finfy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finfy.ui.theme.FinfyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinfyTheme {
                AuthDebugScreen()
            }
        }
    }
}

@Composable
fun AuthDebugScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var token by remember { mutableStateOf<String?>(null) }
    var cookies by remember { mutableStateOf(Api.dumpCookies()) }

    var lastResponse by remember { mutableStateOf("Pronto.") }
    var lastError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun refreshUi() {
        token = Api.accessToken
        cookies = Api.dumpCookies()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Finfy - Auth Debug", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            lastError = null
                            lastResponse = "Fazendo login..."
                            try {
                                val newToken = Api.login(email, password)
                                refreshUi()
                                lastResponse = "Login OK. Token começa com: ${newToken.take(18)}..."
                            } catch (e: Exception) {
                                lastError = e.message
                                lastResponse = "Falhou"
                            }
                        }
                    }
                ) { Text("Login") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            lastError = null
                            lastResponse = "Refrescando..."
                            try {
                                val newToken = Api.refresh()
                                refreshUi()
                                lastResponse = "Refresh OK. Token começa com: ${newToken.take(18)}..."
                            } catch (e: Exception) {
                                lastError = e.message
                                lastResponse = "Falhou"
                            }
                        }
                    }
                ) { Text("Refresh") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            lastError = null
                            lastResponse = "Chamando /status..."
                            try {
                                lastResponse = Api.status()
                                refreshUi()
                            } catch (e: Exception) {
                                lastError = e.message
                                lastResponse = "Falhou"
                            }
                        }
                    }
                ) { Text("Status") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            lastError = null
                            lastResponse = "Logout..."
                            try {
                                lastResponse = Api.logout()
                                refreshUi()
                            } catch (e: Exception) {
                                lastError = e.message
                                lastResponse = "Falhou"
                            }
                        }
                    }
                ) { Text("Logout") }
            }

            Divider()

            Text("Token em memória:")
            Text(token ?: "(sem token)")

            Spacer(Modifier.height(8.dp))

            Text("Cookies guardados (debug):")
            Text(cookies)

            Spacer(Modifier.height(8.dp))

            Text("Última resposta:")
            Text(lastResponse)

            lastError?.let {
                Spacer(Modifier.height(8.dp))
                Text("Erro:", color = MaterialTheme.colorScheme.error)
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
