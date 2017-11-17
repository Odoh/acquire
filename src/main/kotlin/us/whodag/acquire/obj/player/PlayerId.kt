package us.whodag.acquire.obj.player

/**
 * Unique identifier for a Player.
 */
data class PlayerId(val name: String): Comparable<PlayerId> {
    override fun compareTo(other: PlayerId): Int = this.name.compareTo(other.name)
    override fun toString(): String = name
}