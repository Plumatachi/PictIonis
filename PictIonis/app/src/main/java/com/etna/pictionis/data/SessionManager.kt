package com.etna.pictionis.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

// Session branchée sur Firebase Auth.
// - isLoggedIn est un état Compose : les écrans se rafraîchissent tout seuls.
// - Firebase persiste la session : l'utilisateur reste connecté au redémarrage.
// - Pense à activer le fournisseur "E-mail/Mot de passe" dans la console Firebase
//   (Build > Authentication > Sign-in method).
object SessionManager {

    // Anti brute-force : 3 mots de passe faux de suite -> compte bloqué temporairement.
    private const val MAX_ATTEMPTS = 3
    const val LOCKOUT_MINUTES = 2L
    private const val PREFS = "pictionis_session"
    private const val KEY_LOCK_UNTIL = "lock_until"

    private var appContext: Context? = null
    private var failedAttempts = 0

    // Appelé une fois dans MainActivity.onCreate : nécessaire pour persister
    // le blocage (SharedPreferences) même si l'app redémarre.
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    var isLoggedIn by mutableStateOf(auth.currentUser != null)
        private set

    var accountEmail by mutableStateOf(auth.currentUser?.email)
        private set

    var accountDisplayName by mutableStateOf(auth.currentUser?.displayName)
        private set

    var accountEmailVerified by mutableStateOf(auth.currentUser?.isEmailVerified ?: false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Message de confirmation (ex : "email de réinitialisation envoyé").
    var infoMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Tient isLoggedIn à jour quoi qu'il arrive (ex : token révoqué côté console).
        auth.addAuthStateListener { firebaseAuth ->
            isLoggedIn = firebaseAuth.currentUser != null
            accountEmail = firebaseAuth.currentUser?.email
            accountDisplayName = firebaseAuth.currentUser?.displayName
            accountEmailVerified = firebaseAuth.currentUser?.isEmailVerified ?: false
        }
    }

