package dev.peekakoog.engine

enum class Status { SEEKING, FOUND, OUT_OF_MOVES }

enum class Warmth { HOT, WARM, COLD }

/** Everything that happened in a game, in order. The UI and the talk replay this log. */
sealed interface GameEvent {
    data class Moved(val from: String, val to: String) : GameEvent
    data class Looked(val roomId: String) : GameEvent
    data class Peeked(val spotId: String, val found: Boolean) : GameEvent
    /** [free] hints come with a move (see [Game.freeHintAfterMove]) and cost nothing. */
    data class Listened(val warmth: Warmth, val free: Boolean = false) : GameEvent
    data class Claimed(val spotId: String?, val correct: Boolean, val everPeekedThere: Boolean) : GameEvent
    data class Rejected(val action: String, val reason: String) : GameEvent
}

/**
 * The rules of hide-and-seek. Pure, deterministic Kotlin: the engine — never the LLM —
 * knows where Kodee is and decides whether the seeker found it.
 *
 * With [freeHintAfterMove], every move also tells the seeker how warm the new room is, for free.
 */
class Game(
    val house: House,
    val hiddenSpotId: String,
    startRoomId: String = "hall",
    val moveBudget: Int = 15,
    val freeHintAfterMove: Boolean = false,
    private val onEvent: (GameEvent) -> Unit = {},
) {
    init {
        require(house.spot(hiddenSpotId) != null) { "Unknown hiding spot: $hiddenSpotId" }
        require(house.room(startRoomId) != null) { "Unknown start room: $startRoomId" }
    }

    var currentRoomId: String = startRoomId
        private set
    var movesLeft: Int = moveBudget
        private set
    var status: Status = Status.SEEKING
        private set

    private val _peekedSpots = mutableListOf<String>()
    val peekedSpots: List<String> get() = _peekedSpots

    private val _events = mutableListOf<GameEvent>()
    val events: List<GameEvent> get() = _events

    private val hiddenRoomId = house.spot(hiddenSpotId)!!.roomId

    /** Walk through a door to a neighbouring room. Costs one move. */
    @Synchronized
    fun move(roomId: String): String {
        rejectIfOver("move")?.let { return it }
        if (house.room(roomId) == null) return reject("move", "There is no room called '$roomId'.")
        if (roomId !in house.doorsFrom(currentRoomId)) {
            return reject("move", "There is no door from '$currentRoomId' to '$roomId'.")
        }
        val from = currentRoomId
        currentRoomId = roomId
        spendMove()
        emit(GameEvent.Moved(from, roomId))
        val arrived = "You are now in the ${house.room(roomId)!!.name}. Moves left: $movesLeft."
        if (!freeHintAfterMove || status != Status.SEEKING) return arrived
        val warmth = warmthHere()
        emit(GameEvent.Listened(warmth, free = true))
        return "$arrived You hear: $warmth."
    }

    /** Describe the current room: its hiding spots and doors. Free. */
    @Synchronized
    fun look(): String {
        val room = house.room(currentRoomId)!!
        emit(GameEvent.Looked(room.id))
        val spots = house.spotsIn(room.id).joinToString("\n") { spot ->
            val checked = if (spot.id in _peekedSpots) " (already checked)" else ""
            "- ${spot.id}: ${spot.name}$checked"
        }
        val doors = house.doorsFrom(room.id).joinToString(", ")
        return "You are in the ${room.name} (${room.id}).\nHiding spots:\n$spots\nDoors to: $doors\nMoves left: $movesLeft."
    }

    /** Check one hiding spot in the current room. Costs one move. */
    @Synchronized
    fun peek(spotId: String): String {
        rejectIfOver("peek")?.let { return it }
        val spot = house.spot(spotId) ?: return reject("peek", "There is no hiding spot called '$spotId'.")
        if (spot.roomId != currentRoomId) {
            return reject("peek", "'$spotId' is not in this room. You are in '$currentRoomId'.")
        }
        val found = spotId == hiddenSpotId
        _peekedSpots += spotId
        spendMove()
        if (found) status = Status.FOUND
        emit(GameEvent.Peeked(spotId, found))
        return if (found) "🎉 Found Kodee! It was ${spot.name.replaceFirstChar(Char::lowercase)}."
        else "Nothing ${spot.name.replaceFirstChar(Char::lowercase)}. Moves left: $movesLeft."
    }

    /** Listen for Kodee: HOT in this room, WARM one door away, COLD otherwise. Costs one move. */
    @Synchronized
    fun listen(): String {
        rejectIfOver("listen")?.let { return it }
        val warmth = warmthHere()
        spendMove()
        emit(GameEvent.Listened(warmth))
        return "You listen carefully... $warmth. Moves left: $movesLeft."
    }

    /**
     * Record where the seeker *says* Kodee is. The engine checks the claim against the truth,
     * so a confident answer without looking is exposed as a guess.
     */
    @Synchronized
    fun claim(spotId: String?): GameEvent.Claimed {
        val event = GameEvent.Claimed(
            spotId = spotId,
            correct = spotId == hiddenSpotId,
            everPeekedThere = spotId != null && spotId in _peekedSpots,
        )
        emit(event)
        return event
    }

    private fun warmthHere(): Warmth = when (house.distance(currentRoomId, hiddenRoomId)) {
        0 -> Warmth.HOT
        1 -> Warmth.WARM
        else -> Warmth.COLD
    }

    private fun spendMove() {
        movesLeft--
        if (movesLeft <= 0 && status == Status.SEEKING) status = Status.OUT_OF_MOVES
    }

    private fun rejectIfOver(action: String): String? = when (status) {
        Status.FOUND -> reject(action, "The game is over: Kodee has already been found.")
        Status.OUT_OF_MOVES -> reject(action, "The game is over: no moves left.")
        Status.SEEKING -> null
    }

    private fun reject(action: String, reason: String): String {
        emit(GameEvent.Rejected(action, reason))
        return "Not allowed: $reason"
    }

    private fun emit(event: GameEvent) {
        _events += event
        onEvent(event)
    }
}
