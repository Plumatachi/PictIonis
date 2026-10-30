package com.etna.pictionis.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.R
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PixelButton
import com.etna.pictionis.ui.theme.PixelOutlinedButton
import com.etna.pictionis.ui.theme.contentColor

@Composable
fun HomeScreen(
    isLoggedIn: Boolean = false,
    showFooter: Boolean = true,
    onPlayWithFriends: () -> Unit = {},
    onJoinGame: () -> Unit = {},
    onQuickGame: () -> Unit = {},
    onSignUp: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val onBackground = palette.background.contentColor()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo : icône de l'application selon le thème (rouge ou verte)
        val iconRes = when (palette.iconColor) {
            "red" -> R.drawable.app_icon
            "green" -> R.drawable.app_icon_green
            else -> R.drawable.app_icon
        }
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconRes),
                contentDescription = "PictIonis Logo",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "PictIonis",
            color = palette.title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "dessine, devine, gagne",
            color = palette.accent,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(56.dp))

        // Boutons
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PixelButton(
                onClick = onPlayWithFriends,
                modifier = Modifier.fillMaxWidth(),
                text = "Jouer avec des amis",
                backgroundColor = palette.primary,
                textColor = palette.primary.contentColor()
            )

            PixelButton(
                onClick = onJoinGame,
                modifier = Modifier.fillMaxWidth(),
                text = "Rejoindre une partie",
                backgroundColor = palette.secondary,
                textColor = palette.secondary.contentColor()
            )

            PixelOutlinedButton(
                onClick = onQuickGame,
                modifier = Modifier.fillMaxWidth(),
                text = "Partie rapide",
                borderColor = palette.primary,
                textColor = palette.title
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bas de page : masqué dans la coquille connectée (déconnexion sur le profil).
        // Sinon : inscription si déconnecté, état connecté sinon.
        if (showFooter) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoggedIn) {
                    Text(
                        text = "Connecté  ",
                        color = palette.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Se déconnecter",
                        color = onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onSignOut)
                    )
                } else {
                    Text(
                        text = "Pas de compte ?  ",
                        color = palette.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Inscris-toi",
                        color = onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onSignUp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
