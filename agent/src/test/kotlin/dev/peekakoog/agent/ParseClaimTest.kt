package dev.peekakoog.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseClaimTest {
    @Test
    fun `reads the last claim`() {
        assertEquals("fridge", parseClaim("I think under-bed... no!\nFOUND: fridge"))
    }

    @Test
    fun `accepts backticks`() {
        assertEquals("under-bed", parseClaim("FOUND: `under-bed`"))
    }

    @Test
    fun `no claim`() {
        assertNull(parseClaim("I give up."))
    }
}
