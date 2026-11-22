package com.snehonir.lipirx.update

import com.snehonir.lipirx.domain.update.SemVer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemVerTest {

    @Test
    fun testSemVerParsing() {
        val v1 = SemVer.parse("v0.5.1")
        assertEquals(SemVer(0, 5, 1), v1)

        val v2 = SemVer.parse("0.4.4-beta+100")
        assertEquals(SemVer(0, 4, 4), v2)

        val v3 = SemVer.parse("v1.0.0")
        assertEquals(SemVer(1, 0, 0), v3)
    }

    @Test
    fun testSemVerComparison() {
        val vOld = SemVer.parse("v0.4.4")
        val vNew = SemVer.parse("v0.5.1")

        assertTrue(vNew > vOld, "v0.5.1 should be greater than v0.4.4")
        assertTrue(vOld < vNew, "v0.4.4 should be less than v0.5.1")
        assertEquals(0, SemVer.parse("0.5.1").compareTo(SemVer.parse("v0.5.1")))
    }
}
