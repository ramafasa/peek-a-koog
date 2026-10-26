package dev.peekakoog.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.peekakoog.engine.House
import dev.peekakoog.engine.Room

/** Furniture that hides a spot. */
enum class Furniture {
    BED, WARDROBE, CURTAIN, TOY_BOX, BUNK_BED, TENT, BATHTUB, LAUNDRY, SINK,
    SOFA, TV, BOOKSHELF, COAT_RACK, SHOE_CABINET, FRIDGE, OVEN, PANTRY, TABLE,
    CAR, WORKBENCH, BOXES, DESK, ARMCHAIR, FILING_CABINET, WASHER, DRYING_RACK, LINEN_CUPBOARD,
}

/** Things that only make a room recognisable. Nothing can hide in them. */
enum class Decor(val onFloor: Boolean = false) {
    RUG(onFloor = true), ROUND_RUG(onFloor = true), RUNNER(onFloor = true), BATH_MAT(onFloor = true), DOORMAT(onFloor = true),
    OIL_STAIN(onFloor = true), PATH(onFloor = true), BUSH,
    NIGHTSTAND, TOILET, PLANT, BALL, BLOCKS, COUNTER_SINK,
    POSTER, PICTURE, CLOCK, TOWEL, KITCHEN_WINDOW,
}

/** Where a hiding spot's furniture stands and where the detective stops to peek at it. */
data class SpotLayout(val spotId: String, val furniture: Furniture, val rect: Rect, val stand: Offset)

data class DecorLayout(val decor: Decor, val rect: Rect)

/**
 * Geometry of the floor plan in "house units", drawn in a 3/4 top-down view: the floor is seen from above and the
 * back wall of every indoor room shows its face ([WALL_FACE] units tall). Rooms are rectangles of any size; a door
 * sits in the middle of the wall two rooms share. This is pure presentation: the rules live in the engine.
 *
 * @param outdoors places outside the house: no walls, and the detective walks across them without stopping.
 * @param passages rooms the detective only passes through, so it doesn't walk to their middle on the way.
 */
