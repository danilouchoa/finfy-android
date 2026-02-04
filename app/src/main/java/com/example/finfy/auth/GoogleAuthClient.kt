package com.example.finfy.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed class GoogleAuthResult {
    data class Success(val idToken: String) : GoogleAuthResult()
    data class Failure(val message: String) : GoogleAuthResult()
}

class GoogleAuthClient(
    private val credentialManager: CredentialManager,
    private val serverClientId: String
) {
    suspend fun getIdToken(activity: Activity): GoogleAuthResult {
        if (serverClientId.isBlank()) {
            return GoogleAuthResult.Failure("Google OAuth não configurado.")
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            val googleIdCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdCredential.idToken
            if (idToken.isNullOrBlank()) {
                GoogleAuthResult.Failure("Credencial do Google não disponível.")
            } else {
                GoogleAuthResult.Success(idToken)
            }
        } catch (exception: GetCredentialCancellationException) {
            GoogleAuthResult.Failure("Login com Google cancelado.")
        } catch (exception: NoCredentialException) {
            GoogleAuthResult.Failure("Nenhuma conta Google disponível.")
        } catch (exception: GetCredentialProviderConfigurationException) {
            GoogleAuthResult.Failure("Configuração do Google inválida. Verifique o client ID.")
        } catch (exception: GoogleIdTokenParsingException) {
            Log.w("GoogleAuth", "Invalid Google ID token", exception)
            GoogleAuthResult.Failure("Não foi possível autenticar com Google.")
        } catch (exception: GetCredentialException) {
            Log.w("GoogleAuth", "Google credential error", exception)
            GoogleAuthResult.Failure("Erro ao autenticar com Google.")
        } catch (exception: Exception) {
            Log.w("GoogleAuth", "Google auth unexpected error", exception)
            GoogleAuthResult.Failure("Erro ao autenticar com Google.")
        }
    }

    companion object {
        fun create(activity: Activity, serverClientId: String): GoogleAuthClient {
            return GoogleAuthClient(CredentialManager.create(activity), serverClientId)
        }
    }
}
