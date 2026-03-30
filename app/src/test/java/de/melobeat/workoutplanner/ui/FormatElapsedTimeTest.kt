package de.melobeat.workoutplanner.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatElapsedTimeTest {

    @Test
    fun `formats zero as 00 00`() {
        assertEquals("00:00", formatElapsedTime(0L))
    }

    @Test
    fun `formats seconds only`() {
        assertEquals("00:45", formatElapsedTime(45_000L))
    }

    @Test
    fun `pads single-digit seconds`() {
        assertEquals("00:05", formatElapsedTime(5_000L))
    }

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("05:30", formatElapsedTime(5 * 60_000L + 30_000L))
    }

    @Test
    fun `pads single-digit minutes`() {
        assertEquals("03:00", formatElapsedTime(3 * 60_000L))
    }

    @Test
    fun `formats exactly 59 minutes 59 seconds without hours`() {
        assertEquals("59:59", formatElapsedTime(59 * 60_000L + 59_000L))
    }

    @Test
    fun `switches to hour format at 60 minutes`() {
        assertEquals("1:00:00", formatElapsedTime(60 * 60_000L))
    }

    @Test
    fun `formats hours minutes and seconds`() {
        assertEquals("1:02:03", formatElapsedTime(1 * 3_600_000L + 2 * 60_000L + 3_000L))
    }

    @Test
    fun `pads minutes and seconds in hour format`() {
        assertEquals("2:05:08", formatElapsedTime(2 * 3_600_000L + 5 * 60_000L + 8_000L))
    }

    @Test
    fun `formats multi-hour workouts`() {
        assertEquals("3:00:00", formatElapsedTime(3 * 3_600_000L))
    }
}