class FloorPlan(
    private val house: House,
    private val rooms: Map<String, Rect>,
    spots: List<SpotLayout>,
    decor: Map<String, List<DecorLayout>>,
    val outdoors: Set<String> = emptySet(),
    private val passages: Set<String> = emptySet(),
) {
    init {
        val missing = house.rooms.map { it.id } - rooms.keys
        require(missing.isEmpty()) { "No place on the plan for rooms: $missing" }
    }

    val width = rooms.values.maxOf { it.right }
    val height = rooms.values.maxOf { it.bottom }

    private val layouts: Map<String, SpotLayout> = spots.associate { local ->
        val origin = roomRect(house.spot(local.spotId)!!.roomId).topLeft
        local.spotId to local.copy(rect = local.rect.translate(origin), stand = local.stand + origin)
    }

    val decor: List<DecorLayout> = decor.flatMap { (roomId, items) ->
        val origin = roomRect(roomId).topLeft
        items.map { it.copy(rect = it.rect.translate(origin)) }
    }

    init {
        val missing = house.spots.map { it.id } - layouts.keys
        require(missing.isEmpty()) { "No furniture placed for spots: $missing" }
        house.rooms.forEach { room -> house.doorsFrom(room.id).forEach { door(room.id, it) } }
    }

    val spotLayouts: Collection<SpotLayout> get() = layouts.values

    fun roomRect(roomId: String): Rect = rooms.getValue(roomId)
    fun roomRect(room: Room): Rect = roomRect(room.id)

    /** The middle of a room's floor (below its wall face). */
    fun roomCenter(roomId: String): Offset = roomRect(roomId).let { Offset(it.center.x, floorTop(roomId) + (it.bottom - floorTop(roomId)) / 2) }
    fun layout(spotId: String): SpotLayout = layouts.getValue(spotId)
    fun roomOf(spotId: String): String = house.spot(spotId)!!.roomId
    fun isPassage(roomId: String): Boolean = roomId in passages

    private fun floorTop(roomId: String) = roomRect(roomId).top + if (roomId in outdoors) 0f else WALL_FACE

    /** Middle of the doorway between two neighbouring rooms: in the middle of the wall they share. */
    fun door(a: String, b: String): Offset {
        val ra = roomRect(a)
        val rb = roomRect(b)
        return if (isVerticalDoor(a, b)) {
            val x = if (ra.right == rb.left) ra.right else ra.left
            val top = maxOf(floorTop(a), floorTop(b))
            Offset(x, top + (minOf(ra.bottom, rb.bottom) - top) / 2)
        } else {
            val y = if (ra.bottom == rb.top) ra.bottom else ra.top
            Offset((maxOf(ra.left, rb.left) + minOf(ra.right, rb.right)) / 2, y)
        }
    }

    /** Is a doorway in a vertical wall (rooms side by side)? */
    fun isVerticalDoor(a: String, b: String): Boolean {
        val ra = roomRect(a)
        val rb = roomRect(b)
        return when {
            ra.right == rb.left || rb.right == ra.left -> true
            ra.bottom == rb.top || rb.bottom == ra.top -> false
            else -> error("'$a' and '$b' have a door but share no wall")
        }
    }

    /** A step into [roomId] from the doorway to [other]: where the detective stands just after walking in. */
    fun justInside(roomId: String, other: String): Offset {
        val door = door(roomId, other)
        val r = roomRect(roomId)
        return if (isVerticalDoor(roomId, other)) {
            Offset(if (door.x == r.left) door.x + STEP_IN else door.x - STEP_IN, door.y)
        } else if (door.y == r.top) {
            Offset(door.x, floorTop(roomId) + STEP_IN)
        } else {
            Offset(door.x, door.y - STEP_IN)
        }
    }

    /** Waypoints for walking from [from] to the neighbouring room [to]: to the door, through it, and in. */
    fun walk(from: String, to: String): List<Offset> = buildList {
        add(justInside(from, to))
        add(door(from, to))
        add(justInside(to, from))
        if (!isPassage(to)) add(roomCenter(to))
    }

    fun roomAt(point: Offset): String? = house.rooms.firstOrNull { roomRect(it).contains(point) }?.id

    fun spotAt(point: Offset): String? =
        layouts.values.filter { it.rect.inflate(HIT_SLOP).contains(point) }.maxByOrNull { it.rect.bottom }?.spotId

    companion object {
        const val WALL = 2.5f
        const val WALL_FACE = 12f
        const val DOOR = 20f
        private const val HIT_SLOP = 1.5f
        private const val STEP_IN = 12f

        /**
         * Rooms and furniture of [dev.peekakoog.engine.Houses.cozy]. Furniture is in room-local coordinates.
         * Every room is 100 × 80 with its doorway at x = 40..60 (the kitchen's is in its left wall, from the
         * living room, at the middle of the floor); the hall is long and narrow, with three
         * doorways in its back wall (x = 40..60, 140..160, 240..260) and the front door at its right end.
         */
        fun cozy(house: House) = FloorPlan(
            house,
            rooms = mapOf(
                "bedroom" to Rect(0f, 0f, 100f, 80f),
                "kids-room" to Rect(100f, 0f, 200f, 80f),
                "bathroom" to Rect(200f, 0f, 300f, 80f),
                "laundry-room" to Rect(300f, 0f, 400f, 80f),
                "hall" to Rect(0f, 80f, 300f, 124f),
                "front-yard" to Rect(300f, 80f, 400f, 124f),
                "living-room" to Rect(0f, 124f, 100f, 204f),
                "kitchen" to Rect(100f, 124f, 200f, 204f),
                "study" to Rect(200f, 124f, 300f, 204f),
                "garage" to Rect(300f, 124f, 400f, 204f),
            ),
            outdoors = setOf("front-yard"),
            passages = setOf("hall", "front-yard"),
            spots = listOf(
                spot("car", Furniture.CAR, 10f, 14f, 30f, 52f, stand = Offset(52f, 46f)),
                spot("workbench", Furniture.WORKBENCH, 62f, 0f, 34f, 30f, stand = Offset(79f, 40f)),
                spot("boxes", Furniture.BOXES, 62f, 50f, 24f, 20f, stand = Offset(54f, 62f)),

                spot("desk", Furniture.DESK, 8f, 0f, 32f, 30f, stand = Offset(24f, 40f)),
                spot("filing-cabinet", Furniture.FILING_CABINET, 78f, 0f, 16f, 30f, stand = Offset(86f, 40f)),
                spot("armchair", Furniture.ARMCHAIR, 66f, 48f, 22f, 20f, stand = Offset(58f, 60f)),

                spot("washing-machine", Furniture.WASHER, 24f, 2f, 18f, 28f, stand = Offset(33f, 40f)),
                spot("linen-cupboard", Furniture.LINEN_CUPBOARD, 78f, 0f, 18f, 32f, stand = Offset(87f, 42f)),
                spot("drying-rack", Furniture.DRYING_RACK, 44f, 48f, 34f, 20f, stand = Offset(36f, 60f)),

                spot("under-bed", Furniture.BED, 6f, 6f, 32f, 42f, stand = Offset(46f, 42f)),
                spot("behind-curtain", Furniture.CURTAIN, 54f, 0f, 22f, 32f, stand = Offset(65f, 42f)),
                spot("wardrobe", Furniture.WARDROBE, 79f, 0f, 17f, 30f, stand = Offset(87f, 40f)),

                spot("bunk-bed", Furniture.BUNK_BED, 5f, 2f, 26f, 46f, stand = Offset(40f, 44f)),
                spot("tent", Furniture.TENT, 60f, 14f, 32f, 30f, stand = Offset(70f, 52f)),
                spot("toy-box", Furniture.TOY_BOX, 8f, 58f, 22f, 16f, stand = Offset(38f, 68f)),

                spot("under-sink", Furniture.SINK, 6f, 0f, 22f, 30f, stand = Offset(18f, 40f)),
                spot("bathtub", Furniture.BATHTUB, 56f, 6f, 38f, 26f, stand = Offset(74f, 42f)),
                spot("laundry-basket", Furniture.LAUNDRY, 6f, 56f, 16f, 18f, stand = Offset(30f, 66f)),

                spot("bookshelf", Furniture.BOOKSHELF, 4f, 0f, 22f, 34f, stand = Offset(18f, 44f)),
                spot("behind-tv", Furniture.TV, 64f, 2f, 28f, 28f, stand = Offset(76f, 40f)),
                spot("sofa-cushions", Furniture.SOFA, 30f, 54f, 40f, 20f, stand = Offset(24f, 62f)),

                spot("coat-rack", Furniture.COAT_RACK, 74f, 2f, 18f, 32f, stand = Offset(100f, 36f)),
                spot("shoe-cabinet", Furniture.SHOE_CABINET, 172f, 8f, 26f, 20f, stand = Offset(185f, 36f)),

                spot("pantry", Furniture.PANTRY, 4f, 0f, 20f, 34f, stand = Offset(20f, 46f)),
                spot("oven", Furniture.OVEN, 62f, 6f, 16f, 24f, stand = Offset(70f, 40f)),
                spot("fridge", Furniture.FRIDGE, 80f, 0f, 16f, 30f, stand = Offset(88f, 40f)),
                spot("under-table", Furniture.TABLE, 34f, 52f, 34f, 18f, stand = Offset(26f, 62f)),
            ),
            decor = mapOf(
                "garage" to listOf(
                    decor(Decor.OIL_STAIN, 16f, 70f, 18f, 6f),
                ),
                "study" to listOf(
                    decor(Decor.RUG, 28f, 40f, 30f, 20f),
                    decor(Decor.PICTURE, 64f, 2f, 10f, 8f),
                    decor(Decor.PLANT, 4f, 60f, 10f, 16f),
                ),
                "laundry-room" to listOf(
                    decor(Decor.KITCHEN_WINDOW, 54f, 1f, 12f, 4.5f),
                    decor(Decor.BATH_MAT, 22f, 36f, 22f, 7f),
                ),
                "bedroom" to listOf(
                    decor(Decor.RUG, 26f, 52f, 44f, 20f),
                    decor(Decor.NIGHTSTAND, 40f, 10f, 10f, 12f),
                ),
                "kids-room" to listOf(
                    decor(Decor.ROUND_RUG, 36f, 48f, 32f, 22f),
                    decor(Decor.POSTER, 64f, 1.5f, 14f, 9f),
                    decor(Decor.BALL, 72f, 62f, 8f, 8f),
                    decor(Decor.BLOCKS, 82f, 56f, 12f, 10f),
                ),
                "bathroom" to listOf(
                    decor(Decor.BATH_MAT, 62f, 36f, 24f, 8f),
                    decor(Decor.TOILET, 33f, 6f, 13f, 20f),
                    decor(Decor.TOWEL, 48f, 1.5f, 6f, 10f),
                ),
                "living-room" to listOf(
                    decor(Decor.RUG, 28f, 38f, 44f, 22f),
                    decor(Decor.PICTURE, 28f, 2f, 10f, 8f),
                    decor(Decor.PLANT, 4f, 60f, 10f, 16f),
                ),
                "hall" to listOf(
                    decor(Decor.RUNNER, 22f, 29f, 256f, 10f),
                    decor(Decor.DOORMAT, 284f, 17f, 10f, 22f),
                    decor(Decor.CLOCK, 116f, 2f, 8f, 8f),
                    decor(Decor.PICTURE, 212f, 2f, 10f, 8f),
                    decor(Decor.PLANT, 4f, 22f, 10f, 16f),
                ),
                "front-yard" to listOf(
                    decor(Decor.PATH, 0f, 14f, 60f, 30f),
                    decor(Decor.BUSH, 72f, 6f, 22f, 14f),
                ),
                "kitchen" to listOf(
                    decor(Decor.COUNTER_SINK, 25f, 6f, 14f, 24f),
                    decor(Decor.KITCHEN_WINDOW, 26f, 1f, 12f, 4.5f),
                ),
            ),
        )

        private fun spot(id: String, furniture: Furniture, x: Float, y: Float, w: Float, h: Float, stand: Offset) =
            SpotLayout(id, furniture, Rect(x, y, x + w, y + h), stand)

        private fun decor(decor: Decor, x: Float, y: Float, w: Float, h: Float) = DecorLayout(decor, Rect(x, y, x + w, y + h))
    }
}
