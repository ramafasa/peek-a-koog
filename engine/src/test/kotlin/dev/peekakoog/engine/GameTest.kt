package dev.peekakoog.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameTest {
    private fun game(hiddenSpotId: String = "fridge", moveBudget: Int = 15) =
        Game(Houses.cozy, hiddenSpotId, moveBudget = moveBudget)

    @Test
    fun `finds Kodee by walking and peeking`() {
        val game = game()
        game.move("kitchen")
        game.peek("fridge")
        assertEquals(Status.FOUND, game.status)
        assertEquals(13, game.movesLeft)
    }

    @Test
    fun `cannot walk through walls`() {
        val game = game()
        val result = game.move("bathroom")
        assertTrue(result.startsWith("Not allowed"))
        assertEquals("hall", game.currentRoomId)
        assertIs<GameEvent.Rejected>(game.events.last())
    }

    @Test
    fun `cannot peek into another room`() {
        val game = game()
        assertTrue(game.peek("fridge").startsWith("Not allowed"))
        assertEquals(Status.SEEKING, game.status)
    }

    @Test
    fun `listening tells how far Kodee is`() {
        val game = game(hiddenSpotId = "bathtub")
        assertTrue("COLD" in game.listen())
        game.move("kitchen")
        assertTrue("WARM" in game.listen())
        game.move("bathroom")
        assertTrue("HOT" in game.listen())
    }

    @Test
    fun `runs out of moves`() {
        val game = game(moveBudget = 1)
        game.move("kitchen")
        assertEquals(Status.OUT_OF_MOVES, game.status)
        assertTrue(game.peek("fridge").startsWith("Not allowed"))
    }

    @Test
    fun `a lucky guess without looking is still exposed`() {
        val game = game()
        val claim = game.claim("fridge")
        assertTrue(claim.correct)
        assertFalse(claim.everPeekedThere)
    }

    @Test
    fun `finds the shortest path through doors`() {
        assertEquals(listOf("kitchen", "bathroom"), Houses.cozy.path("hall", "bathroom"))
        assertEquals(listOf("hall", "living-room", "bedroom"), Houses.cozy.path("kitchen", "bedroom"))
        assertEquals(emptyList(), Houses.cozy.path("hall", "hall"))
    }

    @Test
    fun `a free hint comes with every move when enabled`() {
        val game = Game(Houses.cozy, "bathtub", freeHintAfterMove = true)
        val result = game.move("kitchen")
        assertTrue("WARM" in result)
        assertEquals(GameEvent.Listened(Warmth.WARM, free = true), game.events.last())
        assertEquals(14, game.movesLeft)
    }

    @Test
    fun `no free hint by default`() {
        val game = game(hiddenSpotId = "bathtub")
        game.move("kitchen")
        assertIs<GameEvent.Moved>(game.events.last())
    }
}
