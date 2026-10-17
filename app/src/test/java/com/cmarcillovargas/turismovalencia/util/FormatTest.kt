package com.cmarcillovargas.turismovalencia.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `los importes se enseñan en euros a la española`() {
        assertEquals("0 €", formatPrice(0))
        assertEquals("18 €", formatPrice(1800))
        assertEquals("1 €", formatPrice(100))
        assertEquals("180 €", formatPrice(18_000))
    }

    @Test
    fun `los centimos llevan coma y siempre dos cifras`() {
        assertEquals("18,50 €", formatPrice(1850))
        assertEquals("0,05 €", formatPrice(5))
        assertEquals("20,05 €", formatPrice(2005))
        assertEquals("0,99 €", formatPrice(99))
    }
}
