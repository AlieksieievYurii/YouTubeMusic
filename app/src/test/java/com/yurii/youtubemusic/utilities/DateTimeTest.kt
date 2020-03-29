package com.yurii.youtubemusic.utilities

import org.junit.Assert.*
import org.junit.Test

class DateTimeTestParsingISO8601TimeFormat {

    @Test
    fun `Test minutes seconds with zero`() {
        val isoFormat = "PT9M3S"
        val minutes = 9
        val seconds = 3

        assertEquals("$minutes:0$seconds", parseDurationToHumanView(isoFormat))
    }

    @Test
    fun `Test hours minutes seconds`() {
        val isoFormat = "PT10H37S"
        val hours = 10
        val minutes = 0
        val seconds = 37

        assertEquals("$hours:$minutes:$seconds", parseDurationToHumanView(isoFormat))
    }

    @Test
    fun `Test seconds`() {
        val isoFormat = "PT30S"
        val minutes = 0
        val seconds = 30

        assertEquals("$minutes:$seconds", parseDurationToHumanView(isoFormat))
    }

    @Test
    fun `Test minutes seconds`() {
        val isoFormat = "PT4M35S"
        val minutes = 4
        val seconds = 35

        assertEquals("$minutes:$seconds", parseDurationToHumanView(isoFormat))
    }
}