package dev.peekakoog.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.peekakoog.engine.House
import dev.peekakoog.engine.Room

enum class Furniture {
    BED, WARDROBE, CURTAIN, TOY_BOX, BUNK_BED, TENT, BATHTUB, LAUNDRY, SINK,
    SOFA, TV, BOOKSHELF, COAT_RACK, SHOE_CABINET, FRIDGE, OVEN, PANTRY, TABLE,
}

/** Where a hiding spot's furniture stands and where the detective stops to peek at it. */
data class SpotLayout(val spotId: String, val furniture: Furniture, val rect: Rect, val stand: Offset)

/**
 * Geometry of the top-down floor plan, in "house units". Every room is [ROOM_W] × [ROOM_H] and sits at
 * its engine `col`/`row`. This is pure presentation: the rules live in the engine.
 */
class FloorPlan(private val house: House, localLayouts: List<SpotLayout>) {
    val width = house.rooms.maxOf { it.col + 1 } * ROOM_W
    val height = house.rooms.maxOf { it.row + 1 } * ROOM_H

    private val layouts: Map<String, SpotLayout> = localLayouts.associate { local ->
        val origin = roomRect(house.spot(local.spotId)!!.roomId).topLeft
        local.spotId to local.copy(rect = local.rect.translate(origin), stand = local.stand + origin)
    }

    init {
        val missing = house.spots.map { it.id } - layouts.keys
        require(missing.isEmpty()) { "No furniture placed for spots: $missing" }
    }

    val spotLayouts: Collection<SpotLayout> get() = layouts.values

    fun roomRect(roomId: String): Rect = roomRect(house.room(roomId)!!)
    fun roomRect(room: Room): Rect = Rect(room.col * ROOM_W, room.row * ROOM_H, (room.col + 1) * ROOM_W, (room.row + 1) * ROOM_H)
    fun roomCenter(roomId: String): Offset = roomRect(roomId).center
    fun layout(spotId: String): SpotLayout = layouts.getValue(spotId)

    /** Middle of the doorway between two neighbouring rooms. */
    fun door(a: String, b: String): Offset {
        val ra = house.room(a)!!
        val rb = house.room(b)!!
        return if (ra.row == rb.row) {
            Offset(maxOf(ra.col, rb.col) * ROOM_W, ra.row * ROOM_H + ROOM_H / 2)
        } else {
            Offset(ra.col * ROOM_W + ROOM_W / 2, maxOf(ra.row, rb.row) * ROOM_H)
        }
    }

    /** Is a doorway on a vertical wall (rooms side by side)? */
    fun isVerticalDoor(a: String, b: String): Boolean = house.room(a)!!.row == house.room(b)!!.row

    fun spotAt(point: Offset): String? =
        layouts.values.firstOrNull { it.rect.inflate(HIT_SLOP).contains(point) }?.spotId

    companion object {
        const val ROOM_W = 100f
        const val ROOM_H = 80f
        const val WALL = 3f
        const val DOOR = 20f
        private const val HIT_SLOP = 2f

        /** Furniture of [dev.peekakoog.engine.Houses.cozy], in room-local coordinates. Doorways are kept clear. */
        fun cozy(house: House) = FloorPlan(
            house,
            listOf(
                spot("under-bed", Furniture.BED, 8f, 8f, 30f, 40f, stand = Offset(48f, 30f)),
                spot("wardrobe", Furniture.WARDROBE, 64f, 6f, 30f, 14f, stand = Offset(78f, 30f)),
                spot("behind-curtain", Furniture.CURTAIN, 90f, 36f, 6f, 30f, stand = Offset(78f, 52f)),
                spot("bunk-bed", Furniture.BUNK_BED, 6f, 8f, 26f, 40f, stand = Offset(42f, 28f)),
                spot("tent", Furniture.TENT, 62f, 8f, 30f, 30f, stand = Offset(70f, 48f)),
                spot("toy-box", Furniture.TOY_BOX, 8f, 56f, 22f, 16f, stand = Offset(36f, 60f)),
                spot("bathtub", Furniture.BATHTUB, 60f, 6f, 34f, 20f, stand = Offset(72f, 36f)),
                spot("under-sink", Furniture.SINK, 8f, 6f, 24f, 14f, stand = Offset(22f, 30f)),
                spot("laundry-basket", Furniture.LAUNDRY, 8f, 56f, 16f, 16f, stand = Offset(32f, 60f)),
                spot("bookshelf", Furniture.BOOKSHELF, 5f, 8f, 10f, 34f, stand = Offset(24f, 26f)),
                spot("sofa-cushions", Furniture.SOFA, 10f, 54f, 40f, 18f, stand = Offset(30f, 44f)),
                spot("behind-tv", Furniture.TV, 62f, 64f, 28f, 8f, stand = Offset(74f, 52f)),
                spot("coat-rack", Furniture.COAT_RACK, 8f, 6f, 16f, 14f, stand = Offset(24f, 30f)),
                spot("shoe-cabinet", Furniture.SHOE_CABINET, 38f, 62f, 26f, 12f, stand = Offset(50f, 52f)),
                spot("oven", Furniture.OVEN, 60f, 6f, 16f, 15f, stand = Offset(66f, 30f)),
                spot("fridge", Furniture.FRIDGE, 80f, 6f, 15f, 19f, stand = Offset(80f, 34f)),
                spot("pantry", Furniture.PANTRY, 80f, 54f, 15f, 20f, stand = Offset(72f, 62f)),
                spot("under-table", Furniture.TABLE, 26f, 48f, 28f, 20f, stand = Offset(42f, 40f)),
            ),
        )

        private fun spot(id: String, furniture: Furniture, x: Float, y: Float, w: Float, h: Float, stand: Offset) =
            SpotLayout(id, furniture, Rect(x, y, x + w, y + h), stand)
    }
}
