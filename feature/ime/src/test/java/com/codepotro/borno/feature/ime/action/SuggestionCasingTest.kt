package com.codepotro.borno.feature.ime.action

import com.codepotro.borno.common.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionCasingTest {

    @Test
    fun testFirstWordSingleCapitalLetterDoesNotMakeAllUppercase() {
        // When typing the first letter of a sentence with Shift (e.g. 'V')
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "very",
            typedPrefix = "V",
            shiftState = ShiftState.UNSHIFTED // Shift reverted after first char tap
        )
        // Must be Title Case "Very", NEVER all uppercase "VERY"
        assertEquals("Very", formatted)

        val viaFormatted = SuggestionCasingUtils.formatCandidate("via", "V", ShiftState.UNSHIFTED)
        assertEquals("Via", viaFormatted)

        val theFormatted = SuggestionCasingUtils.formatCandidate("the", "T", ShiftState.UNSHIFTED)
        assertEquals("The", theFormatted)
    }

    @Test
    fun testFirstWordWithOneShotShiftState() {
        // When typing with Shift ONE_SHOT active
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "very",
            typedPrefix = "V",
            shiftState = ShiftState.ONE_SHOT
        )
        assertEquals("Very", formatted)
    }

    @Test
    fun testSubsequentCharactersMaintainTitleCase() {
        // Typing "Ve", "Ver", "Very" maintains Title Case
        assertEquals("Very", SuggestionCasingUtils.formatCandidate("very", "Ve", ShiftState.UNSHIFTED))
        assertEquals("Verification", SuggestionCasingUtils.formatCandidate("verification", "Ver", ShiftState.UNSHIFTED))
    }

    @Test
    fun testCapsLockProducesFullUppercase() {
        // When user explicitly locked Caps Lock
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "very",
            typedPrefix = "V",
            shiftState = ShiftState.CAPS_LOCK
        )
        assertEquals("VERY", formatted)
    }

    @Test
    fun testAcronymTypedWithMultipleUppercaseProducesFullUppercase() {
        // User deliberately typed multiple uppercase letters (e.g. "US", "NASA")
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "usa",
            typedPrefix = "US",
            shiftState = ShiftState.UNSHIFTED
        )
        assertEquals("USA", formatted)
    }

    @Test
    fun testLowercasePrefixProducesLowercaseSuggestions() {
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "very",
            typedPrefix = "v",
            shiftState = ShiftState.UNSHIFTED
        )
        assertEquals("very", formatted)
    }

    @Test
    fun testNextWordPredictionAtSentenceStartWithOneShotShift() {
        // No prefix typed yet, at sentence start with ONE_SHOT
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "the",
            typedPrefix = "",
            shiftState = ShiftState.ONE_SHOT
        )
        assertEquals("The", formatted)
    }

    @Test
    fun testNextWordPredictionInsideSentenceUnshifted() {
        // No prefix typed yet, inside sentence
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "the",
            typedPrefix = "",
            shiftState = ShiftState.UNSHIFTED
        )
        assertEquals("the", formatted)
    }

    @Test
    fun testProperNounsPreservedInDictionaryAndPredictions() {
      
        val ahsan = SuggestionCasingUtils.formatCandidate(
            word = "Ahsan",
            typedPrefix = "",
            shiftState = ShiftState.UNSHIFTED
        )
        assertEquals("Ahsan", ahsan)

        val saad = SuggestionCasingUtils.formatCandidate(
            word = "Saad",
            typedPrefix = "",
            shiftState = ShiftState.UNSHIFTED
        )
        assertEquals("Saad", saad)

        val jayed = SuggestionCasingUtils.formatCandidate(
            word = "Jayed",
            typedPrefix = "j",
            shiftState = ShiftState.UNSHIFTED
        )
        assertEquals("Jayed", jayed)
    }

    @Test
    fun testBengaliTextIsUnaffected() {
        val formatted = SuggestionCasingUtils.formatCandidate(
            word = "আমি",
            typedPrefix = "আ",
            shiftState = ShiftState.ONE_SHOT
        )
        assertEquals("আমি", formatted)
    }

    @Test
    fun testNextWordCandidateInitialAsIsWhenUnshifted() {
        assertEquals(
            "the",
            SuggestionCasingUtils.formatNextWordCandidate("the", ShiftState.UNSHIFTED, forceLowercaseWhenUnshifted = false)
        )
        assertEquals(
            "Monday",
            SuggestionCasingUtils.formatNextWordCandidate("Monday", ShiftState.UNSHIFTED, forceLowercaseWhenUnshifted = false)
        )
    }

    @Test
    fun testNextWordCandidateFollowsManualShiftCycle() {
        // Shift pressed -> uppercase first letter only
        assertEquals(
            "Hello",
            SuggestionCasingUtils.formatNextWordCandidate("hello", ShiftState.ONE_SHOT, forceLowercaseWhenUnshifted = true)
        )
        assertEquals(
            "Hello",
            SuggestionCasingUtils.formatNextWordCandidate("HELLO", ShiftState.ONE_SHOT, forceLowercaseWhenUnshifted = true)
        )

        // Shift locked -> uppercase whole word
        assertEquals(
            "HELLO",
            SuggestionCasingUtils.formatNextWordCandidate("hello", ShiftState.CAPS_LOCK, forceLowercaseWhenUnshifted = true)
        )
        assertEquals(
            "MONDAY",
            SuggestionCasingUtils.formatNextWordCandidate("Monday", ShiftState.CAPS_LOCK, forceLowercaseWhenUnshifted = true)
        )

        // Shift off -> all lowercase
        assertEquals(
            "hello",
            SuggestionCasingUtils.formatNextWordCandidate("Hello", ShiftState.UNSHIFTED, forceLowercaseWhenUnshifted = true)
        )
        assertEquals(
            "monday",
            SuggestionCasingUtils.formatNextWordCandidate("Monday", ShiftState.UNSHIFTED, forceLowercaseWhenUnshifted = true)
        )
    }

    @Test
    fun testNextWordCandidateSupportsExtendedCasedAndPreservesUncasedScripts() {
        // Extended Latin (e.g. French)
        assertEquals(
            "École",
            SuggestionCasingUtils.formatNextWordCandidate("école", ShiftState.ONE_SHOT, forceLowercaseWhenUnshifted = true)
        )
        assertEquals(
            "ÉCOLE",
            SuggestionCasingUtils.formatNextWordCandidate("école", ShiftState.CAPS_LOCK, forceLowercaseWhenUnshifted = true)
        )
        assertEquals(
            "école",
            SuggestionCasingUtils.formatNextWordCandidate("École", ShiftState.UNSHIFTED, forceLowercaseWhenUnshifted = true)
        )

        // Uncased script (Bengali) preserved in all shift states
        assertEquals(
            "বাংলা",
            SuggestionCasingUtils.formatNextWordCandidate("বাংলা", ShiftState.ONE_SHOT, forceLowercaseWhenUnshifted = true)
        )
        assertEquals(
            "বাংলা",
            SuggestionCasingUtils.formatNextWordCandidate("বাংলা", ShiftState.CAPS_LOCK, forceLowercaseWhenUnshifted = true)
        )
        assertEquals(
            "বাংলা",
            SuggestionCasingUtils.formatNextWordCandidate("বাংলা", ShiftState.UNSHIFTED, forceLowercaseWhenUnshifted = true)
        )
    }
}

