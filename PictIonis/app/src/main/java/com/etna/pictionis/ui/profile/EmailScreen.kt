package com.etna.pictionis.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.SessionManager
import com.etna.pictionis.data.emailErrorMessage
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.PictWarning
import com.etna.pictionis.ui.theme.accentOnLight
import com.etna.pictionis.ui.theme.contentColor

// Page "Adresse email" : affiche l'adresse + son statut de vérification,
// permet de (re)nvoyer le lien de vérification ou de changer d'adresse.
// Le changement prend effet après clic sur le lien reçu (Firebase).
@Composable
fun EmailScreen(
    email: String? = null,
    verified: Boolean = false,
    onBack: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    var newEmail by remember { mutableStateOf("") }
    val emailFormatError = emailErrorMessage(newEmail.trim()).takeIf { newEmail.isNotBlank() }

    LaunchedEffect(Unit) {
        SessionManager.clearMessages()
        SessionManager.refreshAccount()
    }

    SettingsPageScaffold(
        title = "Adresse email",
        subtitle = "Vérifie ton adresse ou change-la",
        onBack = onBack
    ) {
        // --- Carte statut : adresse actuelle + badge ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "Adresse actuelle",
                color = PictHint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = email ?: "—",
                    color = palette.onLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                VerificationBadge(verified = verified)
            }
            if (!verified) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Clique sur le lien reçu par mail, puis reviens ici.",
                    color = PictHint,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Envoyer le lien",
                        color = palette.accentOnLight(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            enabled = !SessionManager.isLoading,
                            onClick = { SessionManager.sendEmailVerification() }
                        )
                    )
                    Text(
                        text = "Actualiser",
                        color = palette.accentOnLight(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            enabled = !SessionManager.isLoading,
                            onClick = { SessionManager.refreshAccount() }
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Changement d'adresse ---
        SettingsTextField(
            label = "Nouvelle adresse",
            value = newEmail,
            onValueChange = { newEmail = it },
            placeholder = "nouveau@exemple.com",
            keyboardType = KeyboardType.Email,
            isError = emailFormatError != null,
            supportingText = emailFormatError
        )
        Spacer(modifier = Modifier.height(20.dp))

        SessionManager.errorMessage?.let { SettingsErrorText(it) }
        SessionManager.infoMessage?.let { SettingsInfoText(it) }

        SettingsPrimaryButton(
            text = if (SessionManager.isLoading) "Patiente..." else "Changer d'email",
            enabled = !SessionManager.isLoading && emailFormatError == null && newEmail.isNotBlank(),
            onClick = { SessionManager.changeEmail(newEmail) }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun VerificationBadge(verified: Boolean) {
    val badge = if (verified) LocalAppPalette.current.accentOnLight() else PictWarning
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(badge)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (verified) "Vérifié" else "À vérifier",
            color = badge.contentColor(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EmailScreenPreview() {
    EmailScreen(email = "lucie@exemple.com", verified = false)
}
