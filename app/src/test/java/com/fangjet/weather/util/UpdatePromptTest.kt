package com.fangjet.weather.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePromptTest {
    private val now = 1_000_000_000L

    @Test
    fun `compares versions number by number`() {
        assertTrue(UpdatePrompt.isOlder("1.9", "1.10"))
        assertTrue(UpdatePrompt.isOlder("1.0", "1.0.1"))
        assertTrue(UpdatePrompt.isOlder("1.0-debug", "1.1"))
        assertFalse(UpdatePrompt.isOlder("1.2", "1.2.0"))
        assertFalse(UpdatePrompt.isOlder("2.0", "1.10"))
        assertFalse(UpdatePrompt.isOlder("1.0", null))
        assertFalse(UpdatePrompt.isOlder("1.0", "  "))
    }

    @Test
    fun `unset keys never ask`() {
        assertEquals(UpdatePrompt.NONE, UpdatePrompt.forVersion("1.0", null, null, null, now))
    }

    @Test
    fun `below the minimum supported version the update is required even when the banner was dismissed`() {
        assertEquals(UpdatePrompt.REQUIRED, UpdatePrompt.forVersion("1.0", "1.1", "1.2", now, now))
    }

    @Test
    fun `below the recommended version the banner shows until dismissed for a week`() {
        fun ask(dismissedAt: Long?) = UpdatePrompt.forVersion("1.0", "1.0", "1.1", dismissedAt, now)
        assertEquals(UpdatePrompt.RECOMMENDED, ask(null))
        assertEquals(UpdatePrompt.NONE, ask(now - 60_000))
        assertEquals(UpdatePrompt.RECOMMENDED, ask(now - UpdatePrompt.SNOOZE_MS))
    }

    @Test
    fun `up to date never asks`() {
        assertEquals(UpdatePrompt.NONE, UpdatePrompt.forVersion("1.2", "1.0", "1.2", null, now))
    }
}
