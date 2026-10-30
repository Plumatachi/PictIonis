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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.etna.pictionis.data.SessionManager
import com.etna.pictionis.data.passwordErrorMessage

// Page "Mot de passe" : définit un nouveau mot de passe
// (mêmes règles qu'à l'inscription, confirmation obligatoire).
@Composable
fun PasswordScreen(
    onBack: () -> Unit = {}
) {
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    // Erreur affichée en direct sous le champ (règles de robustesse).
    val strengthError = passwordErrorMessage(newPassword).takeIf { newPassword.isNotEmpty() }
    val mismatchError =
        "Les mots de passe ne correspondent pas.".takeIf { confirm.isNotEmpty() && confirm != newPassword }

    LaunchedEffect(Unit) { SessionManager.clearMessages() }

    SettingsPageScaffold(
        title = "Mot de passe",
        subtitle = "Choisis un nouveau mot de passe",
        onBack = onBack
    ) {
        SettingsTextField(
            label = "Nouveau mot de passe",
            value = newPassword,
            onValueChange = { newPassword = it },
            placeholder = "••••••••••",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isError = strengthError != null,
            supportingText = strengthError
        )
        Spacer(modifier = Modifier.height(14.dp))
        SettingsTextField(
            label = "Confirme le mot de passe",
            value = confirm,
            onValueChange = { confirm = it },
            placeholder = "••••••••••",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isError = mismatchError != null,
            supportingText = mismatchError
        )
        Spacer(modifier = Modifier.height(20.dp))

        SessionManager.errorMessage?.let { SettingsErrorText(it) }
        SessionManager.infoMessage?.let { SettingsInfoText(it) }

        SettingsPrimaryButton(
            text = if (SessionManager.isLoading) "Patiente..." else "Mettre à jour",
            enabled = !SessionManager.isLoading &&
                newPassword.isNotEmpty() &&
                strengthError == null &&
                mismatchError == null &&
                confirm.isNotEmpty(),
            onClick = { SessionManager.updatePassword(newPassword) { onBack() } }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PasswordScreenPreview() {
    PasswordScreen()
}
