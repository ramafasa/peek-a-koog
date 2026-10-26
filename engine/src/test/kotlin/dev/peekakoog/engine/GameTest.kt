package dev.peekakoog.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameTest {
    private fun game(hiddenSpotId: String = "desk", moveBudget: Int = 15) =
        Game(Houses.cozy, hiddenSpotId, moveBudget = moveBudget)

    @Test
    fun `finds Kodee by walking and peeking`() {
        val game = game()
        game.move("study")
        game.peek("desk")
        assertEquals(Status.FOUND, game.status)
        assertEquals(13, game.movesLeft)
    }

    @Test
    fun `cannot walk through walls`() {
        val game = game()
        val result = game.move("laundry-room")
        assertTrue(result.startsWith("Not allowed"))
        assertEquals("hall", game.currentRoomId)
        assertIs<GameEvent.Rejected>(game.events.last())
    }

    @Test
    fun `cannot peek into another room`() {
        val game = game()
        assertTrue(game.peek("desk").startsWith("Not allowed"))
        assertEquals(Status.SEEKING, game.status)
    }

    @Test
    fun `listening tells how far Kodee is`() {
        val game = game(hiddenSpotId = "washing-machine")
        assertTrue("COLD" in game.listen())
        game.move("bathroom")
        assertTrue("WARM" in game.listen())
        game.move("laundry-room")
        assertTrue("HOT" in game.listen())
    }

    @Test
    fun `runs out of moves`() {
        val game = game(moveBudget = 1)
        game.move("study")
        assertEquals(Status.OUT_OF_MOVES, game.status)
        assertTrue(game.peek("desk").startsWith("Not allowed"))
    }

    @Test
    fun `a lucky guess without looking is still exposed`() {
        val game = game()
        val claim = game.claim("desk")
        assertTrue(claim.correct)
        assertFalse(claim.everPeekedThere)
    }

    @Test
    fun `every room opens onto the hall, except the laundry room, the kitchen and the garage`() {
        val house = Houses.cozy
        val offTheHall = house.rooms.map { it.id }.filter { it != "hall" && "hall" !in house.doorsFrom(it) }
        assertEquals(listOf("laundry-room", "kitchen", "garage"), offTheHall)
        assertEquals(setOf("living-room"), house.doorsFrom("kitchen"))
        assertEquals(setOf("bathroom"), house.doorsFrom("laundry-room"))
        assertEquals(setOf("front-yard"), house.doorsFrom("garage"))
    }

    @Test
    fun `finds the shortest path through doors`() {
        assertEquals(listOf("bathroom", "laundry-room"), Houses.cozy.path("hall", "laundry-room"))
        assertEquals(listOf("living-room", "hall", "bedroom"), Houses.cozy.path("kitchen", "bedroom"))
        assertEquals(listOf("hall", "front-yard", "garage"), Houses.cozy.path("study", "garage"))
        assertEquals(emptyList(), Houses.cozy.path("hall", "hall"))
    }

    @Test
    fun `a free hint comes with every move when enabled`() {
        val game = Game(Houses.cozy, "washing-machine", freeHintAfterMove = true)
        val result = game.move("bathroom")
        assertTrue("WARM" in result)
        assertEquals(GameEvent.Listened(Warmth.WARM, free = true), game.events.last())
        assertEquals(game.moveBudget - 1, game.movesLeft)
    }

    @Test
    fun `no free hint by default`() {
        val game = game(hiddenSpotId = "washing-machine")
        game.move("bathroom")
        assertIs<GameEvent.Moved>(game.events.last())
    }
}
