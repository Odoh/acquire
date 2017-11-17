package us.whodag.acquire.json

import com.eclipsesource.json.*

/**
 * Exception thrown for any unexpected condition working with JSON.
 *
 * @property message description of the unexpected condition.
 */
class JsonException(override val message: String) : Exception()

/**
 * Parse string into a JsonValue.
 *
 * @param string the input string
 */
fun parseJson(string: String): JsonValue =
        try {
            Json.parse(string)
        } catch (e: ParseException) {
            throw JsonException("Invalid JSON: $string")
        }

/** Convert a JsonValue into a JsonObject. */
fun JsonValue.toObject():JsonObject =
        try {
            asObject()
        } catch (e: UnsupportedOperationException) {
            throw JsonException("JSON is not an object: ${toString()}")
        }

/** Convert a JsonValue into a JsonObject. */
fun JsonValue.toArray(): JsonArray =
        try {
            asArray()
        } catch (e: UnsupportedOperationException) {
            throw JsonException("JSON is not an array: ${toString()}")
        }

/** Convert a JsonValue into an int. */
fun JsonValue.toInt(): Int =
        try {
            asInt()
        } catch (e: UnsupportedOperationException) {
            throw JsonException("JSON is not a number: ${toString()}")
        }

/** Convert a JsonValue into a long. */
fun JsonValue.toLong(): Long =
        try {
            asLong()
        } catch (e: UnsupportedOperationException) {
            throw JsonException("JSON is not a number: ${toString()}")
        }

/** Convert a JsonValue into a string. */
fun JsonValue.toStr(): String =
        try {
            asString()
        } catch (e: UnsupportedOperationException) {
            throw JsonException("JSON is not a string: ${toString()}")
        }

/** Extract a field from a JsonObject and return its JsonValue. */
fun JsonObject.field(name: String): JsonValue {
    return get(name) ?: throw JsonException("JSON object does not have field [$name]: ${toString()}")
}
