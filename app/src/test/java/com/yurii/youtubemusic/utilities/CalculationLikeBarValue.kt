package com.yurii.youtubemusic.utilities

import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

class CalculationLikeBarValue {

    @Test
    fun equallyLikesAndDisLikes() {
        assertEquals(50, calculateLikeBarValue(BigInteger("300"), BigInteger("300")))
    }

    @Test
    fun zeroLikesAndDisLikes() {
        assertEquals(50, calculateLikeBarValue(BigInteger("0"), BigInteger("0")))
    }

    @Test
    fun likesMoreThanDisLikes() {
        assertEquals(85, calculateLikeBarValue(BigInteger("23"), BigInteger("4")))
    }

    @Test
    fun disLikesMoreThanLikes() {
        assertEquals(14, calculateLikeBarValue(BigInteger("4"), BigInteger("23")))
    }

    @Test
    fun onlyLikes() {
        assertEquals(100, calculateLikeBarValue(BigInteger("43"), BigInteger("0")))
    }

    @Test
    fun onlyDisLikes() {
        assertEquals(0, calculateLikeBarValue(BigInteger("0"), BigInteger("23")))
    }
}