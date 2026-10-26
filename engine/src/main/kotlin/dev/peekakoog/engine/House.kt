package dev.peekakoog.engine

/** A room in the house, or a place outside it. Where it is drawn is up to the app; the engine only knows the doors. */
data class Room(
    val id: String,
    val name: String,
    val emoji: String,
)

/** A place in a room where something can be hidden. */
data class Spot(
    val id: String,
    val roomId: String,
    val name: String,
    val emoji: String,
)

class House(
    val rooms: List<Room>,
    val spots: List<Spot>,
    doors: List<Pair<String, String>>,
) {
    private val roomsById = rooms.associateBy { it.id }
    private val spotsById = spots.associateBy { it.id }
    private val neighbours: Map<String, Set<String>> =
        (doors + doors.map { (a, b) -> b to a })
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }

    init {
        require(spots.all { it.roomId in roomsById }) { "Every spot must belong to an existing room" }
        require(neighbours.keys.all { it in roomsById }) { "Every door must connect existing rooms" }
    }

    fun room(id: String): Room? = roomsById[id]
    fun spot(id: String): Spot? = spotsById[id]
    fun spotsIn(roomId: String): List<Spot> = spots.filter { it.roomId == roomId }
    fun doorsFrom(roomId: String): Set<String> = neighbours[roomId].orEmpty()

    /** Rooms to walk through, door by door, from [from] to [to] (excluding [from]). Empty when already there. */
    fun path(from: String, to: String): List<String> {
        val previous = mutableMapOf(from to from)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val room = queue.removeFirst()
            if (room == to) break
            doorsFrom(room).sorted().forEach { next ->
                if (next !in previous) {
                    previous[next] = room
                    queue += next
                }
            }
        }
        if (to !in previous) return emptyList()
        return generateSequence(to) { room -> previous[room]?.takeIf { room != from } }
            .takeWhile { it != from }
            .toList()
            .reversed()
    }

    /** Number of doors between two rooms (breadth-first search). */
    fun distance(from: String, to: String): Int {
        if (from == to) return 0
        val visited = mutableSetOf(from)
        var frontier = setOf(from)
        var steps = 0
        while (frontier.isNotEmpty()) {
            steps++
            frontier = frontier.flatMap { doorsFrom(it) }.filter { visited.add(it) }.toSet()
            if (to in frontier) return steps
        }
        return Int.MAX_VALUE
    }
}

object Houses {
    /**
     * The house used in the talk. Every room opens onto the long hall, except three: the kitchen is
     * behind the living room, the laundry room behind the bathroom, and the garage is outside, across the front yard.
     *
     * ```
     * bedroom | kids room | bathroom ┊ laundry room
     * ========== hall ===============🚪 front yard
     * living  ┊  kitchen  |  study   ┊ garage
     * ```
     */
    val cozy = House(
        rooms = listOf(
            Room("hall", "Hall", "🚪"),
            Room("bedroom", "Bedroom", "🛏️"),
            Room("kids-room", "Kids room", "🧸"),
            Room("bathroom", "Bathroom", "🛁"),
            Room("laundry-room", "Laundry room", "🫧"),
            Room("living-room", "Living room", "🛋️"),
            Room("study", "Study", "📝"),
            Room("kitchen", "Kitchen", "🍳"),
            Room("front-yard", "Front yard", "🌳"),
            Room("garage", "Garage", "🚗"),
        ),
        spots = listOf(
            Spot("car", "garage", "In the car", "🚗"),
            Spot("workbench", "garage", "Under the workbench", "🔧"),
            Spot("boxes", "garage", "In the cardboard boxes", "📦"),
            Spot("desk", "study", "Under the desk", "🖥️"),
            Spot("armchair", "study", "Behind the armchair", "🪑"),
            Spot("filing-cabinet", "study", "In the filing cabinet", "🗄️"),
            Spot("washing-machine", "laundry-room", "In the washing machine", "🫧"),
            Spot("drying-rack", "laundry-room", "Behind the drying rack", "👕"),
            Spot("linen-cupboard", "laundry-room", "In the linen cupboard", "🧺"),
            Spot("under-bed", "bedroom", "Under the bed", "🛏️"),
            Spot("wardrobe", "bedroom", "In the wardrobe", "🚪"),
            Spot("behind-curtain", "bedroom", "Behind the curtain", "🪟"),
            Spot("toy-box", "kids-room", "In the toy box", "📦"),
            Spot("bunk-bed", "kids-room", "On the top bunk", "🪜"),
            Spot("tent", "kids-room", "In the play tent", "⛺"),
            Spot("bathtub", "bathroom", "In the bathtub", "🛁"),
            Spot("laundry-basket", "bathroom", "In the laundry basket", "🧺"),
            Spot("under-sink", "bathroom", "Under the sink", "🚰"),
            Spot("sofa-cushions", "living-room", "Between the sofa cushions", "🛋️"),
            Spot("behind-tv", "living-room", "Behind the TV", "📺"),
            Spot("bookshelf", "living-room", "On the bookshelf", "📚"),
            Spot("coat-rack", "hall", "Among the coats", "🧥"),
            Spot("shoe-cabinet", "hall", "In the shoe cabinet", "👟"),
            Spot("fridge", "kitchen", "In the fridge", "🧊"),
            Spot("oven", "kitchen", "In the oven", "🔥"),
            Spot("pantry", "kitchen", "In the pantry", "🥫"),
            Spot("under-table", "kitchen", "Under the table", "🪑"),
        ),
        doors = listOf(
            "hall" to "bedroom",
            "hall" to "kids-room",
            "hall" to "bathroom",
            "hall" to "living-room",
            "hall" to "study",
            "living-room" to "kitchen",
            "bathroom" to "laundry-room",
            "hall" to "front-yard",
            "front-yard" to "garage",
        ),
    )
}
