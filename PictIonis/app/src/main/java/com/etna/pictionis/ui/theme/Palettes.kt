package com.etna.pictionis.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ---------------------------------------------------------------------------
// Palettes de l'application (dossier Design/Palettes + thème historique).
//
// Chaque palette mappe les 5 couleurs brutes Coolors vers des rôles
// sémantiques utilisés par tous les écrans :
// - background / backgroundDark : fond sombre principal + barre de navigation
// - primary / secondary : boutons, avatar, accents forts, indicateur
// - title : logo, nom d'app, onglets inactifs (doit contraster avec background)
// - accent : sous-titres, liens discrets (doit contraster avec background)
// - lightBg / border : corps clair (profil, auth) + bordures/diviseurs
// - preview : les 5 couleurs brutes d'origine, pour la miniature de sélection
//
// Quelques teintes sont légèrement dérivées (assombries/éclaircies) quand la
// palette brute ne fournit pas assez de contraste pour rester lisible.
// ---------------------------------------------------------------------------
data class AppPalette(
    val id: String,
    val name: String,
    val background: Color,
    val backgroundDark: Color,
    val primary: Color,
    val secondary: Color,
    val title: Color,
    val accent: Color,
    val lightBg: Color,
    val border: Color,
    // Encre sombre pour les textes posés sur surfaces claires (cartes blanches).
    // Vaut le fond sombre, sauf pour les palettes pastel dont le fond est clair.
    val onLight: Color,
    val preview: List<Color>,
    // Corps des cartes/zones secondaires : lightBg en mode clair, background en mode sombre
    val body: Color = lightBg,
    // Couleur de l'icône de l'application : "red" ou "green"
    val iconColor: String = "green"
)

// Texte lisible (blanc ou encre sombre) sur un fond coloré.
fun Color.contentColor(): Color =
    if (luminance() > 0.4f) Color(0xFF1A2332) else Color.White

// Assombrit une couleur (utilisé pour la barre de navigation en mode lumineux).
fun Color.darken(factor: Float): Color {
    val f = (1f - factor).coerceIn(0f, 1f)
    return Color(red * f, green * f, blue * f, alpha)
}

// Accent lisible sur les cartes claires : certaines palettes ont un
// "secondary" très clair (ex. gris perle) qui serait invisible sur fond
// blanc -> on retombe alors sur l'encre sombre de la palette.
fun AppPalette.accentOnLight(): Color =
    if (secondary.luminance() > 0.6f) onLight else secondary

// Déclinaison lumineuse d'une palette (mode clair) : le fond devient la
// teinte claire du thème, mais les sections/cartes restent blanches pour
// l'harmonie. Les couleurs du thème (title, accent) sont conservées pour
// afficher les couleurs choisies par l'utilisateur.
fun AppPalette.asLight(): AppPalette = copy(
    background = lightBg,
    backgroundDark = lightBg.darken(0.15f),
    title = title,
    accent = accent,
    body = Color.White
)

// Déclinaison sombre d'une palette (mode sombre) : le corps des cartes
// utilise backgroundDark pour créer une distinction avec le fond principal.
fun AppPalette.asDark(): AppPalette = copy(
    body = backgroundDark
)

val AppPalettes: List<AppPalette> = listOf(
    AppPalette(
        id = "pictionis_classic",
        name = "PictIonis (défaut)",
        background = Color(0xFF001133), // Bleu foncé 8-bit
        backgroundDark = Color(0xFF000022),
        primary = Color(0xFF00FF00), // Vert pur 8-bit
        secondary = Color(0xFF00CC00),
        title = Color(0xFF00FF88),
        accent = Color(0xFF00FF66),
        lightBg = Color(0xFFE0FFE0),
        border = Color(0xFF00AA00),
        onLight = Color(0xFF001133),
        preview = listOf(
            Color(0xFF001133), Color(0xFF00FF00), Color(0xFF00CC00),
            Color(0xFF00FF88), Color(0xFFE0FFE0)
        ),
        iconColor = "green"
    ),
    AppPalette(
        id = "cherry_crush",
        name = "Cherry Crush",
        background = Color(0xFF330000), // Rouge foncé 8-bit
        backgroundDark = Color(0xFF220000),
        primary = Color(0xFFFF0000), // Rouge pur 8-bit
        secondary = Color(0xFFCC0000),
        title = Color(0xFFFFFFFF),
        accent = Color(0xFFFF6666),
        lightBg = Color(0xFFFFE0E0),
        border = Color(0xFFAA0000),
        onLight = Color(0xFF330000),
        preview = listOf(
            Color(0xFFFF0000), Color(0xFFCC0000), Color(0xFF990000),
            Color(0xFF660000), Color(0xFF330000)
        ),
        iconColor = "red"
    ),
    AppPalette(
        id = "cool_waters",
        name = "Cool Waters",
        background = Color(0xFF002244), // Cyan foncé 8-bit
        backgroundDark = Color(0xFF001133),
        primary = Color(0xFF00FFFF), // Cyan pur 8-bit
        secondary = Color(0xFF00CCCC),
        title = Color(0xFFAAFFFF),
        accent = Color(0xFF66FFFF),
        lightBg = Color(0xFFE0FFFF),
        border = Color(0xFF0099CC),
        onLight = Color(0xFF002244),
        preview = listOf(
            Color(0xFF002244), Color(0xFF00CCCC), Color(0xFF00FFFF),
            Color(0xFF66FFFF), Color(0xFFAAFFFF)
        ),
        iconColor = "green"
    ),
    AppPalette(
        id = "fiery_sunset",
        name = "Fiery Sunset",
        background = Color(0xFF331100), // Orange foncé 8-bit
        backgroundDark = Color(0xFF220800),
        primary = Color(0xFFFF8800), // Orange pur 8-bit
        secondary = Color(0xFFCC6600),
        title = Color(0xFFFFAA00),
        accent = Color(0xFFFFCC66),
        lightBg = Color(0xFFFFF0E0),
        border = Color(0xFFAA5500),
        onLight = Color(0xFF331100),
        preview = listOf(
            Color(0xFFFF8800), Color(0xFFCC6600), Color(0xFF994400),
            Color(0xFF662200), Color(0xFF331100)
        ),
        iconColor = "red"
    )
)

// Palette active fournie à tout l'arbre Compose (voir MainActivity).
val LocalAppPalette = compositionLocalOf { AppPalettes.first() }

fun paletteById(id: String?): AppPalette =
    AppPalettes.firstOrNull { it.id == id } ?: AppPalettes.first()
