package com.example.finfy

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finfy.core.NetworkModule
import com.example.finfy.ui.screens.AuthDebugScreen
import com.example.finfy.ui.screens.HomeScreen
import com.example.finfy.ui.screens.LoginScreen
import com.example.finfy.ui.screens.RegisterScreen
import kotlinx.coroutines.launch

object Routes {
    const val Login = "login"
    const val Register = "register"
    const val Home = "home"
    const val AuthDebug = "authDebug"
}

@Composable
fun FinfyApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Login,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Login) {
                LoginScreen(
                    snackbarHostState = snackbarHostState,
                    onNavigateRegister = { navController.navigate(Routes.Register) },
                    onNavigateHome = { navigateHome(navController) },
                    onNavigateDebug = if (BuildConfig.DEBUG) {
                        { navController.navigate(Routes.AuthDebug) }
                    } else {
                        null
                    }
                )
            }
            composable(Routes.Register) {
                RegisterScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.Home) {
                HomeScreen(onNavigateLogout = {
                    scope.launch {
                        try {
                            NetworkModule.authRepository.logout()
                        } finally {
                            NetworkModule.clearSession()
                        }
                    }
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                })
            }
            if (BuildConfig.DEBUG) {
                composable(Routes.AuthDebug) {
                    AuthDebugScreen()
                }
            }
        }
    }
}

private fun navigateHome(navController: NavHostController) {
    navController.navigate(Routes.Home) {
        popUpTo(Routes.Login) { inclusive = true }
    }
}
