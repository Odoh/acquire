package us.whodag.acquire.acq

/**
 * Uniquely identifies a game of Acquire.
 */
data class AcquireId(val name: String): Comparable<AcquireId> {
    override fun compareTo(other: AcquireId): Int = this.name.compareTo(other.name)
    override fun toString(): String = name
}