package com.etna.pictionis.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.emailErrorMessage
import com.etna.pictionis.data.passwordRules
import com.etna.pictionis.data.passwordStrength
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictDanger
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.PictWarning
import com.etna.pictionis.ui.theme.PixelButton
import com.etna.pictionis.ui.theme.PixelOutlinedButton
import com.etna.pictionis.ui.theme.PixelTextField
import com.etna.pictionis.ui.theme.accentOnLight
import com.etna.pictionis.ui.theme.contentColor

// Écran connexion / inscription branché sur Firebase Auth via les callbacks.
@Composable
fun AuthScreen(
    startOnSignUpTab: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    infoMessage: String? = null,
    onBack: () -> Unit = {},
    onTabChange: () -> Unit = {},
    onSubmit: (isSignUp: Boolean, email: String, password: String) -> Unit = { _, _, _ -> },
    onForgotPassword: (email: String) -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onAppleSignIn: () -> Unit = {}
) {
    var isSignUp by remember(startOnSignUpTab) { mutableStateOf(startOnSignUpTab) }
    var pseudo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Erreur de format en direct : rouge + message dès que le texte saisi
    // n'est pas un email valide (mais pas quand le champ est encore vide).
    val emailFormatError = emailErrorMessage(email.trim())?.takeIf { email.isNotBlank() }

    // Bouton retour système -> revient à l'accueil
    BackHandler(onBack = onBack)

    val palette = LocalAppPalette.current
    val accentOnLight = palette.accentOnLight()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // --- Header aux couleurs de la palette active ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 20.dp)
        ) {
            Text(
                text = "Bienvenue",
                color = palette.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Connecte-toi pour jouer",
                color = palette.accent,
                fontSize = 15.sp
            )
        }

        // --- Corps aux coins hauts arrondis ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(palette.body)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Sélecteur Connexion / Inscription
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.border)
                    .padding(4.dp)
            ) {
                AuthTab(
                    title = "Connexion",
                    selected = !isSignUp,
                    onClick = {
                        isSignUp = false
                        onTabChange()
                    },
                    modifier = Modifier.weight(1f)
                )
                AuthTab(
                    title = "Inscription",
                    selected = isSignUp,
                    onClick = {
                        isSignUp = true
                        onTabChange()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isSignUp) {
                AuthField(
                    label = "Pseudo",
                    value = pseudo,
                    onValueChange = { pseudo = it },
                    placeholder = "ton pseudo",
                    keyboardType = KeyboardType.Text
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            AuthField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                placeholder = "toi@exemple.com",
                keyboardType = KeyboardType.Email,
                isError = emailFormatError != null,
                supportingText = emailFormatError
            )
            Spacer(modifier = Modifier.height(14.dp))

            AuthField(
                label = "Mot de passe",
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••••",
                keyboardType = KeyboardType.Password,
                isPassword = true
            )

            // Jauge de robustesse, visible sur l'onglet inscription
            if (isSignUp && password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                PasswordStrengthMeter(password = password)
            }

            // Checklist des règles, visible sur l'onglet inscription
            if (isSignUp) {
                Spacer(modifier = Modifier.height(8.dp))
                PasswordChecklist(password = password)
            }

            if (!isSignUp) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mot de passe oublié ?",
                    color = accentOnLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = { onForgotPassword(email) })
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Erreur renvoyée par Firebase (ex : mot de passe incorrect)
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Confirmation (ex : email de réinitialisation envoyé)
            if (infoMessage != null) {
                Text(
                    text = infoMessage,
                    color = accentOnLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            PixelButton(
                onClick = { onSubmit(isSignUp, email, password) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                text = if (isLoading) "Patiente..." else if (isSignUp) "S'inscrire" else "Se connecter",
                backgroundColor = palette.primary,
                textColor = palette.primary.contentColor()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ou continue avec",
                color = PictHint,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PixelOutlinedButton(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.weight(1f),
                    text = "G",
                    borderColor = palette.border,
                    textColor = palette.body.contentColor()
                )
                PixelOutlinedButton(
                    onClick = onAppleSignIn,
                    modifier = Modifier.weight(1f),
                    text = "",
                    borderColor = palette.border,
                    textColor = palette.body.contentColor()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AuthTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.secondary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) palette.secondary.contentColor() else palette.body.contentColor()
        )
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null
) {
    // Affichage du mot de passe en clair ou masqué (œil dans le champ).
    var passwordVisible by remember { mutableStateOf(false) }
    val palette = LocalAppPalette.current

    Text(
        text = label,
        color = palette.body.contentColor(),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))
    PixelTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        borderColor = palette.border,
        focusedBorderColor = palette.secondary,
        textColor = palette.background.contentColor(),
        backgroundColor = palette.background,
        modifier = Modifier.fillMaxWidth()
    )
}

// Jauge de robustesse : 4 segments colorés + libellé (Faible -> Excellent).
// Les couleurs suivent le niveau atteint : rouge, orange, vert, sarcelle.
@Composable
private fun PasswordStrengthMeter(password: String) {
    val palette = LocalAppPalette.current
    val strength = passwordStrength(password)
    val levelColors = listOf(PictDanger, PictWarning, palette.primary, palette.secondary)
    val levelColor = levelColors[strength.level]

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Sécurité : ", color = PictHint, fontSize = 13.sp)
            Text(
                text = strength.label,
                color = levelColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index <= strength.level) levelColor else palette.border)
                )
            }
        }
    }
}

