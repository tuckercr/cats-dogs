package com.fangjet.weather.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationLocationTest {
    private val paris = SavedLocation("Paris, FR", 48.9, 2.4)
    private val tokyo = SavedLocation("Tokyo, JP", 35.7, 139.7)
    private val here = SavedLocation("My Location", 40.0, -83.0, isCurrentLocation = true)

    @Test
    fun `My Location wins wherever it is in the list`() {
        assertEquals(here, listOf(paris, tokyo, here).notificationLocation())
    }

    @Test
    fun `without My Location it's the first city`() {
        assertEquals(paris, listOf(paris, tokyo).notificationLocation())
    }

    @Test
    fun `distanceMeters measures along the earth`() {
        // A tenth of a degree of latitude is about 11.1 km anywhere.
        assertEquals(11_119.0, distanceMeters(40.0, -83.0, 40.1, -83.0), 10.0)
        assertEquals(0.0, distanceMeters(40.0, -83.0, 40.0, -83.0), 0.0)
    }

    @Test
    fun `no cities, no notification city`() {
        assertNull(emptyList<SavedLocation>().notificationLocation())
        assertNull(emptyList<SavedLocation>().notificationIndex())
    }

    @Test
    fun `the bell in Manage Locations marks the same row`() {
        assertEquals(2, listOf(paris, tokyo, here).notificationIndex())
        assertEquals(0, listOf(tokyo, paris).notificationIndex())
    }
}
