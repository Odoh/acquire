package us.whodag.acquire.obj.board

import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.split
import java.util.*

/**
 * Base implementation of an Acquire Board.
 *
 *      y0 y1 y2 ...
 *   x0 x  x  x
 *   x1 x  x  x
 *   x2 x  x  x
 *
 *   x - letters
 *   y - numbers
 *
 * @property 2-D array which holds the structuree of the board.
 */
data class BaseBoard(val tiles: Array<Array<BoardTile?>>) : Board {

    override fun maxX(): Int = tiles.size
    override fun maxY(): Int = tiles[0].size

    override fun tiles(): List<TileId> {
        val tiles = mutableListOf<TileId>()
        for (x in 0 until maxX()) {
            (0 until maxY()).mapNotNullTo(tiles) { y -> tile(x, y) }
        }
        return tiles.toList()
    }

    override fun tile(x: Int, y: Int): TileId? = tiles[x][y]?.tile
    override fun containsTile(tile: TileId): Boolean {
        val (x, y) = tile.toXY()
        return tile(x, y) != null
    }

    override fun adjacentTiles(tile: TileId): List<TileId> = adjacentBoardTiles(tile).map { t -> t.tile }
    override fun connectedTiles(tile: TileId): List<TileId> {
        val xy = tile.toXY()
        return tiles[xy.x][xy.y]?.group ?: emptyList()
    }
    override fun addTile(tile: TileId): Board = addTileAndCombine(tile)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseBoard

        if (!Arrays.equals(tiles, other.tiles)) return false

        return true
    }
    override fun hashCode(): Int = Arrays.hashCode(tiles)
}

/**
 * The data saved in BaseBoard.
 * Need to track all the connected tiles.
 *
 * @property tile the acquire tile this corresponds to.
 * @property group the group of connected thiles this is a part of.
 */
data class BoardTile(val tile: TileId, val group: List<TileId>)

/* Convert a TileId into an (X, Y) coordinate. */
private fun TileId.toXY(): XY {
    val (num, char) = split()
    val y = num - 1
    val x = char.toInt().minus(65)
    return XY(x, y)
}
data class XY(val x: Int, val y: Int)

/* Add a tile to the board and combine all adjacent, grouped tiles. */
fun BaseBoard.addTileAndCombine(tile: TileId): BaseBoard {
    val xy = tile.toXY()
    require(tiles[xy.x][xy.y] == null, { "Tile to add must no already be on the board" })

    // combine all adjacent tiles with this tile
    val adjacentTiles: List<BoardTile> = adjacentBoardTiles(tile) + BoardTile(tile, listOf(tile))
    val groupedTiles: List<TileId> = adjacentTiles.flatMap { t -> t.group }.distinct()

    // construct an updated BaseBoard
    val array = Array(maxX(), { x -> Array(maxY(),{ y ->
        // only need to update tiles that previous existed AND the new tile
        val oldBoardTile = tiles[x][y]
        when (oldBoardTile) {
            null -> if (x == xy.x && y == xy.y) BoardTile(tile, groupedTiles)
                    else null
            else -> if (groupedTiles.contains(oldBoardTile.tile)) BoardTile(oldBoardTile.tile, groupedTiles)
                    else oldBoardTile
        }
    })})
    return BaseBoard(array)
}

/* Find all board tiles adjacent to tile. */
fun BaseBoard.adjacentBoardTiles(tile: TileId): List<BoardTile> {
    val adjTiles = mutableListOf<BoardTile>()
    val xy = tile.toXY()

    // up
    var x = xy.x - 1
    var y = xy.y
    if (x >= 0 && tiles[x][y] != null) {
        adjTiles += tiles[x][y]!!
    }

    // down
    x = xy.x + 1
    y = xy.y
    if (x < maxX() && tiles[x][y] != null) {
        adjTiles += tiles[x][y]!!
    }

    // left
    x = xy.x
    y = xy.y - 1
    if (y >= 0 && tiles[x][y] != null) {
        adjTiles += tiles[x][y]!!
    }

    // right
    x = xy.x
    y = xy.y + 1
    if (y < maxY() && tiles[x][y] != null) {
        adjTiles += tiles[x][y]!!
    }

    return adjTiles
}