package dev.peekakoog.engine

/** A room in the house. [col] and [row] place it on the map grid. */
data class Room(
    val id: String,
    val name: String,
    val emoji: String,
    val col: Int,
    val row: Int,
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
     * The house used in the talk:
     *
     * ```
     * bedroom  | kids room | bathroom
     * ---------+-----------+---------
     * living   |   hall    | kitchen
     * ```
     */
    val cozy = House(
        rooms = listOf(
            Room("bedroom", "Bedroom", "🛏️", col = 0, row = 0),
            Room("kids-room", "Kids room", "🧸", col = 1, row = 0),
            Room("bathroom", "Bathroom", "🛁", col = 2, row = 0),
            Room("living-room", "Living room", "🛋️", col = 0, row = 1),
            Room("hall", "Hall", "🚪", col = 1, row = 1),
            Room("kitchen", "Kitchen", "🍳", col = 2, row = 1),
        ),
        spots = listOf(
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
            "hall" to "living-room",
            "hall" to "kitchen",
            "hall" to "kids-room",
            "living-room" to "bedroom",
            "kitchen" to "bathroom",
        ),
    )
}