// Checklist des règles du mot de passe, mise à jour à chaque frappe.
// Chaque règle passe au vert quand elle est respectée.
@Composable
private fun PasswordChecklist(password: String) {
    val accentOnLight = LocalAppPalette.current.accentOnLight()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        passwordRules(password).forEach { rule ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (rule.passed) "✓ " else "• ",
                    color = if (rule.passed) accentOnLight else PictHint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = rule.label,
                    color = if (rule.passed) accentOnLight else PictHint,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// Croix "vider le champ" : deux traits diagonaux.
@Composable
private fun ClearGlyph() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.14f, cap = StrokeCap.Round)
        val margin = w * 0.24f
        drawLine(
            color = PictHint,
            start = Offset(margin, margin),
            end = Offset(w - margin, w - margin),
            strokeWidth = stroke.width,
            cap = stroke.cap
        )
        drawLine(
            color = PictHint,
            start = Offset(w - margin, margin),
            end = Offset(margin, w - margin),
            strokeWidth = stroke.width,
            cap = stroke.cap
        )
    }
}

// Œil "voir le mot de passe" : ovale + pupille, barré quand masqué.
// Passe à la couleur d'accent quand le mot de passe est visible.
@Composable
private fun EyeGlyph(visible: Boolean) {
    val palette = LocalAppPalette.current
    val color = if (visible) palette.accentOnLight() else PictHint
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val outline = Stroke(width = w * 0.09f)
        // Amande de l'œil
        drawOval(
            color = color,
            topLeft = Offset(w * 0.07f, h * 0.28f),
            size = Size(w * 0.86f, h * 0.44f),
            style = outline
        )
        // Pupille
        drawCircle(color = color, radius = w * 0.11f, center = center)
        // Barre en diagonale quand le mot de passe est masqué
        if (!visible) {
            drawLine(
                color = color,
                start = Offset(w * 0.14f, h * 0.86f),
                end = Offset(w * 0.86f, h * 0.14f),
                strokeWidth = w * 0.09f,
                cap = StrokeCap.Round
            )
        }
    }
}

// Pomme dessinée en Canvas pour éviter une dépendance d'icônes
@Composable
private fun AppleIcon() {
    val palette = LocalAppPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val navy = palette.background.contentColor()
            val pixelSize = size.width * 0.12f

            // Corps de la pomme - style pixel avec carrés
            // Partie gauche
            drawRect(
                color = navy,
                size = Size(pixelSize * 2, pixelSize * 3),
                topLeft = Offset(size.width * 0.24f, size.height * 0.44f)
            )
            // Partie droite
            drawRect(
                color = navy,
                size = Size(pixelSize * 2, pixelSize * 3),
                topLeft = Offset(size.width * 0.52f, size.height * 0.44f)
            )
            // Centre
            drawRect(
                color = navy,
                size = Size(pixelSize * 2, pixelSize * 2),
                topLeft = Offset(size.width * 0.38f, size.height * 0.50f)
            )
            // Tige - ligne verticale
            drawLine(
                color = navy,
                start = Offset(size.width * 0.50f, size.height * 0.32f),
                end = Offset(size.width * 0.50f, size.height * 0.14f),
                strokeWidth = pixelSize,
                cap = StrokeCap.Square
            )
            // Feuille - carré
            drawRect(
                color = navy,
                size = Size(pixelSize, pixelSize),
                topLeft = Offset(size.width * 0.62f, size.height * 0.14f)
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenSignInPreview() {
    AuthScreen(startOnSignUpTab = false)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenSignUpPreview() {
    AuthScreen(startOnSignUpTab = true)
}
