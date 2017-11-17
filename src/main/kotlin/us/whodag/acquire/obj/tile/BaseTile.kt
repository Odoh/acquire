package us.whodag.acquire.obj.tile

/**
 * Base implementation of a Tile.
 *
 * @property id unique identifier.
 * @property state state the Tile.
 */
data class BaseTile(private val id: TileId,
                    private val state: TileState) : Tile {

    override fun id(): TileId = id
    override fun state(): TileState = state
    override fun withState(state: TileState): Tile = copy(state = state)
}