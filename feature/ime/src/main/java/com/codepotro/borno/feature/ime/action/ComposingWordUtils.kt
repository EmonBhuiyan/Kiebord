package com.codepotro.borno.feature.ime.action

/**
 * Pure utilities for extracting the full word surrounding the cursor, deciding when
 * next-word prediction is appropriate, and performing cursor-offset-aware edits inside
 * a composing buffer.
 */
object ComposingWordUtils {

    data class WordAroundCursor(
        val word: String,
        val charsBeforeCursor: Int,
        val charsAfterCursor: Int
    ) {
        val length: Int get() = word.length
    }

    private fun isApostrophe(c: Char): Boolean = c == '\'' || c == '’'

    /**
     * Extracts the full contiguous word touching or surrounding the cursor.
     *
     * Returns `null` if the cursor is not touching any word character (for example, when
     * surrounded by whitespace or punctuation, or outside trimmed quote apostrophes).
     */
    fun extractWordAroundCursor(
        textBefore: String,
        textAfter: String,
        isWordChar: (Char) -> Boolean
    ): WordAroundCursor? {
        var beforeStart = textBefore.length
        while (beforeStart > 0 && isWordChar(textBefore[beforeStart - 1])) {
            beforeStart--
        }

        var afterEnd = 0
        while (afterEnd < textAfter.length && isWordChar(textAfter[afterEnd])) {
            afterEnd++
        }

        if (beforeStart == textBefore.length && afterEnd == 0) {
            return null
        }

        val beforePart = textBefore.substring(beforeStart)
        val afterPart = textAfter.substring(0, afterEnd)
        val raw = beforePart + afterPart
        val cursorInRaw = beforePart.length

        var trimStart = 0
        while (trimStart < raw.length && isApostrophe(raw[trimStart])) {
            trimStart++
        }
        var trimEnd = raw.length
        while (trimEnd > trimStart && isApostrophe(raw[trimEnd - 1])) {
            trimEnd--
        }

        if (trimStart >= trimEnd) {
            return null
        }
        if (cursorInRaw < trimStart || cursorInRaw > trimEnd) {
            return null
        }

        return WordAroundCursor(
            word = raw.substring(trimStart, trimEnd),
            charsBeforeCursor = cursorInRaw - trimStart,
            charsAfterCursor = trimEnd - cursorInRaw
        )
    }

    /**
     * Returns `true` only when next-word prediction is appropriate at the cursor:
     * the cursor must not be touching or inside a word, and there must be preceding text.
     */
    fun shouldPredictNextWord(
        textBefore: String,
        textAfter: String,
        isWordChar: (Char) -> Boolean
    ): Boolean {
        if (textBefore.isEmpty()) return false
        return extractWordAroundCursor(textBefore, textAfter, isWordChar) == null
    }

    /**
     * Inserts [text] into [buffer] at [cursorOffset], returning `(updatedBuffer, newCursorOffset)`.
     */
    fun insertAtCursor(buffer: String, cursorOffset: Int, text: String): Pair<String, Int> {
        val safeOffset = cursorOffset.coerceIn(0, buffer.length)
        val updated = buildString(buffer.length + text.length) {
            append(buffer, 0, safeOffset)
            append(text)
            append(buffer, safeOffset, buffer.length)
        }
        return updated to (safeOffset + text.length)
    }

    /**
     * Deletes one character (or surrogate pair, plus any preceding zero-width space `\u200B`)
     * immediately before [cursorOffset] inside [buffer].
     *
     * Returns `null` if [cursorOffset] is `0` (meaning backspace should delete before the word).
     */
    fun deleteBeforeCursor(buffer: String, cursorOffset: Int): Pair<String, Int>? {
        val safeOffset = cursorOffset.coerceIn(0, buffer.length)
        if (safeOffset == 0) return null

        val cp = Character.codePointBefore(buffer, safeOffset)
        val charCount = Character.charCount(cp)
        var newOffset = (safeOffset - charCount).coerceAtLeast(0)
        if (newOffset > 0 && buffer[newOffset - 1] == '\u200B') {
            newOffset--
        }
        val updated = buildString(buffer.length - (safeOffset - newOffset)) {
            append(buffer, 0, newOffset)
            append(buffer, safeOffset, buffer.length)
        }
        return updated to newOffset
    }

    /**
     * Given [textAfterCursor] fetched from `InputConnection.getTextAfterCursor(...)`,
     * strips the remaining characters of the active composing word so that auto-space
     * checks inspect the character immediately following the end of the word.
     */
    fun textAfterComposingWord(
        textAfterCursor: CharSequence?,
        composingLength: Int,
        cursorOffset: Int
    ): CharSequence? {
        val remainingInWord = (composingLength - cursorOffset.coerceIn(0, composingLength)).coerceAtLeast(0)
        if (textAfterCursor == null) return null
        if (textAfterCursor.length <= remainingInWord) return ""
        return textAfterCursor.subSequence(remainingInWord, textAfterCursor.length)
    }
}
