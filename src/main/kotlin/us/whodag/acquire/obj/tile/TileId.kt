package us.whodag.acquire.obj.tile

/**
 * Unique identifier for Tiles.
 *
 * @property name the name of the tile ("1-A")
 */
data class TileId(val name: String): Comparable<TileId> {
    constructor(number: String, letter: String) : this("$number-$letter")

    override fun compareTo(other: TileId): Int {
        val (thisNum, thisChar) = this.split()
        val (otherNum, otherChar) = other.split()
        if (thisNum.compareTo(otherNum) == 0) {
            return thisChar.compareTo(otherChar)
        }
        return thisNum.compareTo(otherNum)
    }
    override fun toString(): String = name
}

/** Split the name of a tile into its number of letter components. */
fun TileId.split(): Pair<Int, Char> {
    val split = name.split("-")
    return Pair(split[0].toInt(), split[1].toCharArray()[0])
}