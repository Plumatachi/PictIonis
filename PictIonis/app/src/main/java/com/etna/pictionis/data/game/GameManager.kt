package com.etna.pictionis.data.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.etna.pictionis.data.SessionManager
import com.etna.pictionis.data.stats.GameResult
import com.etna.pictionis.data.stats.StatsManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

// Parties multijoueur branchées sur Firebase Realtime Database.
// Même principe que SessionManager : des états Compose lus directement par les
// écrans et tenus à jour par les listeners temps réel. L'écran affiché dépend
// du statut de la partie, donc tous les terminaux changent d'écran ensemble.
//
// Arborescence (règles de sécurité : database.rules.json) :
//   games/{code}     état de la partie : joueurs, statut, tour en cours
//   drawings/{code}  traits du dessin en cours
//   chats/{code}     messages du chat
object GameManager {

    // shortcut: URL en dur (base créée en europe-west1), à changer si la base est recréée ailleurs.
    private const val DATABASE_URL =
        "https://pictionis-f5fe5-default-rtdb.europe-west1.firebasedatabase.app"

    private const val CHAT_HISTORY = 200

    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance(DATABASE_URL) }
    private val root: DatabaseReference get() = database.reference
    private val games: DatabaseReference get() = root.child("games")

    val myUid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // Partie rejointe (null = pas en partie).
    var game by mutableStateOf<Game?>(null)
        private set

    // Parties publiques en attente ou en cours, pour l'écran "Rejoindre".
    var openGames by mutableStateOf<List<Game>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Traits du dessin en cours, dans l'ordre de création.
    var strokes by mutableStateOf<List<Stroke>>(emptyList())
        private set

    // Derniers messages du chat, du plus ancien au plus récent.
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    private var gameRef: DatabaseReference? = null
    private var gameListener: ValueEventListener? = null
    private var connectedListener: ValueEventListener? = null
    private var drawingRef: DatabaseReference? = null
    private var drawingListener: ChildEventListener? = null
    private var chatRef: DatabaseReference? = null
    private var chatQuery: Query? = null
    private var chatListener: ChildEventListener? = null
    private var offsetListener: ValueEventListener? = null
    private var endingTurn = -1
    private var choosingTurn = -1

    // Décalage horloge locale / serveur : tous les minuteurs restent alignés.
    private var serverTimeOffset = 0L
    fun serverNow(): Long = System.currentTimeMillis() + serverTimeOffset
    private val openGamesListeners = mutableListOf<Pair<Query, ValueEventListener>>()

    fun clearMessages() {
        errorMessage = null
    }

    // Pseudo affiché aux autres joueurs : même règle que la page profil.
    fun myName(): String =
        SessionManager.accountDisplayName?.takeIf { it.isNotBlank() }
            ?: SessionManager.accountEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Joueur"

    // --- Création / rejoindre / quitter ---

    fun createGame(name: String, settings: GameSettings) {
        val uid = myUid ?: return
        clearMessages()
        val code = generateGameCode()
        val data = mapOf(
            "name" to name.trim().ifEmpty { "Partie de ${myName()}" },
            "hostUid" to uid,
            "status" to GameStatus.LOBBY.key,
            "createdAt" to ServerValue.TIMESTAMP,
            "settings" to settings.toMap(),
            "players" to mapOf(uid to newPlayer(score = 0))
        )
        isLoading = true
        // Les règles refusent d'écraser une partie existante (code déjà pris).
        games.child(code).setValue(data)
            .addOnSuccessListener {
                attach(code)
                postSystem("${myName()} a créé la partie")
            }
            .addOnFailureListener { errorMessage = it.toUserMessage() }
            .addOnCompleteListener { isLoading = false }
    }

    fun joinGame(input: String) {
        val uid = myUid ?: return
        clearMessages()
        val code = normalizeGameCode(input)
        if (!isValidGameCode(code)) {
            errorMessage = "Le code fait $CODE_LENGTH caractères (lettres et chiffres)."
            return
        }
        isLoading = true
        games.child(code).get()
            .addOnSuccessListener { snapshot ->
                val target = parseGame(snapshot)
                val known = target?.player(uid)
                errorMessage = when {
                    target == null -> "Aucune partie avec ce code."
                    target.status == GameStatus.FINISHED -> "Cette partie est terminée."
                    known == null && target.onlinePlayers.size >= MAX_PLAYERS -> "La partie est complète."
                    else -> null
                }
                if (target == null || errorMessage != null) {
                    isLoading = false
                    return@addOnSuccessListener
                }
                // Un joueur déjà connu (app fermée, réseau coupé) retrouve son score.
                games.child(code).child("players").child(uid)
                    .setValue(newPlayer(score = known?.score ?: 0))
                    .addOnSuccessListener {
                        if (target.status == GameStatus.PLAYING && uid !in target.order) {
                            appendToOrder(code, uid)
                        }
                        attach(code)
                        postSystem(
                            if (known == null) "${myName()} a rejoint la partie"
                            else "${myName()} est de retour"
                        )
                    }
                    .addOnFailureListener { errorMessage = it.toUserMessage() }
                    .addOnCompleteListener { isLoading = false }
            }
            .addOnFailureListener {
                errorMessage = it.toUserMessage()
                isLoading = false
            }
    }

    // Partie rapide : la partie publique en attente la plus remplie, sinon on en crée une.
    fun quickGame() {
        if (myUid == null) return
        clearMessages()
        isLoading = true
        games.orderByChild("status").equalTo(GameStatus.LOBBY.key).get()
            .addOnSuccessListener { snapshot ->
                isLoading = false
                val target = snapshot.children.mapNotNull(::parseGame)
                    .filter { it.settings.isPublic && it.onlinePlayers.size in 1 until MAX_PLAYERS }
                    .maxByOrNull { it.onlinePlayers.size }
                if (target != null) joinGame(target.id) else createGame("Partie rapide", GameSettings())
            }
            .addOnFailureListener {
                errorMessage = it.toUserMessage()
                isLoading = false
            }
    }

    fun leaveGame() {
        val current = game ?: return
        val uid = myUid ?: return
        val ref = games.child(current.id)
        detach()
        game = null
        ref.child("players").child(uid).child("online").onDisconnect().cancel()

        val others = current.players.filter { it.uid != uid }
        if (others.none { it.online }) {
            // Dernier joueur connecté : on supprime la partie et ses données.
            root.updateChildren(
                mapOf(
                    "games/${current.id}" to null,
                    "drawings/${current.id}" to null,
                    "chats/${current.id}" to null
                )
            )
            return
        }
        // Message envoyé avant le retrait : les règles exigent d'être encore joueur.
        postSystem("${myName()} a quitté la partie", current.id)
        val updates = mutableMapOf<String, Any?>("players/$uid" to null)
        if (current.hostUid == uid) {
            updates["hostUid"] = others.first { it.online }.uid
        }
        ref.updateChildren(updates)
    }

    // --- Chat ---

    fun sendMessage(input: String) {
        val text = input.trim().take(MAX_MESSAGE_LENGTH)
        if (text.isEmpty()) return
        pushMessage(text, MessageKind.CHAT)
    }

    private fun postSystem(text: String, code: String? = null) =
        pushMessage(text, MessageKind.SYSTEM, code)

    private fun pushMessage(text: String, kind: MessageKind, code: String? = null) {
        val uid = myUid ?: return
        val ref = code?.let { root.child("chats").child(it) } ?: chatRef ?: return
        ref.push().setValue(
            mapOf(
                "uid" to uid,
                "name" to myName(),
                "text" to text,
                "kind" to kind.key,
                "ts" to ServerValue.TIMESTAMP
            )
        )
    }

    // --- Déroulement de la partie ---

    // Lancement (hôte uniquement) : ordre de passage tiré au sort, premier tour.
    fun startGame() {
        val current = game ?: return
        if (current.hostUid != myUid) return
        val order = current.onlinePlayers.map { it.uid }.shuffled()
        if (order.size < MIN_PLAYERS) {
            errorMessage = "Il faut au moins $MIN_PLAYERS joueurs connectés."
            return
        }
        root.child("drawings").child(current.id).removeValue()
        games.child(current.id).updateChildren(
            mapOf(
                "status" to GameStatus.PLAYING.key,
                "order" to order,
                "totalTurns" to current.settings.rounds * order.size,
                "round" to newRound(0, order.first(), current.settings, emptySet())
            )
        )
            .addOnSuccessListener {
                postSystem("La partie commence ! ${current.player(order.first())?.name} choisit un mot")
            }
            .addOnFailureListener { errorMessage = it.toUserMessage() }
    }

    // Appelé régulièrement par l'écran de jeu de chaque joueur : termine le tour
    // quand le temps est écoulé, quand tous ont trouvé ou quand le dessinateur part.
    // N'importe quel terminal peut passer au tour suivant (pas de serveur de jeu).
    fun tick() {
        val current = game ?: return
        if (current.status != GameStatus.PLAYING) return
        val round = current.round ?: return
        val guessers = current.onlinePlayers.filter { it.uid != round.drawerUid }
        val drawerGone = current.player(round.drawerUid) == null || current.players.size < MIN_PLAYERS
        if (round.isChoosing && !drawerGone) {
            // Dessinateur trop lent à choisir : on prend la première proposition.
            if (serverNow() >= round.endsAt) chooseWord(round.choices.first())
            return
        }
        val over = drawerGone || serverNow() >= round.endsAt ||
            (guessers.isNotEmpty() && guessers.all { it.uid in round.found }) ||
            (current.settings.mode == GameMode.RUSH && round.found.isNotEmpty())
        if (over) endTurn(current, round)
    }

    // Choix du mot parmi les propositions : le chrono du tour démarre à ce moment.
    fun chooseWord(word: String) {
        val current = game ?: return
        val round = current.round ?: return
        if (!round.isChoosing || word !in round.choices || choosingTurn == round.turn) return
        choosingTurn = round.turn
        val ref = games.child(current.id)
        ref.child("round").runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                val turn = (data.child("turn").value as? Number)?.toInt()
                val chosen = data.child("word").getValue(String::class.java).orEmpty()
                if (turn != round.turn || chosen.isNotEmpty()) return Transaction.abort()
                data.child("word").value = word
                data.child("startedAt").value = serverNow()
                return Transaction.success(data)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) choosingTurn = -1 // on retentera au prochain tick
                if (committed) ref.child("usedWords").child(normalizeGuess(word)).setValue(true)
            }
        })
    }

    // Message du chat. Une bonne réponse n'est pas affichée : elle rapporte des
    // points au joueur et au dessinateur.
    fun submitGuess(input: String) {
        val text = input.trim().take(MAX_MESSAGE_LENGTH)
        if (text.isEmpty()) return
        errorMessage = null
        val current = game
        val round = current?.round
        val uid = myUid
        if (current == null || round == null || uid == null ||
            current.status != GameStatus.PLAYING || round.word.isEmpty()
        ) {
            sendMessage(text)
            return
        }
        if (uid == round.drawerUid || uid in round.found) {
            if (revealsWord(text, round.word)) errorMessage = "Chut ! Ne donne pas le mot aux autres."
            else sendMessage(text)
            return
        }
        if (!isCorrectGuess(text, round.word)) {
            sendMessage(text)
            return
        }
        val points = guessPoints(round.endsAt - serverNow(), round.durationMs)
        val rush = current.settings.mode == GameMode.RUSH
        val ref = games.child(current.id)
        // Transaction : un seul gain par joueur et uniquement pour le tour en cours
        // (en mode Éclair, uniquement pour le premier qui trouve).
        ref.child("round").runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                val turn = (data.child("turn").value as? Number)?.toInt()
                if (turn != round.turn || data.child("found").hasChild(uid)) return Transaction.abort()
                if (rush && data.child("found").childrenCount > 0) return Transaction.abort()
                data.child("found").child(uid).value = points
                return Transaction.success(data)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (!committed) return
                ref.child("players").child(uid).child("score")
                    .setValue(ServerValue.increment(points.toLong()))
                ref.child("players").child(uid).child("wordsFound")
                    .setValue(ServerValue.increment(1))
                ref.child("players").child(round.drawerUid).child("score")
                    .setValue(ServerValue.increment(DRAWER_POINTS_PER_FIND.toLong()))
                pushMessage("${myName()} a trouvé le mot ! (+$points)", MessageKind.FOUND)
            }
        })
    }

    // Nouveau tour en phase de choix : le dessinateur reçoit des propositions.
    private fun newRound(turn: Int, drawerUid: String, settings: GameSettings, used: Set<String>) = mapOf(
        "turn" to turn,
        "drawerUid" to drawerUid,
        "word" to "",
        "choices" to generateChoices(wordPool(settings), used),
        "startedAt" to serverNow(),
        "durationMs" to settings.roundSeconds * 1000L
    )

    // Passe au tour suivant (ou termine la partie). Plusieurs terminaux peuvent
    // le demander en même temps : la transaction garantit un seul passage.
    private fun endTurn(current: Game, round: Round) {
        if (endingTurn == round.turn) return
        endingTurn = round.turn
        val available = current.onlinePlayers.map { it.uid }.toSet()
        val nextTurn = round.turn + 1
        val nextDrawer = drawerForTurn(current.order, available, nextTurn)
        val finished = nextTurn >= current.totalTurns || available.size < MIN_PLAYERS || nextDrawer == null
        val next = if (finished || nextDrawer == null) {
            mapOf("turn" to nextTurn, "drawerUid" to "", "word" to "")
        } else {
            newRound(nextTurn, nextDrawer, current.settings, current.usedWords)
        }
        val ref = games.child(current.id)
        ref.child("round").runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if ((data.child("turn").value as? Number)?.toInt() != round.turn) return Transaction.abort()
                data.value = next
                return Transaction.success(data)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) endingTurn = -1 // on retentera au prochain tick
                if (!committed) return
                if (round.word.isNotEmpty()) postSystem("Le mot était : ${round.word}", current.id)
                root.child("drawings").child(current.id).removeValue()
                if (finished) {
                    ref.child("status").setValue(GameStatus.FINISHED.key)
                    postSystem("Partie terminée !", current.id)
                    // Sauvegarder les stats à la fin de la partie
                    saveGameStats(current)
                } else {
                    postSystem(
                        "Tour ${nextTurn + 1}/${current.totalTurns} : ${current.player(nextDrawer)?.name} choisit un mot",
                        current.id
                    )
                }
            }
        })
    }

    // --- Dessin (les règles n'autorisent que le dessinateur du tour) ---

    // Crée le trait avec ses premiers points et renvoie son id.
    fun beginStroke(color: Long, width: Float, tool: StrokeTool, points: List<Offset>): String? {
        val ref = drawingRef?.push() ?: return null
        ref.setValue(
            mapOf("c" to color, "w" to width, "t" to tool.key, "p" to mapOf("0" to encodePoints(points)))
        )
        return ref.key
    }

    // Ajoute un paquet de points au trait : les autres le voient se tracer en direct.
    fun appendStrokePoints(strokeId: String, chunkIndex: Int, points: List<Offset>) {
        if (points.isEmpty()) return
        drawingRef?.child(strokeId)?.child("p")?.child(chunkIndex.toString())
            ?.setValue(encodePoints(points))
    }

    fun clearDrawing() {
        drawingRef?.removeValue()
    }

    // --- Liste des parties publiques ---

    fun watchOpenGames() {
        stopWatchingOpenGames()
        val byStatus = mutableMapOf<GameStatus, List<Game>>()
        listOf(GameStatus.LOBBY, GameStatus.PLAYING).forEach { status ->
            val query = games.orderByChild("status").equalTo(status.key)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // shortcut: les parties abandonnées (plus personne en ligne) restent en base et sont juste masquées ici.
                    byStatus[status] = snapshot.children.mapNotNull(::parseGame)
                        .filter { it.settings.isPublic && it.onlinePlayers.isNotEmpty() }
                    openGames = byStatus.values.flatten()
                        .sortedWith(compareBy<Game> { it.status != GameStatus.LOBBY }.thenBy { it.name })
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = error.toException().toUserMessage()
                }
            }
            query.addValueEventListener(listener)
            openGamesListeners += query to listener
        }
    }

    fun stopWatchingOpenGames() {
        openGamesListeners.forEach { (query, listener) -> query.removeEventListener(listener) }
        openGamesListeners.clear()
    }

    // --- Interne ---

    private fun newPlayer(score: Long): Map<String, Any> = mapOf(
        "name" to myName(),
        "score" to score,
        "online" to true,
        "joinedAt" to ServerValue.TIMESTAMP,
        "wordsFound" to 0
    )

    // Écoute la partie en temps réel et tient la présence du joueur à jour.
    private fun attach(code: String) {
        val uid = myUid ?: return
        detach()
        endingTurn = -1
        choosingTurn = -1
        if (offsetListener == null) {
            offsetListener = database.getReference(".info/serverTimeOffset")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        serverTimeOffset = (snapshot.value as? Number)?.toLong() ?: 0L
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
        val ref = games.child(code)
        gameRef = ref
        gameListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val parsed = parseGame(snapshot)
                if (parsed?.player(uid) == null) {
                    // Partie supprimée ou joueur retiré : retour à l'accueil.
                    detach()
                    game = null
                } else {
                    game = parsed
                }
            }

            override fun onCancelled(error: DatabaseError) {
                errorMessage = error.toException().toUserMessage()
                detach()
                game = null
            }
        })

        // Les traits déjà tracés sont rejoués à l'arrivée : un joueur qui
        // (re)rejoint en cours de tour voit tout le dessin.
        val drawing = root.child("drawings").child(code)
        drawingRef = drawing
        drawingListener = drawing.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                parseStroke(snapshot)?.let { stroke -> strokes = (strokes + stroke).sortedBy { it.id } }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                parseStroke(snapshot)?.let { stroke ->
                    strokes = strokes.map { if (it.id == stroke.id) stroke else it }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                strokes = strokes.filter { it.id != snapshot.key }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        val chat = root.child("chats").child(code)
        chatRef = chat
        chatQuery = chat.limitToLast(CHAT_HISTORY)
        chatListener = chatQuery?.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Tri par clé (chronologique) : notre propre message, affiché localement
                // tout de suite, peut arriver avant l'historique du serveur.
                parseMessage(snapshot)?.let { message ->
                    messages = (messages + message).sortedBy { it.id }.takeLast(CHAT_HISTORY)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        // "online" repasse à false côté serveur si la connexion coupe,
        // puis à true à la reconnexion.
        val online = ref.child("players").child(uid).child("online")
        connectedListener = database.getReference(".info/connected")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) != true) return
                    online.onDisconnect().setValue(false).addOnSuccessListener { online.setValue(true) }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun detach() {
        gameListener?.let { gameRef?.removeEventListener(it) }
        connectedListener?.let { database.getReference(".info/connected").removeEventListener(it) }
        drawingListener?.let { drawingRef?.removeEventListener(it) }
        chatListener?.let { chatQuery?.removeEventListener(it) }
        gameListener = null
        connectedListener = null
        drawingListener = null
        chatListener = null
        gameRef = null
        drawingRef = null
        chatRef = null
        chatQuery = null
        strokes = emptyList()
        messages = emptyList()
    }

    // Un joueur arrivé en cours de partie dessinera aux tours suivants.
    private fun appendToOrder(code: String, uid: String) {
        games.child(code).child("order").runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                val order = data.children.mapNotNull { it.getValue(String::class.java) }
                if (uid !in order) data.child(order.size.toString()).value = uid
                return Transaction.success(data)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
    }

    private fun Exception.toUserMessage(): String = when {
        this is FirebaseNetworkException -> "Pas de connexion réseau, réessaie."
        message?.contains("permission", ignoreCase = true) == true ->
            "Action refusée par la base de données."
        else -> "Oups, réessaie (${localizedMessage ?: "erreur inconnue"})."
    }

    // Sauvegarde les stats de tous les joueurs à la fin d'une partie
    private fun saveGameStats(game: Game) {
        val uid = myUid ?: return
        val myPlayer = game.player(uid) ?: return
        
        // Déterminer si le joueur a gagné (meilleur score)
        val isWinner = game.players.maxByOrNull { it.score }?.uid == uid
        
        val result = GameResult(
            uid = uid,
            difficulty = game.settings.difficulty,
            isWinner = isWinner,
            pointsEarned = myPlayer.score,
            wordsFound = myPlayer.wordsFound,
            opponentsCount = game.players.size - 1
        )
        
        StatsManager.updateStats(result)
    }
}
