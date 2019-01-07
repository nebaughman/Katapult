package net.nyhm.katapult

import io.javalin.Context

/**
 * Report whether this context is an AJAX request.
 * Security note: This is based on headers, which could be spoofed.
 */
fun Context.isAjax() = this.header("X-Requested-With") == "XMLHttpRequest"

/**
 * Deserialize the specified [Action] type from the request body's json data,
 * then invoke the action with this context.
 */
inline fun <reified T: Action> Context.process() = this.process(this.body<T>())

/**
 * Process the given [Action] with this [Context].
 * The Action is invoked with the current [UserSession].
 * Any return value from invoking the Action is serialized by JavalinJson as the response body.
 * To fail the action, throw an exception (possibly a Javalin [HttpResponseException]).
 */
fun <T: Action> Context.process(action: T) {
  action.invoke(UserSession.get(this))?.let { this.json(it) } // else no body
}

/**
 * An action is invoked with a [UserSession].
 * The action implementation must be able to be deserialized by JavalinJson.
 * Likewise, any return value must be able to be serialized by JavalinJson.
 */
interface Action {
  fun invoke(session: UserSession): Any?
}