package com.etna.pictionis.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.etna.pictionis.ui.theme.AppPalettes
import com.etna.pictionis.ui.theme.paletteById

// Mode d'apparence de l'interface : suit le système par défaut.
enum class AppearanceMode(val label: String, val description: String) {
    SYSTEM("Système", "Suit le réglage du téléphone (défaut)"),
    LIGHT("Clair", "Interface lumineuse en permanence"),
    DARK("Sombre", "Interface sombre en permanence")
}

// Choix de palette de l'utilisateur, persisté en local (SharedPreferences).
// - currentPalette est un état Compose : tout l'arbre se recompose au changement.
// - Indépendant de Firebase Auth : le choix survit même déconnecté.
object ThemeSettings {

    private const val PREFS = "pictionis_theme"
    private const val KEY_PALETTE_ID = "palette_id"
    private const val KEY_APPEARANCE = "appearance_mode"

    private var appContext: Context? = null

    var currentPalette by mutableStateOf(AppPalettes.first())
        private set

    var appearanceMode by mutableStateOf(AppearanceMode.SYSTEM)
        private set

    // Appelé une fois dans MainActivity.onCreate, avant le premier setContent.
    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentPalette = paletteById(prefs?.getString(KEY_PALETTE_ID, null))
        appearanceMode = prefs?.getString(KEY_APPEARANCE, null)?.let { saved ->
            AppearanceMode.entries.firstOrNull { it.name == saved }
        } ?: AppearanceMode.SYSTEM
    }

    fun select(paletteId: String) {
        currentPalette = paletteById(paletteId)
        appContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_PALETTE_ID, currentPalette.id)
            ?.apply()
    }

    fun setAppearance(mode: AppearanceMode) {
        appearanceMode = mode
        appContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_APPEARANCE, mode.name)
            ?.apply()
    }
}
