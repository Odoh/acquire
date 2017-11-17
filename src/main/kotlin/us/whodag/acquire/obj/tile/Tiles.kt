package us.whodag.acquire.obj.tile

/**
 * Factory for creating Tiles.
 */
object Tiles {

    val NUMBERS = (1..100).map(Int::toString)
    val LETTERS = ('A'..'Z').map(Char::toString)
    val STANDARD_IDS = NUMBERS.subList(0, 12).flatMap { n -> (LETTERS.subList(0, 9).map { l -> TileId(n, l) })}

    /** Create all the tiles for a standard game. */
    fun standard(): List<Tile> = Tiles.STANDARD_IDS.map { id -> customTile(id, TileState.DrawPile) }

    /** Create a single, custom tile. */
    fun customTile(id: TileId, state: TileState) = BaseTile(id, state)
}