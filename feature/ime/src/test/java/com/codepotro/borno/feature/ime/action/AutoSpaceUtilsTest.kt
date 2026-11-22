package com.codepotro.borno.feature.ime.action

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSpaceUtilsTest {

    @Test
    fun `inserts space at tail of text field when enabled`() {
        assertTrue(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = false,
                candidateText = "receive",
                textAfterCursor = ""
            )
        )
    }

    @Test
    fun `inserts space when InputConnection returns null for textAfterCursor`() {
        assertTrue(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = false,
                candidateText = "receive",
                textAfterCursor = null
            )
        )
    }

    @Test
    fun `skips space when space already follows cursor`() {
        assertFalse(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = false,
                candidateText = "receive",
                textAfterCursor = " "
            )
        )
    }

    @Test
    fun `skips space when punctuation follows cursor`() {
        val punctuationSamples = listOf(".", ",", "!", "?", ":", ";", ")", "]", "।", "\"")
        for (punc in punctuationSamples) {
            assertFalse(
                "Expected no auto-space before '$punc'",
                AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                    isPreferenceEnabled = true,
                    layoutNoAutoSpace = false,
                    candidateText = "receive",
                    textAfterCursor = punc
                )
            )
        }
    }

    @Test
    fun `skips space when another word or letter follows cursor`() {
        assertFalse(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = false,
                candidateText = "receive",
                textAfterCursor = "m"
            )
        )
    }

    @Test
    fun `skips space when newline follows cursor`() {
        assertFalse(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = false,
                candidateText = "receive",
                textAfterCursor = "\n"
            )
        )
    }

    @Test
    fun `skips space when preference is disabled`() {
        assertFalse(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = false,
                layoutNoAutoSpace = false,
                candidateText = "receive",
                textAfterCursor = ""
            )
        )
    }

    @Test
    fun `skips space when layout has NoAutoSpace flag`() {
        assertFalse(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = true,
                candidateText = "receive",
                textAfterCursor = ""
            )
        )
    }

    @Test
    fun `skips space when candidate text is empty or ends with whitespace`() {
        assertFalse(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = false,
                candidateText = "",
                textAfterCursor = ""
            )
        )
        assertFalse(
            AutoSpaceUtils.shouldAutoSpaceAfterSuggestion(
                isPreferenceEnabled = true,
                layoutNoAutoSpace = false,
                candidateText = "receive ",
                textAfterCursor = ""
            )
        )
    }
}
