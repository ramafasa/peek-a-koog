package dev.peekakoog.agent

import dev.peekakoog.engine.Game
import dev.peekakoog.engine.Houses
import dev.peekakoog.engine.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaselineTest {
    @Test
    fun `finds Kodee in every hiding spot within the move budget`() {
        Houses.cozy.spots.forEach { spot ->
            val game = Game(Houses.cozy, spot.id)
            val answer = baselineSeek(game)
            assertEquals(Status.FOUND, game.status, "Not found in ${spot.id}")
            assertEquals(spot.id, parseClaim(answer))
            assertTrue(game.movesLeft >= 0)
        }
    }
}
