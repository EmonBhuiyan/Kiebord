package com.codepotro.borno.feature.ime.action

import com.codepotro.borno.common.model.ShiftState

/**
 * Formats suggestion candidate text casing based on typed prefix and shift state.
 * Follows Borno Keyboard's approach:
 * - If Caps Lock is active, or user typed an acronym (>1 uppercase letter), format suggestions in ALL UPPERCASE (e.g. "USA").
 * - If only the first character is capitalized (e.g. at sentence start or Shift ONE_SHOT), format suggestions in Title Case ("Very", "Via", "The"), NOT all uppercase.
 * - Otherwise preserve the word's casing as stored in the dictionary / user dictionary (e.g. "Codepotro", "Borno", "very").
 * - Non-Latin text (e.g. Bengali) is preserved untouched.
 */
object SuggestionCasingUtils {

    fun formatCandidate(
        word: String,
        typedPrefix: String,
        shiftState: ShiftState = ShiftState.UNSHIFTED
    ): String {
        if (word.isEmpty() || !containsLatinText(word)) return word

        val isAllUpper = when {
            shiftState == ShiftState.CAPS_LOCK -> true
            typedPrefix.length > 1 && typedPrefix.all { it.isUpperCase() } -> true
            else -> false
        }
        val isFirstUpper = when {
            isAllUpper -> false
            typedPrefix.isNotEmpty() -> typedPrefix[0].isUpperCase()
            shiftState == ShiftState.ONE_SHOT -> true
            else -> false
        }

        return when {
            isAllUpper -> word.uppercase()
            isFirstUpper -> word.replaceFirstChar { it.uppercase() }
            else -> word
        }
    }

    /**
     * Formats next-word prediction candidates in supported cased scripts based on the active [shiftState].
     * - Initial predictions ([forceLowercaseWhenUnshifted] == false): preserves dictionary casing when UNSHIFTED,
     *   applies Title Case when ONE_SHOT, and ALL UPPERCASE when CAPS_LOCK.
     * - After manual Shift cycling ([forceLowercaseWhenUnshifted] == true): UNSHIFTED formats to all lowercase,
     *   ONE_SHOT uppercases only the first letter, and CAPS_LOCK uppercases the entire word.
     * - Uncased scripts (e.g. Bengali, Arabic) are preserved untouched.
     */
    fun formatNextWordCandidate(
        word: String,
        shiftState: ShiftState,
        forceLowercaseWhenUnshifted: Boolean = false
    ): String {
        if (word.isEmpty() || !containsCasedText(word)) return word

        return when (shiftState) {
            ShiftState.CAPS_LOCK -> word.uppercase()
            ShiftState.ONE_SHOT -> word.lowercase().replaceFirstChar { it.uppercase() }
            ShiftState.UNSHIFTED -> if (forceLowercaseWhenUnshifted) word.lowercase() else word
        }
    }

    private fun containsLatinText(text: String): Boolean =
        text.any { (it in 'a'..'z') || (it in 'A'..'Z') }

    private fun containsCasedText(text: String): Boolean =
        text.any { it.isLetter() && it.lowercaseChar() != it.uppercaseChar() }
}

