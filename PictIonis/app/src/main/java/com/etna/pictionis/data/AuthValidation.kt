package com.etna.pictionis.data

import android.util.Patterns

// Règles de validation partagées entre SessionManager (refus au submit)
// et AuthScreen (checklist affichée en direct sous le champ mot de passe).

fun emailErrorMessage(email: String): String? = when {
    email.isBlank() -> "Renseigne ton email."
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Cet email n'a pas un format valide."
    else -> null
}

data class PasswordRule(val label: String, val passed: Boolean)

fun passwordRules(password: String): List<PasswordRule> = listOf(
    PasswordRule("8 caractères minimum", password.length >= 8),
    PasswordRule("1 majuscule", password.any { it.isUpperCase() }),
    PasswordRule("1 minuscule", password.any { it.isLowerCase() }),
    PasswordRule("1 chiffre", password.any { it.isDigit() }),
    PasswordRule("1 caractère spécial", password.any { !it.isLetterOrDigit() })
)

fun passwordErrorMessage(password: String): String? {
    if (password.isEmpty()) return "Renseigne ton mot de passe."
    val missing = passwordRules(password).filterNot { it.passed }.map { it.label }
    return if (missing.isEmpty()) null
    else "Mot de passe incomplet, il manque : ${missing.joinToString(", ")}."
}

// Niveau de robustesse 0 (Faible) à 3 (Excellent), dérivé des règles.
// Sert à la jauge affichée sous le champ mot de passe.
data class PasswordStrength(val level: Int, val label: String)

fun passwordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength(0, "")
    return when (val passed = passwordRules(password).count { it.passed }) {
        0, 1 -> PasswordStrength(0, "Faible")
        2, 3 -> PasswordStrength(1, "Moyen")
        4 -> PasswordStrength(2, "Fort")
        else -> if (password.length >= 12) PasswordStrength(3, "Excellent")
        else PasswordStrength(2, "Fort")
    }
}
