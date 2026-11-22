package com.codepotro.borno.feature.ime.action

/**
 * Pure helper utilities for deciding whether Auto-Space after Suggestion should
 * insert a trailing space based on user preference, layout flags, candidate text,
 * and cursor position in the text field.
 */
object AutoSpaceUtils {

    /**
     * Returns true only when Auto-Space after Suggestion should append a trailing space:
     * - User preference is enabled for the active language
     * - The active layout does not specify the `NoAutoSpace` flag
     * - [candidateText] is non-empty and does not already end with whitespace
     * - The cursor is at the tail of the text field ([textAfterCursor] is null or empty),
     *   ensuring no space is inserted before an existing space, punctuation, newline, or word.
     */
    fun shouldAutoSpaceAfterSuggestion(
        isPreferenceEnabled: Boolean,
        layoutNoAutoSpace: Boolean,
        candidateText: String,
        textAfterCursor: CharSequence?
    ): Boolean {
        if (!isPreferenceEnabled) return false
        if (layoutNoAutoSpace) return false
        if (candidateText.isEmpty() || candidateText.last().isWhitespace()) return false
        if (!textAfterCursor.isNullOrEmpty()) return false
        return true
    }
}
