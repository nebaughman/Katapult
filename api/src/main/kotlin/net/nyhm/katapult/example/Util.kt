package net.nyhm.katapult.example

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse

/**
 * Javalin [Handler] that invokes the given [action] if [precondition] is met.
 */
open class ConditionalHandler(
    val precondition: (ctx: Context) -> Boolean,
    val action: (ctx: Context) -> Unit
): Handler {
  override fun handle(ctx: Context) {
    if (precondition(ctx)) action(ctx)
  }
}

/**
 * [ConditionalHandler] that issues a redirect to the given path if the [precondition] is met.
 */
class RedirectHandler(
    path: String,
    precondition: (ctx: Context) -> Boolean = { true }
): ConditionalHandler(precondition, { it.redirect(path) })

/**
 * [ConditionalHandler] that throws an [UnauthorizedResponse] if the [precondition] is met.
 */
class UnauthorizedHandler(
    precondition: (ctx: Context) -> Boolean = { true }
): ConditionalHandler(precondition, { throw UnauthorizedResponse() })