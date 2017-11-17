package us.whodag.acquire.obj.hotel

/**
 * Unique identifier for a Hotel.
 */
data class HotelId(val name: String) : Comparable<HotelId> {
    override fun compareTo(other: HotelId): Int = this.name.compareTo(other.name)
    override fun toString(): String = name
}