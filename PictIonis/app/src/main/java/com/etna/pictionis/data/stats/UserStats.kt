package com.etna.pictionis.data.stats

import com.etna.pictionis.data.game.Difficulty
import com.google.firebase.database.DataSnapshot

// Statistiques globales d'un joueur
data class UserStats(
    val totalGamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val totalPoints: Long = 0,
    val totalWordsFound: Int = 0,
    val totalOpponents: Int = 0,
    val byDifficulty: Map<String, DifficultyStats> = emptyMap()
) {
    val winRate: Float get() = if (totalGamesPlayed > 0) gamesWon.toFloat() / totalGamesPlayed else 0f
    val lossRate: Float get() = if (totalGamesPlayed > 0) gamesLost.toFloat() / totalGamesPlayed else 0f

    fun toMap(): Map<String, Any> = mapOf(
        "totalGamesPlayed" to totalGamesPlayed,
        "gamesWon" to gamesWon,
        "gamesLost" to gamesLost,
        "totalPoints" to totalPoints,
        "totalWordsFound" to totalWordsFound,
        "totalOpponents" to totalOpponents,
        "byDifficulty" to byDifficulty.mapValues { it.value.toMap() }
    )

    companion object {
        fun from(snapshot: DataSnapshot): UserStats {
            val byDifficulty = snapshot.child("byDifficulty").children
                .mapNotNull { child -> child.key?.let { it to DifficultyStats.from(child) } }
                .toMap()
            return UserStats(
                totalGamesPlayed = snapshot.child("totalGamesPlayed").getValue(Int::class.java) ?: 0,
                gamesWon = snapshot.child("gamesWon").getValue(Int::class.java) ?: 0,
                gamesLost = snapshot.child("gamesLost").getValue(Int::class.java) ?: 0,
                totalPoints = snapshot.child("totalPoints").getValue(Long::class.java) ?: 0L,
                totalWordsFound = snapshot.child("totalWordsFound").getValue(Int::class.java) ?: 0,
                totalOpponents = snapshot.child("totalOpponents").getValue(Int::class.java) ?: 0,
                byDifficulty = byDifficulty
            )
        }

        fun fromMap(map: Map<String, Any>): UserStats {
            val byDifficultyMap = map["byDifficulty"] as? Map<String, Map<String, Any>> ?: emptyMap()
            val byDifficulty = byDifficultyMap.mapValues { (_, value) -> DifficultyStats.fromMap(value) }
            return UserStats(
                totalGamesPlayed = (map["totalGamesPlayed"] as? Number)?.toInt() ?: 0,
                gamesWon = (map["gamesWon"] as? Number)?.toInt() ?: 0,
                gamesLost = (map["gamesLost"] as? Number)?.toInt() ?: 0,
                totalPoints = (map["totalPoints"] as? Number)?.toLong() ?: 0L,
                totalWordsFound = (map["totalWordsFound"] as? Number)?.toInt() ?: 0,
                totalOpponents = (map["totalOpponents"] as? Number)?.toInt() ?: 0,
                byDifficulty = byDifficulty
            )
        }
    }
}

// Statistiques par niveau de difficulté
data class DifficultyStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val wordsFound: Int = 0,
    val pointsEarned: Long = 0,
    val opponentsFaced: Int = 0
) {
    val winRate: Float get() = if (gamesPlayed > 0) gamesWon.toFloat() / gamesPlayed else 0f

    fun toMap(): Map<String, Any> = mapOf(
        "gamesPlayed" to gamesPlayed,
        "gamesWon" to gamesWon,
        "wordsFound" to wordsFound,
        "pointsEarned" to pointsEarned,
        "opponentsFaced" to opponentsFaced
    )

    companion object {
        fun from(snapshot: DataSnapshot): DifficultyStats = DifficultyStats(
            gamesPlayed = snapshot.child("gamesPlayed").getValue(Int::class.java) ?: 0,
            gamesWon = snapshot.child("gamesWon").getValue(Int::class.java) ?: 0,
            wordsFound = snapshot.child("wordsFound").getValue(Int::class.java) ?: 0,
            pointsEarned = snapshot.child("pointsEarned").getValue(Long::class.java) ?: 0L,
            opponentsFaced = snapshot.child("opponentsFaced").getValue(Int::class.java) ?: 0
        )

        fun fromMap(map: Map<String, Any>): DifficultyStats = DifficultyStats(
            gamesPlayed = (map["gamesPlayed"] as? Number)?.toInt() ?: 0,
            gamesWon = (map["gamesWon"] as? Number)?.toInt() ?: 0,
            wordsFound = (map["wordsFound"] as? Number)?.toInt() ?: 0,
            pointsEarned = (map["pointsEarned"] as? Number)?.toLong() ?: 0L,
            opponentsFaced = (map["opponentsFaced"] as? Number)?.toInt() ?: 0
        )
    }
}

// Résultat d'une partie terminée, pour mise à jour des stats
data class GameResult(
    val uid: String,
    val difficulty: Difficulty,
    val isWinner: Boolean,
    val pointsEarned: Long,
    val wordsFound: Int,
    val opponentsCount: Int
)
