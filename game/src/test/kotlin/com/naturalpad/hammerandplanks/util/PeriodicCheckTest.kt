package com.naturalpad.hammerandplanks.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeriodicCheckTest {
    @Test fun `premier appel verifie et signale la valeur`() {
        val check = PeriodicCheck(1f) { true }
        assertTrue(check.update(0f))
        assertEquals(true, check.value)
    }

    @Test fun `pas de verification avant l intervalle`() {
        var calls = 0
        val check = PeriodicCheck(1f) { calls++; false }
        check.update(0f)
        repeat(9) { check.update(0.1f) }
        assertEquals(1, calls)
        check.update(0.2f)
        assertEquals(2, calls)
    }

    @Test fun `ne signale que les changements`() {
        var connected = false
        val check = PeriodicCheck(1f) { connected }
        check.update(0f)
        assertFalse(check.update(1f))  // toujours débranchée
        connected = true
        assertTrue(check.update(1f))   // manette branchée
        assertFalse(check.update(1f))
        connected = false
        assertTrue(check.update(1f))   // débranchée
    }

    @Test fun `invalidate force la verification suivante`() {
        var calls = 0
        val check = PeriodicCheck(10f) { calls++ }
        check.update(0f)
        check.invalidate()
        check.update(0f)
        assertEquals(2, calls)
    }

    @Test fun `intervalle invalide refuse`() {
        assertFailsWith<IllegalArgumentException> { PeriodicCheck(0f) { 1 } }
    }
}
