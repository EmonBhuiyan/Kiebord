package com.codepotro.borno.feature.ime.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposingWordUtilsTest {

    private val isEnWordChar: (Char) -> Boolean = { it.isLetter() || it == '\'' || it == '’' }

    @Test
    fun extractWord_returnsSameWholeWordAtEveryCursorOffset() {
        val fullWord = "Dinosaor"
        for (offset in 0..fullWord.length) {
            val before = "I saw " + fullWord.substring(0, offset)
            val after = fullWord.substring(offset) + " today"
            val result = ComposingWordUtils.extractWordAroundCursor(before, after, isEnWordChar)
            assertEquals(
                ComposingWordUtils.WordAroundCursor(
                    word = fullWord,
                    charsBeforeCursor = offset,
                    charsAfterCursor = fullWord.length - offset
                ),
                result
            )
        }
    }

    @Test
    fun extractWord_handlesEndOfWordAfterBackspaceOverSpace() {
        val result = ComposingWordUtils.extractWordAroundCursor("hello dont", "", isEnWordChar)
        assertEquals(ComposingWordUtils.WordAroundCursor("dont", 4, 0), result)
    }

    @Test
    fun extractWord_preservesInternalApostrophes() {
        val result = ComposingWordUtils.extractWordAroundCursor("I don'", "t know", isEnWordChar)
        assertEquals(ComposingWordUtils.WordAroundCursor("don't", 4, 1), result)

        val resultCurly = ComposingWordUtils.extractWordAroundCursor("I’", "ve", isEnWordChar)
        assertEquals(ComposingWordUtils.WordAroundCursor("I’ve", 2, 2), resultCurly)
    }

    @Test
    fun extractWord_excludesLeadingAndTrailingQuotes() {
        assertNull(ComposingWordUtils.extractWordAroundCursor("say ", "'hello'", isEnWordChar))
        assertNull(ComposingWordUtils.extractWordAroundCursor("say 'hello'", " now", isEnWordChar))

        val inside = ComposingWordUtils.extractWordAroundCursor("say 'hel", "lo'", isEnWordChar)
        assertEquals(ComposingWordUtils.WordAroundCursor("hello", 3, 2), inside)
    }

    @Test
    fun extractWord_returnsNullWhenSurroundedBySpacesOrPunctuation() {
        assertNull(ComposingWordUtils.extractWordAroundCursor("hello ", " world", isEnWordChar))
        assertNull(ComposingWordUtils.extractWordAroundCursor("hello. ", "", isEnWordChar))
        assertNull(ComposingWordUtils.extractWordAroundCursor("", "", isEnWordChar))
        assertNull(ComposingWordUtils.extractWordAroundCursor("''", "", isEnWordChar))
    }

    @Test
    fun shouldPredictNextWord_isFalseWheneverCursorTouchesWord() {
        assertFalse(ComposingWordUtils.shouldPredictNextWord("Can", "t", isEnWordChar))
        assertFalse(ComposingWordUtils.shouldPredictNextWord("Cant", "", isEnWordChar))
        assertFalse(ComposingWordUtils.shouldPredictNextWord("Do", "nt", isEnWordChar))
        assertFalse(ComposingWordUtils.shouldPredictNextWord("hello ", "world", isEnWordChar))
        assertFalse(ComposingWordUtils.shouldPredictNextWord("", "", isEnWordChar))
    }

    @Test
    fun shouldPredictNextWord_isTrueAfterTrailingSpace() {
        assertTrue(ComposingWordUtils.shouldPredictNextWord("can ", "", isEnWordChar))
        assertTrue(ComposingWordUtils.shouldPredictNextWord("thank ", " ", isEnWordChar))
    }

    @Test
    fun insertAtCursor_insertsMidWordAndAdvancesOffset() {
        assertEquals("Dinousaor" to 5, ComposingWordUtils.insertAtCursor("Dinosaor", 4, "u"))
        assertEquals("xDinosaor" to 1, ComposingWordUtils.insertAtCursor("Dinosaor", 0, "x"))
        assertEquals("Dinosaors" to 9, ComposingWordUtils.insertAtCursor("Dinosaor", 8, "s"))
    }

    @Test
    fun deleteBeforeCursor_deletesMidWordAndReturnsNullAtStart() {
        assertEquals("Dinsaor" to 3, ComposingWordUtils.deleteBeforeCursor("Dinosaor", 4))
        assertEquals("Dinosaor".dropLast(1) to 7, ComposingWordUtils.deleteBeforeCursor("Dinosaor", 8))
        assertNull(ComposingWordUtils.deleteBeforeCursor("Dinosaor", 0))
    }

    @Test
    fun textAfterComposingWord_stripsRemainingWordTail() {
        // Cursor at Dino|saor at end of field
        assertEquals("", ComposingWordUtils.textAfterComposingWord("saor", 8, 4))
        // Cursor at Dino|saor followed by space
        assertEquals(" ", ComposingWordUtils.textAfterComposingWord("saor ", 8, 4))
        // Cursor at end of word Dinosaor| followed by period
        assertEquals(".", ComposingWordUtils.textAfterComposingWord(".", 8, 8))
    }
}
