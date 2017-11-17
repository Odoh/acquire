package us.whodag.acquire.req

/**
 * A response to a request.
 *
 * @property returnValue whether the request succeeded.
 * @property message a description of the action taken.
 */
data class Response(val returnValue: Boolean, val message: String)

/** Whether this is a successful response. */
fun Response.isSuccess(): Boolean = returnValue

/** Whether this is a failed response. */
fun Response.isFailure(): Boolean = !returnValue

/**
 * Factory for creating responses.
 */
object Responses {

    /** Create a failed response with the specified message. */
    fun FAILURE(message: String): Response = Response(false, message)

    /** Create a successful response with the specified message. */
    fun SUCCESS(message: String): Response = Response(true, message)
}