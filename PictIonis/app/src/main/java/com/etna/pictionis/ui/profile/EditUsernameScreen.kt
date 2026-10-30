package com.etna.pictionis.ui.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.etna.pictionis.data.SessionManager

// Page "Nom d'utilisateur" : modifie le pseudo (displayName Firebase).
@Composable
fun EditUsernameScreen(
    currentName: String? = null,
    onBack: () -> Unit = {}
) {
    var name by remember(currentName) { mutableStateOf(currentName ?: "") }

    LaunchedEffect(Unit) { SessionManager.clearMessages() }

    SettingsPageScaffold(
        title = "Nom d'utilisateur",
        subtitle = "Ton pseudo visible par les autres joueurs",
        onBack = onBack
    ) {
        SettingsTextField(
            label = "Pseudo",
            value = name,
            onValueChange = { if (it.length <= 24) name = it },
            placeholder = "Ton pseudo"
        )
        Spacer(modifier = Modifier.height(20.dp))

        SessionManager.errorMessage?.let { SettingsErrorText(it) }
        SessionManager.infoMessage?.let { SettingsInfoText(it) }

        SettingsPrimaryButton(
            text = if (SessionManager.isLoading) "Patiente..." else "Enregistrer",
            enabled = !SessionManager.isLoading && name.isNotBlank(),
            onClick = { SessionManager.updateDisplayName(name) { onBack() } }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditUsernameScreenPreview() {
    EditUsernameScreen(currentName = "Lucie.M")
}
