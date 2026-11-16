package com.yogeshwar.budgettracker.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class MerchantNormalizerTest {
    private fun key(raw: String) = MerchantNormalizer.normalize(raw).key

    @Test
    fun `strips processor prefixes`() {
        assertEquals("remi flower & coff", key("SQ *REMI FLOWER & COFF"))
        assertEquals("l&b spumoni garden", key("TST*L&B SPUMONI GARDEN"))
    }

    @Test
    fun `drops trailing store numbers`() {
        assertEquals("walmart", key("Walmart2000153590447"))
        assertEquals("wal-mart", key("WAL-MART #1234"))
    }

    @Test
    fun `leaves ordinary names alone and ignores case and spacing`() {
        assertEquals("h&m", key("H&M"))
        assertEquals("path tapp paygo cp", key("PATH TAPP PAYGO CP"))
        assertEquals("heytea grand st", key("  HeyTea   Grand St "))
    }

    @Test
    fun `short trailing numbers are part of the name`() {
        assertEquals("studio 54", key("Studio 54"))
    }

    @Test
    fun `same merchant from different notifications gets the same key`() {
        assertEquals(key("SQ *REMI FLOWER & COFF"), key("REMI FLOWER & COFF"))
    }

    @Test
    fun `display text is cleaned but keeps original casing`() {
        assertEquals("REMI FLOWER & COFF", MerchantNormalizer.normalize("SQ *REMI FLOWER & COFF").display)
    }
}
