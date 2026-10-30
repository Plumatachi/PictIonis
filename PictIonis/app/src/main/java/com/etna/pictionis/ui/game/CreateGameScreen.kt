package com.etna.pictionis.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.game.Difficulty
import com.etna.pictionis.data.game.GameManager
import com.etna.pictionis.data.game.GameMode
import com.etna.pictionis.data.game.GameSettings
import com.etna.pictionis.data.game.WORD_CHOICES
import com.etna.pictionis.data.game.parseCustomWords
import com.etna.pictionis.ui.profile.SettingsErrorText
import com.etna.pictionis.ui.profile.SettingsPageScaffold
import com.etna.pictionis.ui.profile.SettingsPrimaryButton
import com.etna.pictionis.ui.profile.SettingsTextField
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint

// Création d'une partie : nom + réglages, puis salle d'attente.
@Composable
fun CreateGameScreen(onBack: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var settings by remember { mutableStateOf(GameSettings()) }
    var customInput by remember { mutableStateOf("") }
    val customWords = remember(customInput) { parseCustomWords(customInput) }

    LaunchedEffect(Unit) { GameManager.clearMessages() }

    SettingsPageScaffold(
        title = "Nouvelle partie",
        subtitle = "Choisis les règles puis invite tes amis",
        onBack = onBack
    ) {
        SettingsTextField(
            label = "Nom de la partie",
            value = name,
            onValueChange = { if (it.length <= 30) name = it },
            placeholder = "Partie de ${GameManager.myName()}"
        )
        Spacer(modifier = Modifier.height(20.dp))

        OptionChips(
            label = "Mode de jeu",
            options = GameMode.entries,
            selected = settings.mode,
            labelOf = { it.label },
            onSelect = { settings = settings.copy(mode = it) }
        )
        Text(
            text = settings.mode.description,
            color = PictHint,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OptionChips(
            label = "Difficulté des mots",
            options = Difficulty.entries,
            selected = settings.difficulty,
            labelOf = { it.label },
            onSelect = { settings = settings.copy(difficulty = it) }
        )
        OptionChips(
            label = "Manches (chaque joueur dessine une fois par manche)",
            options = listOf(1, 2, 3, 4, 5),
            selected = settings.rounds,
            labelOf = { "$it" },
            onSelect = { settings = settings.copy(rounds = it) }
        )
        OptionChips(
            label = "Durée d'un tour",
            options = listOf(30, 60, 90, 120),
            selected = settings.roundSeconds,
            labelOf = { "$it s" },
            onSelect = { settings = settings.copy(roundSeconds = it) }
        )

        SettingsTextField(
            label = "Mots personnalisés (facultatif)",
            value = customInput,
            onValueChange = { if (it.length <= 1000) customInput = it },
            placeholder = "licorne, tour Eiffel, prof de maths",
            supportingText = "${customWords.size} mot(s) ajouté(s) aux propositions, séparés par des virgules"
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (customWords.size >= WORD_CHOICES) {
            SwitchRow(
                title = "Uniquement mes mots",
                subtitle = "Ignore la liste de mots du jeu",
                checked = settings.customOnly,
                onCheckedChange = { settings = settings.copy(customOnly = it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        SwitchRow(
            title = "Partie publique",
            subtitle = if (settings.isPublic) "Visible dans la liste des parties"
            else "Accessible uniquement avec le code",
            checked = settings.isPublic,
            onCheckedChange = { settings = settings.copy(isPublic = it) }
        )
        Spacer(modifier = Modifier.height(24.dp))

        GameManager.errorMessage?.let { SettingsErrorText(it) }

        SettingsPrimaryButton(
            text = if (GameManager.isLoading) "Patiente..." else "Créer la partie",
            enabled = !GameManager.isLoading,
            onClick = {
                GameManager.createGame(
                    name,
                    settings.copy(
                        customWords = customWords,
                        customOnly = settings.customOnly && customWords.size >= WORD_CHOICES
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.onLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = subtitle, color = PictHint, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = palette.secondary)
        )
    }
}
