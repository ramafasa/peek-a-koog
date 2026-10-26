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
    OIL_STAIN(onFloor = true),
    NIGHTSTAND, TOILET, PLANT, BALL, BLOCKS, COUNTER_SINK,
    POSTER, PICTURE, CLOCK, TOWEL, KITCHEN_WINDOW,
}

/** Where a hiding spot's furniture stands and where the detective stops to peek at it. */
data class SpotLayout(val spotId: String, val furniture: Furniture, val rect: Rect, val stand: Offset)

data class DecorLayout(val decor: Decor, val rect: Rect)

/**
 * Geometry of the floor plan in "house units", drawn in a 3/4 top-down view: the floor is seen from above and the
 * back wall of every room shows its face ([WALL_FACE] units tall). Every room is [ROOM_W] × [ROOM_H] and sits at its
 * engine `col`/`row`. This is pure presentation: the rules live in the engine.
 */
class FloorPlan(
    private val house: House,
    spots: List<SpotLayout>,
    decor: Map<String, List<DecorLayout>>,
) {
    val width = house.rooms.maxOf { it.col + 1 } * ROOM_W
    val height = house.rooms.maxOf { it.row + 1 } * ROOM_H

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
    }

    val spotLayouts: Collection<SpotLayout> get() = layouts.values

    fun roomRect(roomId: String): Rect = roomRect(house.room(roomId)!!)
    fun roomRect(room: Room): Rect = Rect(room.col * ROOM_W, room.row * ROOM_H, (room.col + 1) * ROOM_W, (room.row + 1) * ROOM_H)
    fun roomCenter(roomId: String): Offset = roomRect(roomId).let { Offset(it.center.x, it.top + (WALL_FACE + ROOM_H) / 2) }
    fun layout(spotId: String): SpotLayout = layouts.getValue(spotId)
    fun roomOf(spotId: String): String = house.spot(spotId)!!.roomId

    /** Middle of the doorway between two neighbouring rooms. */
    fun door(a: String, b: String): Offset {
        val ra = house.room(a)!!
        val rb = house.room(b)!!
        return if (ra.row == rb.row) {
            Offset(maxOf(ra.col, rb.col) * ROOM_W, ra.row * ROOM_H + (WALL_FACE + ROOM_H) / 2)
        } else {
            Offset(ra.col * ROOM_W + ROOM_W / 2, maxOf(ra.row, rb.row) * ROOM_H)
        }
    }

    /** Is a doorway on a vertical wall (rooms side by side)? */
    fun isVerticalDoor(a: String, b: String): Boolean = house.room(a)!!.row == house.room(b)!!.row

    fun roomAt(point: Offset): String? = house.rooms.firstOrNull { roomRect(it).contains(point) }?.id

    fun spotAt(point: Offset): String? =
        layouts.values.filter { it.rect.inflate(HIT_SLOP).contains(point) }.maxByOrNull { it.rect.bottom }?.spotId

    companion object {
        const val ROOM_W = 100f
        const val ROOM_H = 80f
        const val WALL = 2.5f
        const val WALL_FACE = 12f
        const val DOOR = 20f
        private const val HIT_SLOP = 1.5f

        /**
         * Furniture of [dev.peekakoog.engine.Houses.cozy], in room-local coordinates.
         * Doorways (back walls at x = 40..60, side walls at the middle of the floor) are kept clear.
         * The study's door to the kids room is in its front wall, so the middle of its front is kept clear too.
         */
        fun cozy(house: House) = FloorPlan(
            house,
            spots = listOf(
                spot("car", Furniture.CAR, 10f, 14f, 30f, 52f, stand = Offset(52f, 46f)),
                spot("workbench", Furniture.WORKBENCH, 56f, 0f, 38f, 30f, stand = Offset(74f, 40f)),
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

                spot("coat-rack", Furniture.COAT_RACK, 6f, 2f, 18f, 32f, stand = Offset(24f, 44f)),
                spot("shoe-cabinet", Furniture.SHOE_CABINET, 68f, 8f, 26f, 20f, stand = Offset(80f, 38f)),

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
                    decor(Decor.PICTURE, 50f, 2f, 10f, 8f),
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
                    decor(Decor.RUNNER, 42f, 46f, 16f, 26f),
                    decor(Decor.DOORMAT, 41f, 72f, 18f, 7f),
                    decor(Decor.CLOCK, 30f, 2f, 8f, 8f),
                    decor(Decor.PLANT, 4f, 60f, 10f, 16f),
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
