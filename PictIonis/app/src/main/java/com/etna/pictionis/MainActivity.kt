package com.etna.pictionis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.etna.pictionis.data.AppearanceMode
import com.etna.pictionis.data.SessionManager
import com.etna.pictionis.data.ThemeSettings
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.asDark
import com.etna.pictionis.ui.theme.asLight
import com.etna.pictionis.ui.auth.AuthScreen
import com.etna.pictionis.ui.home.HomeScreen
import com.etna.pictionis.ui.main.MainScreen
import com.etna.pictionis.ui.theme.PictIonisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this) // pour persister le blocage anti brute-force
        ThemeSettings.init(this) // pour persister la palette choisie
        enableEdgeToEdge()
        setContent {
            // Mode sombre / lumineux : suit le système sauf choix explicite.
            val dark = when (ThemeSettings.appearanceMode) {
                AppearanceMode.DARK -> true
                AppearanceMode.LIGHT -> false
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            }
            val base = ThemeSettings.currentPalette
            val resolved = if (dark) base.asDark() else base.asLight()
            PictIonisTheme(dynamicColor = false, darkTheme = dark) {
                CompositionLocalProvider(LocalAppPalette provides resolved) {
                    AppNav()
                }
            }
        }
    }
}

// Mini-navigation sans librairie : "home" <-> "auth".
// Le lien "Inscris-toi" ouvre la page auth sur l'onglet Inscription.
@Composable
fun AppNav() {
    var route by remember { mutableStateOf("home") }
    var startOnSignUpTab by remember { mutableStateOf(false) }
    val isLoggedIn = SessionManager.isLoggedIn

    // Garde : si l'utilisateur n'est pas connecté, on le renvoie
    // automatiquement vers la page de connexion au lieu d'exécuter l'action.
    fun requireAuth(action: () -> Unit) {
        if (isLoggedIn) {
            action()
        } else {
            startOnSignUpTab = false // onglet "Connexion"
            route = "auth"
        }
    }

    // Connecté -> coquille avec menu bas (Accueil / Stats / Profil).
    // Déconnecté -> accueil public + page de connexion.
    if (isLoggedIn) {
        MainScreen(
            email = SessionManager.accountEmail,
            onSignOut = { SessionManager.signOut() }
        )
        return
    }

    when (route) {
        "auth" -> AuthScreen(
            startOnSignUpTab = startOnSignUpTab,
            isLoading = SessionManager.isLoading,
            errorMessage = SessionManager.errorMessage,
            infoMessage = SessionManager.infoMessage,
            onBack = {
                SessionManager.clearMessages()
                route = "home"
            },
            onTabChange = { SessionManager.clearMessages() },
            onSubmit = { isSignUp, email, password ->
                SessionManager.clearMessages()
                if (isSignUp) {
                    SessionManager.signUp(email, password) { route = "home" }
                } else {
                    SessionManager.signIn(email, password) { route = "home" }
                }
            },
            onForgotPassword = { email -> SessionManager.sendPasswordReset(email) }
        )
        else -> HomeScreen(
            isLoggedIn = isLoggedIn,
            onPlayWithFriends = { requireAuth { /* TODO: écran création de partie */ } },
            onJoinGame = { requireAuth { /* TODO: écran rejoindre une partie */ } },
            onQuickGame = { requireAuth { /* TODO: écran partie rapide */ } },
            onSignUp = {
                startOnSignUpTab = true
                route = "auth"
            },
            onSignOut = { SessionManager.signOut() }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomePreview() {
    PictIonisTheme(dynamicColor = false, darkTheme = false) {
        HomeScreen()
    }
}