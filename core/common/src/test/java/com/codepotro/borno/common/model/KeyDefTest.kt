package com.codepotro.borno.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyDefTest {

    @Test
    fun flickUp_filtersOutActionTokens() {
        val key = KeyDef(
            label = "ঁ",
            shiftedLabel = "ঃ",
            flickUp = "ঃ",
            shiftedFlickUp = "",
            moreKeys = listOf("ঃ", "ACTION_SELECT_ALL"),
            shiftedMoreKeys = listOf("ACTION_SELECT_ALL")
        )

        assertEquals("ঃ", key.flickUp(ShiftState.UNSHIFTED))
        assertNull(key.flickUp(ShiftState.ONE_SHOT))
        assertNull(key.flickUp(ShiftState.CAPS_LOCK))
    }

    @Test
    fun flickUp_returnsShiftedMoreKeysFirstNonActionWhenShiftedFlickUpNull() {
        val keyWithActionFirst = KeyDef(
            label = "a",
            shiftedLabel = "A",
            shiftedMoreKeys = listOf("ACTION_SELECT_ALL", "Ä")
        )

        assertEquals("Ä", keyWithActionFirst.flickUp(ShiftState.ONE_SHOT))

        val keyWithOnlyAction = KeyDef(
            label = "b",
            shiftedLabel = "B",
            shiftedMoreKeys = listOf("ACTION_SELECT_ALL")
        )

        assertNull(keyWithOnlyAction.flickUp(ShiftState.ONE_SHOT))
    }

    @Test
    fun flickUp_returnsExplicitShiftedFlickUp() {
        val key = KeyDef(
            label = "ক",
            shiftedLabel = "খ",
            flickUp = "খ",
            shiftedFlickUp = "ক"
        )

        assertEquals("খ", key.flickUp(ShiftState.UNSHIFTED))
        assertEquals("ক", key.flickUp(ShiftState.ONE_SHOT))
        assertEquals("ক", key.flickUp(ShiftState.CAPS_LOCK))
    }

    @Test
    fun hasShiftKey_detectsPresenceOfShiftKey() {
        val layoutWithShift = KeyboardLayoutDef(
            id = "with_shift",
            rows = listOf(
                KeyRow(listOf(KeyDef(label = "a"))),
                KeyRow(listOf(KeyDef(type = KeyType.SHIFT), KeyDef(label = "z")))
            )
        )
        val layoutWithoutShift = KeyboardLayoutDef(
            id = "without_shift",
            rows = listOf(
                KeyRow(listOf(KeyDef(label = "q"))),
                KeyRow(listOf(KeyDef(label = "a"), KeyDef(label = "z")))
            )
        )

        org.junit.Assert.assertTrue(layoutWithShift.hasShiftKey())
        org.junit.Assert.assertFalse(layoutWithoutShift.hasShiftKey())
    }

    @Test
    fun moreKeys_filtersPrimaryShortcutsWhenDisabled() {
        val zKey = KeyDef(
            label = "z",
            shiftedLabel = "Z",
            moreKeys = listOf("ACTION_SELECT_ALL", "*")
        )
        assertEquals(listOf("ACTION_SELECT_ALL", "*"), zKey.moreKeys(ShiftState.UNSHIFTED, includePrimaryShortcuts = true))
        assertEquals(listOf("*"), zKey.moreKeys(ShiftState.UNSHIFTED, includePrimaryShortcuts = false))

        val cKey = KeyDef(
            label = "c",
            shiftedLabel = "C",
            moreKeys = listOf("ACTION_COPY", "'", "ç"),
            shiftedMoreKeys = listOf("action:cut", "ACTION_PASTE", "ACTION_UNDO", "ACTION_REDO")
        )
        assertEquals(listOf("'", "ç"), cKey.moreKeys(ShiftState.UNSHIFTED, includePrimaryShortcuts = false))
        assertEquals(emptyList<String>(), cKey.moreKeys(ShiftState.ONE_SHOT, includePrimaryShortcuts = false))

        val commaKey = KeyDef(
            label = ",",
            moreKeys = listOf("ACTION_SETTINGS", "ACTION_ONE_HANDED")
        )
        assertEquals(
            listOf("ACTION_SETTINGS", "ACTION_ONE_HANDED"),
            commaKey.moreKeys(ShiftState.UNSHIFTED, includePrimaryShortcuts = false)
        )

        val enterKey = KeyDef(
            type = KeyType.ACTION,
            icon = "enter",
            moreKeys = listOf("ACTION_EMOJI")
        )
        assertEquals(
            listOf("ACTION_EMOJI", "ACTION_NEW_LINE"),
            enterKey.moreKeys(ShiftState.UNSHIFTED, includePrimaryShortcuts = false)
        )
    }
}

