package com.etna.pictionis.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.etna.pictionis.data.SessionManager
import com.etna.pictionis.data.game.GameManager
import com.etna.pictionis.ui.game.GameEntry
import com.etna.pictionis.ui.game.GameFlow
import com.etna.pictionis.ui.home.HomeScreen
import com.etna.pictionis.ui.profile.AppearanceScreen
import com.etna.pictionis.ui.profile.EditUsernameScreen
import com.etna.pictionis.ui.profile.EmailScreen
import com.etna.pictionis.ui.profile.PasswordScreen
import com.etna.pictionis.ui.profile.ProfileScreen
import com.etna.pictionis.ui.profile.ThemeScreen
import com.etna.pictionis.ui.stats.StatsScreen
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.contentColor

private enum class MainTab { HOME, STATS, PROFILE }

// Sous-pages de l'onglet Profil : chaque option des paramètres ouvre sa page.
private enum class ProfileRoute { MAIN, USERNAME, EMAIL, PASSWORD, APPEARANCE, THEMES }

// Coquille affichée quand l'utilisateur est connecté :
// le contenu change selon l'onglet du menu bas.
@Composable
fun MainScreen(
    email: String? = null,
    onSignOut: () -> Unit = {}
) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var profileRoute by remember { mutableStateOf(ProfileRoute.MAIN) }
    var gameEntry by remember { mutableStateOf<GameEntry?>(null) }
    val palette = LocalAppPalette.current

    // En partie (ou en train d'en chercher une) : plein écran, sans menu bas.
    if (gameEntry != null || GameManager.game != null) {
        GameFlow(entry = gameEntry, onExit = { gameEntry = null })
        return
    }

    Scaffold(
        containerColor = palette.background,
        bottomBar = {
            NavigationBar(containerColor = palette.backgroundDark) {
                BottomTab(
                    title = "Accueil",
                    glyph = Glyph.HOME,
                    selected = tab == MainTab.HOME,
                    onClick = {
                        tab = MainTab.HOME
                        profileRoute = ProfileRoute.MAIN
                    }
                )
                BottomTab(
                    title = "Stats",
                    glyph = Glyph.STATS,
                    selected = tab == MainTab.STATS,
                    onClick = {
                        tab = MainTab.STATS
                        profileRoute = ProfileRoute.MAIN
                    }
                )
                BottomTab(
                    title = "Profil",
                    glyph = Glyph.PROFILE,
                    selected = tab == MainTab.PROFILE,
                    onClick = {
                        tab = MainTab.PROFILE
                        profileRoute = ProfileRoute.MAIN
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    isLoggedIn = true,
                    showFooter = false, // plus de lien inscription/déconnexion ici
                    onPlayWithFriends = { gameEntry = GameEntry.CREATE },
                    onJoinGame = { gameEntry = GameEntry.JOIN },
                    onQuickGame = {
                        gameEntry = GameEntry.JOIN // affiche l'attente / les erreurs
                        GameManager.quickGame()
                    }
                )
                MainTab.STATS -> StatsScreen()
                MainTab.PROFILE -> when (profileRoute) {
                    ProfileRoute.MAIN -> ProfileScreen(
                        email = email,
                        displayName = SessionManager.accountDisplayName,
                        emailVerified = SessionManager.accountEmailVerified,
                        onEditUsername = { profileRoute = ProfileRoute.USERNAME },
                        onEmail = { profileRoute = ProfileRoute.EMAIL },
                        onPassword = { profileRoute = ProfileRoute.PASSWORD },
                        onAppearance = { profileRoute = ProfileRoute.APPEARANCE },
                        onTheme = { profileRoute = ProfileRoute.THEMES },
                        onSignOut = onSignOut
                    )
                    ProfileRoute.USERNAME -> EditUsernameScreen(
                        currentName = SessionManager.accountDisplayName,
                        onBack = { profileRoute = ProfileRoute.MAIN }
                    )
                    ProfileRoute.EMAIL -> EmailScreen(
                        email = SessionManager.accountEmail,
                        verified = SessionManager.accountEmailVerified,
                        onBack = { profileRoute = ProfileRoute.MAIN }
                    )
                    ProfileRoute.PASSWORD -> PasswordScreen(
                        onBack = { profileRoute = ProfileRoute.MAIN }
                    )
                    ProfileRoute.APPEARANCE -> AppearanceScreen(
                        onBack = { profileRoute = ProfileRoute.MAIN }
                    )
                    ProfileRoute.THEMES -> ThemeScreen(
                        onBack = { profileRoute = ProfileRoute.MAIN }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomTab(
    title: String,
    glyph: Glyph,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { NavGlyph(kind = glyph, selected = selected) },
        label = { Text(title) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = palette.secondary.contentColor(),
            selectedTextColor = palette.backgroundDark.contentColor(),
            indicatorColor = palette.secondary,
            unselectedIconColor = palette.title,
            unselectedTextColor = palette.title
        )
    )
}

private enum class Glyph { HOME, STATS, PROFILE }

// Icônes dessinées en Canvas pour éviter une dépendance d'icônes :
// maison (accueil), barres (stats), personne (profil).
// Pleines quand l'onglet est sélectionné, en contour sinon.
@Composable
private fun NavGlyph(kind: Glyph, selected: Boolean) {
    val palette = LocalAppPalette.current
    val color = if (selected) palette.secondary.contentColor() else palette.title
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val outline = Stroke(width = w * 0.08f)
        when (kind) {
            Glyph.HOME -> {
                val house = Path().apply {
                    moveTo(w * 0.50f, h * 0.10f)
                    lineTo(w * 0.90f, h * 0.45f)
                    lineTo(w * 0.78f, h * 0.45f)
                    lineTo(w * 0.78f, h * 0.90f)
                    lineTo(w * 0.22f, h * 0.90f)
                    lineTo(w * 0.22f, h * 0.45f)
                    lineTo(w * 0.10f, h * 0.45f)
                    close()
                }
                if (selected) drawPath(house, color) else drawPath(house, color, style = outline)
            }
            Glyph.STATS -> {
                val barWidth = w * 0.18f
                val lefts = listOf(0.13f, 0.41f, 0.69f)
                val tops = listOf(0.55f, 0.35f, 0.15f)
                lefts.forEachIndexed { i, left ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(w * left, h * tops[i]),
                        size = Size(barWidth, h * 0.90f - h * tops[i]),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        style = if (selected) Fill else outline
                    )
                }
            }
            Glyph.PROFILE -> {
                // Tête
                drawCircle(
                    color = color,
                    radius = w * 0.20f,
                    center = Offset(w * 0.50f, h * 0.32f),
                    style = if (selected) Fill else outline
                )
                // Épaules (arc bas)
                drawArc(
                    color = color,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.40f),
                    size = Size(w * 0.70f, h * 0.65f),
                    style = outline
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    MainScreen(email = "toi@exemple.com")
}
