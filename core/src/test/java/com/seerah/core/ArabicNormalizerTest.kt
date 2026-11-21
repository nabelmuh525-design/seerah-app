package com.seerah.core

import com.seerah.core.text.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNormalizerTest {
    @Test
    fun normalizesHamzaAlefMaqsuraAndTashkeel() {
        val input = "غَزْوَةُ أُحُدٍ وَإِسْرَاءٌ إِلَى الأَقْصَى"
        val expected = "غزوه احد واسراء الي الاقصي"
        assertEquals(expected, ArabicNormalizer.normalize(input))
    }

    @Test
    fun buildsPrefixFtsQuery() {
        assertEquals("غزوه* بدر*", ArabicNormalizer.toFtsQuery("غزوة بدر"))
    }
}
