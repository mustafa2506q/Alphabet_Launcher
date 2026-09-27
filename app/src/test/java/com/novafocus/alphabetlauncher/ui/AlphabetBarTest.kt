package com.novafocus.alphabetlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetBarTest {

    @Test
    fun `touched letter gets the full pull`() {
        val pull = pullForDistance(rowsAway = 0f, maxPullPx = 100f)
        assertEquals(100f, pull, 0.01f)
    }

    @Test
    fun `pull fades out the further a letter is from the touch`() {
        val nearby = pullForDistance(rowsAway = 1f, maxPullPx = 100f)
        val faraway = pullForDistance(rowsAway = 5f, maxPullPx = 100f)
        assertTrue("closer letters should be pulled more", nearby > faraway)
    }

    @Test
    fun `pull never goes negative or above the max`() {
        val pull = pullForDistance(rowsAway = 0f, maxPullPx = 100f)
        assertTrue(pull <= 100f)
        assertTrue(pull >= 0f)
    }

    @Test
    fun `touch at the very top maps to A`() {
        val letter = letterForTouchY(touchY = 0f, barHeightPx = 260f)
        assertEquals('A', letter)
    }

    @Test
    fun `touch at the very bottom maps to Z`() {
        val letter = letterForTouchY(touchY = 259f, barHeightPx = 260f)
        assertEquals('Z', letter)
    }

    @Test
    fun `touch in the middle maps to a middle letter`() {
        val letter = letterForTouchY(touchY = 130f, barHeightPx = 260f)
        assertEquals('N', letter) // row 13 of 26, zero-indexed
    }

    @Test
    fun `zero height bar returns no letter instead of crashing`() {
        val letter = letterForTouchY(touchY = 50f, barHeightPx = 0f)
        assertNull(letter)
    }

    @Test
    fun `touch below the bar clamps to Z instead of crashing`() {
        val letter = letterForTouchY(touchY = 9999f, barHeightPx = 260f)
        assertEquals('Z', letter)
    }
}