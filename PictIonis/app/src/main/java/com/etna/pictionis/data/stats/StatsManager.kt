package com.etna.pictionis.data.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.etna.pictionis.data.game.Difficulty
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

// Gestion des statistiques utilisateur dans Firebase
object StatsManager {
    
    private const val DATABASE_URL =
        "https://pictionis-f5fe5-default-rtdb.europe-west1.firebasedatabase.app"
    
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance(DATABASE_URL) }
    private val root: DatabaseReference get() = database.reference
    private val users: DatabaseReference get() = root.child("users")
    
    val myUid: String? get() = FirebaseAuth.getInstance().currentUser?.uid
    
    var stats by mutableStateOf<UserStats?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    private var statsListener: ValueEventListener? = null
    private var statsRef: DatabaseReference? = null
    
    fun clearMessages() {
        errorMessage = null
    }
    
    // Charge les stats de l'utilisateur connecté
    fun loadStats() {
        val uid = myUid ?: return
        clearMessages()
        isLoading = true
        stats = null
        
        val ref = users.child(uid).child("stats")
        statsRef = ref
        
        statsListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLoading = false
                stats = if (snapshot.exists()) UserStats.from(snapshot) else UserStats()
            }
            
            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                errorMessage = error.toException().message ?: "Erreur de chargement des statistiques"
            }
        })
    }
    
    // Arrête d'écouter les stats (quand on quitte l'écran)
    fun stopWatchingStats() {
        statsListener?.let { statsRef?.removeEventListener(it) }
        statsListener = null
        statsRef = null
    }
    
    // Met à jour les stats après une partie terminée
    fun updateStats(result: GameResult) {
        val uid = myUid ?: return
        val ref = users.child(uid).child("stats")
        
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(data: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val current = if (data.value != null) UserStats.fromMap(data.value as? Map<String, Any> ?: emptyMap()) else UserStats()
                
                val updated = current.copy(
                    totalGamesPlayed = current.totalGamesPlayed + 1,
                    gamesWon = if (result.isWinner) current.gamesWon + 1 else current.gamesWon,
                    gamesLost = if (!result.isWinner) current.gamesLost + 1 else current.gamesLost,
                    totalPoints = current.totalPoints + result.pointsEarned,
                    totalWordsFound = current.totalWordsFound + result.wordsFound,
                    totalOpponents = current.totalOpponents + result.opponentsCount,
                    byDifficulty = current.byDifficulty.toMutableMap().apply {
                        val diffKey = result.difficulty.key
                        val diffStats = getOrDefault(diffKey, DifficultyStats())
                        put(diffKey, diffStats.copy(
                            gamesPlayed = diffStats.gamesPlayed + 1,
                            gamesWon = if (result.isWinner) diffStats.gamesWon + 1 else diffStats.gamesWon,
                            wordsFound = diffStats.wordsFound + result.wordsFound,
                            pointsEarned = diffStats.pointsEarned + result.pointsEarned,
                            opponentsFaced = diffStats.opponentsFaced + result.opponentsCount
                        ))
                    }
                )
                
                data.value = updated.toMap()
                return com.google.firebase.database.Transaction.success(data)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    errorMessage = "Erreur lors de la mise à jour des statistiques"
                }
            }
        })
    }
    
    // Réinitialise toutes les stats (optionnel, pour tests)
    fun resetStats() {
        val uid = myUid ?: return
        users.child(uid).child("stats").removeValue()
    }
}
