package com.youtubemusic.core.common

import org.junit.Assert.*
import org.junit.Test

class RoundBigIntegerTest {
    @Test
    fun numberLessThousand() {
        assertEquals("756", longToShortCutSuffix(756))
    }

    @Test
    fun numberZero() {
        assertEquals("0", longToShortCutSuffix(0))
    }

    @Test
    fun numberThousand() {
        assertEquals("1K", longToShortCutSuffix(1000))
    }

    @Test
    fun numberMoreThanThousand() {
        assertEquals("1.2K", longToShortCutSuffix(1200))
    }

    @Test
    fun numberMoreThanThousandDecimal() {
        assertEquals("1.2K", longToShortCutSuffix(1232))
    }

    @Test
    fun numberMoreThanTenThousand() {
        assertEquals("10K", longToShortCutSuffix(10_000))
    }

    @Test
    fun numberMoreThanTenThousandDecimal() {
        assertEquals("10K", longToShortCutSuffix(10_754))
    }

    @Test
    fun numberOneMillion() {
        assertEquals("1M", longToShortCutSuffix(1_000_000))
    }

    @Test
    fun numberOneMillionDecimal() {
        assertEquals("1M", longToShortCutSuffix(1_000_523))
    }

    @Test
    fun numberMoreThanOneMillion() {
        assertEquals("1.3M", longToShortCutSuffix(1_342_523))
    }

    @Test
    fun numberMoreThanBillion() {
        val bigNumber = java.math.BigInteger("6689194517")

        assertEquals("6.6B", bigIntegerToShortCutSuffix(bigNumber))
    }

}