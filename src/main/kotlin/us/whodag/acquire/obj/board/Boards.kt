package us.whodag.acquire.obj.board

import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles

/**
 * Factory for creating Boards.
 */
object Boards {

    val STANDARD_X_SIZE = 9
    val STANDARD_Y_SIZE = 12

    /** Create a board for a standard game. */
    fun standard(): Board = custom(STANDARD_X_SIZE, STANDARD_Y_SIZE)

    /** Create a custom board. */
    fun custom(xSize: Int, ySize: Int): Board {
        require(xSize < Tiles.LETTERS.size, { "Size of the board cannot exceed the number of possible letters" })
        require(ySize < Tiles.NUMBERS.size, { "Size of the board cannot exceed the number of possible numbers" })
        val array = Array(xSize, { _ -> Array<BoardTile?>(ySize, { _ -> null })})
        return BaseBoard(array)
    }

    /** A copy of board with tile added. */
    fun Board.withTile(tile: TileId): Board = this.withTiles(listOf(tile))

    /** A copy of board with tiles added. */
    fun Board.withTiles(tiles: Collection<TileId>): Board =
        tiles.fold(this) { board, tile -> board.addTile(tile) }
}