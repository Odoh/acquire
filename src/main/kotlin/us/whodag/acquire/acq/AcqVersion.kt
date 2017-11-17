package us.whodag.acquire.acq

/**
 * The version of this Acquire game.
 *
 * @property major the major version. A difference in major version breaks compatibility.
 * @property minor the minor version. A difference in minor version does not break compatibility.
 */
data class AcqVersion(val major: Int, val minor: Int) {
    override fun toString(): String = "AcqVersion_$major.$minor"

    /** Whether this version is compatible with the other version. */
    fun isCompatible(other: AcqVersion) = major == other.major && minor >= other.minor
}