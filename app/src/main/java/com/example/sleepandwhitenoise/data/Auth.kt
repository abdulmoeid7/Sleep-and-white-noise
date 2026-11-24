package com.example.sleepandwhitenoise.data

import kotlinx.coroutines.delay

/**
 * Accounts are optional (the app is fully usable as a guest). The design handoff states that
 * authentication is an integration still to be chosen, so this is a local placeholder:
 * it validates input and records the email on-device, but nothing is sent to a server and no
 * password is stored. Replace the bodies with a real provider when one is selected.
 */
class AuthRepository(private val settings: SettingsRepository) {

    sealed interface Result {
        data object Ok : Result
        data class Error(val message: String) : Result
    }

    fun validateEmail(email: String): String? =
        if (EMAIL.matches(email.trim())) null else "Enter a valid email address"

    fun validatePassword(pw: String): String? = when {
        pw.length < 8 -> "Use at least 8 characters"
        pw.none { it.isDigit() } || pw.none { it.isLetter() } -> "Use both letters and numbers"
        else -> null
    }

    suspend fun signIn(email: String, password: String): Result {
        validateEmail(email)?.let { return Result.Error(it) }
        if (password.isEmpty()) return Result.Error("Enter your password")
        delay(600)
        settings.update { it.copy(accountEmail = email.trim()) }
        return Result.Ok
    }

    suspend fun signUp(name: String, email: String, password: String): Result {
        if (name.isBlank()) return Result.Error("Enter your name")
        validateEmail(email)?.let { return Result.Error(it) }
        validatePassword(password)?.let { return Result.Error(it) }
        delay(600)
        settings.update { it.copy(accountEmail = email.trim(), displayName = name.trim()) }
        return Result.Ok
    }

    suspend fun requestReset(email: String): Result {
        validateEmail(email)?.let { return Result.Error(it) }
        delay(600)
        return Result.Ok
    }

    suspend fun verifyCode(code: String): Result {
        if (code.length != 6 || code.any { !it.isDigit() }) return Result.Error("Enter the 6-digit code")
        delay(400)
        return Result.Ok
    }

    suspend fun setNewPassword(pw: String, confirm: String): Result {
        validatePassword(pw)?.let { return Result.Error(it) }
        if (pw != confirm) return Result.Error("Passwords don't match")
        delay(500)
        return Result.Ok
    }

    fun signOut() = settings.update { it.copy(accountEmail = null) }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
