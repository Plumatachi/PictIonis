package com.etna.pictionis.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etna.pictionis.data.stats.StatsManager
import com.etna.pictionis.data.stats.UserStats
import com.etna.pictionis.ui.theme.LocalAppPalette
import com.etna.pictionis.ui.theme.PictHint
import com.etna.pictionis.ui.theme.PixelCard
import com.etna.pictionis.ui.theme.contentColor
import java.text.DecimalFormat

@Composable
fun StatsScreen() {
    val palette = LocalAppPalette.current
    val stats = StatsManager.stats
    val isLoading = StatsManager.isLoading
    val errorMessage = StatsManager.errorMessage

    LaunchedEffect(Unit) {
        StatsManager.loadStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Statistiques",
            color = palette.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tes performances en jeu",
            color = palette.accent,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            isLoading -> {
                Text(
                    text = "Chargement...",
                    color = PictHint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Erreur",
                    color = Color(0xFFB3261E),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            stats != null -> {
                StatsContent(stats = stats!!)
            }
            else -> {
                Text(
                    text = "Aucune statistique disponible pour le moment. Joue une partie pour commencer !",
                    color = PictHint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatsContent(stats: UserStats) {
    val palette = LocalAppPalette.current
    val percentFormat = DecimalFormat("#.#")

    // Stats globales
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.background)
            .padding(20.dp)
    ) {
        Text(
            text = "Global",
            color = palette.background.contentColor(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard(
                label = "Parties jouées",
                value = stats.totalGamesPlayed.toString(),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatCard(
                label = "Victoires",
                value = stats.gamesWon.toString(),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatCard(
                label = "Défaites",
                value = stats.gamesLost.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard(
                label = "Taux de victoire",
                value = "${percentFormat.format(stats.winRate * 100)}%",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatCard(
                label = "Points totaux",
                value = stats.totalPoints.toString(),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatCard(
                label = "Mots trouvés",
                value = stats.totalWordsFound.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatCard(
            label = "Adversaires rencontrés",
            value = stats.totalOpponents.toString(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Stats par difficulté
    Text(
        text = "Par difficulté",
        color = palette.title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))

    val difficulties = listOf("easy", "medium", "hard", "mixed")
    val difficultyLabels = mapOf(
        "easy" to "Facile",
        "medium" to "Moyen",
        "hard" to "Difficile",
        "mixed" to "Mélangé"
    )

    difficulties.forEach { diffKey ->
        val diffStats = stats.byDifficulty[diffKey]
        if (diffStats != null && diffStats.gamesPlayed > 0) {
            DifficultyStatsCard(
                label = difficultyLabels[diffKey] ?: diffKey,
                stats = diffStats
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (stats.byDifficulty.isEmpty()) {
        Text(
            text = "Aucune statistique par difficulté pour le moment.",
            color = PictHint,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    PixelCard(
        modifier = modifier,
        backgroundColor = palette.body,
        borderColor = palette.border
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = palette.body.contentColor(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = PictHint,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DifficultyStatsCard(
    label: String,
    stats: com.etna.pictionis.data.stats.DifficultyStats
) {
    val palette = LocalAppPalette.current
    val percentFormat = DecimalFormat("#.#")

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = palette.background,
        borderColor = palette.border
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                color = palette.background.contentColor(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniStat(label = "Parties", value = stats.gamesPlayed.toString())
                MiniStat(label = "Victoires", value = stats.gamesWon.toString())
                MiniStat(label = "Mots trouvés", value = stats.wordsFound.toString())
                MiniStat(label = "Points", value = stats.pointsEarned.toString())
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniStat(label = "Taux victoire", value = "${percentFormat.format(stats.winRate * 100)}%")
                MiniStat(label = "Adversaires", value = stats.opponentsFaced.toString())
            }
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String
) {
    val palette = LocalAppPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = palette.background.contentColor(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            color = PictHint,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatsScreenPreview() {
    StatsScreen()
}