    fun clearMessages() {
        errorMessage = null
        infoMessage = null
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        clearMessages()
        if (isLocked()) {
            errorMessage = "Trop d'échecs, réessaie dans ${formatRemaining()}."
            return
        }
        errorMessage = emailErrorMessage(email.trim())
            ?: if (password.isEmpty()) "Renseigne ton mot de passe." else null
        if (errorMessage != null) return
        isLoading = true
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                failedAttempts = 0
                onSuccess()
            }
            .addOnFailureListener {
                if (it is FirebaseAuthInvalidCredentialsException) {
                    failedAttempts++
                    errorMessage = if (failedAttempts >= MAX_ATTEMPTS) {
                        lockNow()
                        "3 échecs de suite : patiente $LOCKOUT_MINUTES minutes avant de réessayer."
                    } else {
                        "Email ou mot de passe incorrect. (Essai $failedAttempts/$MAX_ATTEMPTS)"
                    }
                } else {
                    errorMessage = it.toUserMessage()
                }
            }
            .addOnCompleteListener { isLoading = false }
    }

    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        clearMessages()
        errorMessage = emailErrorMessage(email.trim()) ?: passwordErrorMessage(password)
        if (errorMessage != null) return
        isLoading = true
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { errorMessage = it.toUserMessage() }
            .addOnCompleteListener { isLoading = false }
    }

    // Mot de passe oublié : Firebase envoie un email avec un lien de réinitialisation.
    fun sendPasswordReset(email: String) {
        clearMessages()
        val mailError = emailErrorMessage(email.trim())
        if (mailError != null) {
            errorMessage = mailError
            return
        }
        isLoading = true
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                infoMessage = "Email envoyé ! Clique sur le lien reçu pour créer un nouveau mot de passe."
            }
            .addOnFailureListener { errorMessage = it.toUserMessage() }
            .addOnCompleteListener { isLoading = false }
    }

    fun signOut() {
        auth.signOut() // l'AuthStateListener passe isLoggedIn à false
    }

    // --- Profil : pseudo, email, mot de passe ---

    // Recharge le profil Firebase (ex : statut "email vérifié" après clic sur le lien).
    fun refreshAccount() {
        val user = auth.currentUser ?: return
        user.reload().addOnCompleteListener {
            accountEmail = user.email
            accountDisplayName = user.displayName
            accountEmailVerified = user.isEmailVerified
        }
    }

    // Nouveau pseudo (2 à 24 caractères), enregistré comme displayName Firebase.
    fun updateDisplayName(name: String, onSuccess: () -> Unit) {
        clearMessages()
        val clean = name.trim()
        errorMessage = when {
            auth.currentUser == null -> "Reconnecte-toi pour modifier ton profil."
            clean.length < 2 -> "Choisis un pseudo d'au moins 2 caractères."
            clean.length > 24 -> "Ton pseudo ne doit pas dépasser 24 caractères."
            else -> null
        }
        if (errorMessage != null) return
        isLoading = true
        auth.currentUser!!.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(clean).build()
        )
            .addOnSuccessListener {
                accountDisplayName = clean
                infoMessage = "Pseudo mis à jour !"
                onSuccess()
            }
            .addOnFailureListener { errorMessage = it.toUserMessage() }
            .addOnCompleteListener { isLoading = false }
    }

    // Envoie (ou renvoie) le lien de vérification à l'adresse actuelle.
    fun sendEmailVerification() {
        clearMessages()
        val user = auth.currentUser ?: run {
            errorMessage = "Reconnecte-toi pour vérifier ton email."
            return
        }
        isLoading = true
        user.sendEmailVerification()
            .addOnSuccessListener {
                infoMessage = "Lien envoyé ! Clique dessus dans ta boîte mail, puis reviens ici."
            }
            .addOnFailureListener { errorMessage = it.toUserMessage() }
            .addOnCompleteListener { isLoading = false }
    }

    // Changement d'adresse : Firebase envoie un lien à la NOUVELLE adresse,
    // le changement ne prend effet qu'après clic (verifyBeforeUpdateEmail).
    fun changeEmail(newEmail: String) {
        clearMessages()
        val clean = newEmail.trim()
        errorMessage = when {
            auth.currentUser == null -> "Reconnecte-toi pour changer ton email."
            emailErrorMessage(clean) != null -> emailErrorMessage(clean)
            clean.equals(accountEmail, ignoreCase = true) -> "C'est déjà ton adresse actuelle."
            else -> null
        }
        if (errorMessage != null) return
        isLoading = true
        auth.currentUser!!.verifyBeforeUpdateEmail(clean)
            .addOnSuccessListener {
                infoMessage = "Lien envoyé à $clean ! Clique dessus pour confirmer le changement."
            }
            .addOnFailureListener { errorMessage = it.toSensitiveActionMessage() }
            .addOnCompleteListener { isLoading = false }
    }

    // Nouveau mot de passe (mêmes règles qu'à l'inscription).
    fun updatePassword(newPassword: String, onSuccess: () -> Unit) {
        clearMessages()
        errorMessage = when {
            auth.currentUser == null -> "Reconnecte-toi pour changer ton mot de passe."
            else -> passwordErrorMessage(newPassword)
        }
        if (errorMessage != null) return
        isLoading = true
        auth.currentUser!!.updatePassword(newPassword)
            .addOnSuccessListener {
                infoMessage = "Mot de passe mis à jour !"
                onSuccess()
            }
            .addOnFailureListener { errorMessage = it.toSensitiveActionMessage() }
            .addOnCompleteListener { isLoading = false }
    }

    // Email / mot de passe = actions sensibles : Firebase exige parfois une
    // connexion récente avant de les autoriser.
    private fun Exception.toSensitiveActionMessage(): String = when (this) {
        is FirebaseAuthInvalidCredentialsException ->
            "Par sécurité, déconnecte-toi puis reconnecte-toi avant de réessayer."
        else -> toUserMessage()
    }

    // --- Blocage temporaire ---

    private fun prefs() =
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun lockUntil(): Long = prefs()?.getLong(KEY_LOCK_UNTIL, 0L) ?: 0L

    private fun isLocked(): Boolean =
        System.currentTimeMillis() < lockUntil()

    private fun lockNow() {
        failedAttempts = 0
        prefs()?.edit()
            ?.putLong(KEY_LOCK_UNTIL, System.currentTimeMillis() + LOCKOUT_MINUTES * 60_000L)
            ?.apply()
    }

    private fun formatRemaining(): String {
        val total = ((lockUntil() - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        val minutes = total / 60
        val seconds = (total % 60).toString().padStart(2, '0')
        return if (minutes > 0) "$minutes min $seconds s" else "$seconds s"
    }

    // Traduit les erreurs Firebase en messages compréhensibles.
    private fun Exception.toUserMessage(): String = when (this) {
        is FirebaseAuthWeakPasswordException -> "Mot de passe trop faible (6 caractères minimum)."
        is FirebaseAuthUserCollisionException -> "Un compte existe déjà avec cet email, connecte-toi."
        is FirebaseAuthInvalidUserException -> "Aucun compte avec cet email, inscris-toi."
        is FirebaseAuthInvalidCredentialsException -> "Email ou mot de passe incorrect."
        is FirebaseNetworkException -> "Pas de connexion réseau, réessaie."
        is FirebaseTooManyRequestsException -> "Trop de tentatives, patiente quelques minutes."
        else -> "Oups, réessaie (${localizedMessage ?: "erreur inconnue"})."
    }
}
