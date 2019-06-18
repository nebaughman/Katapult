package net.nyhm.katapult

import io.javalin.http.Context

/**
 * Endpoint provides a convenience interface for implementing api endpoints.
 * It allows a class to encapsulate the logic for handling a call to an endpoint.
 * The return value of [invoke] is sent as the JSON response.
 *
 * Endpoint implementations can be singleton objects:
 *
 * ```
 * object Hello: Endpoint {
 *   override fun invoke(ctx: Context) = "Hello"
 * }
 *
 * // when registering the Javalin endpoint handler
 * ApiBuilder.get("api/hello") { it.process(Hello) }
 * ```
 *
 * Endpoints can be classes with parameters automatically deserialized from the
 * request body, using Javalin's [Context.body], and can return objects, so long
 * as they are serializable by JavalinJson:
 *
 * ```
 * data class Response(val status: String, val load: Int, val open: Boolean)
 *
 * class Status: Endpoint {
 *   override fun invoke(ctx: Context): Response {
 *     // ... compute response values
 *     return Response(status, load, open)
 *   }
 * }
 *
 * // when registering the Javalin endpoint handler
 * ApiBuilder.get("api/status") { it.process<Status>() }
 * ```
 */
interface Endpoint {
  fun invoke(ctx: Context): Any?
}

/**
 * Deserialize the specified [Endpoint] type from the request body's json data,
 * (using [Context.body]) then invoke with this Context.
 */
inline fun <reified T: Endpoint> Context.process() = this.process(this.body<T>())

/**
 * Invoke the given [Endpoint] with this [Context].
 * Any return value is serialized by JavalinJson as the response body.
 * To fail the endpoint, throw an exception (possibly an [io.javalin.HttpResponseException]).
 */
fun <T: Endpoint> Context.process(endpoint: T) {
  endpoint.invoke(this)?.let { this.json(it) } // else no body
}

/**
 * Report whether this context is an AJAX request.
 * Security note: This is based on headers, which could be spoofed.
 */
fun Context.isAjax() = this.header("X-Requested-With") == "XMLHttpRequest"