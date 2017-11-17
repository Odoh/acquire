package us.whodag.acquire.obj.board

import us.whodag.acquire.obj.tile.TileId

/**
 * Describes the board in Acquire.
 */
interface Board {

    /** The number of tiles which make up the height. */
    fun maxX(): Int

    /** The number of tiles which make up the width. */
    fun maxY(): Int

    /** The tiles on the board. */
    fun tiles(): Collection<TileId>

    /** The tile, if it exists, at the specified coordinates. */
    fun tile(x: Int, y: Int): TileId?

    /** Whether the board contains the specified tile. */
    fun containsTile(tile: TileId): Boolean

    /** The tiles which are adjacent to the specified tile. */
    fun adjacentTiles(tile: TileId): List<TileId>

    /** The tiles which all connected to the specified tile on the board. */
    fun connectedTiles(tile: TileId): List<TileId>

    /** Add a tile to the board. */
    fun addTile(tile: TileId): Board

    /** Return the tiles which are adjacent AND all tiles in their tile groups */
    fun adjacentAndConnectedTiles(tile: TileId): List<TileId> =
            adjacentTiles(tile).flatMap { t -> connectedTiles(t) }.distinct()
}