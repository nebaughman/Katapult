package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.Handler
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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

/* // TODO: Redesign Endpoints;
  Modules configure routes using Javalin route methods, telling Endpoints to process:

  MyModule(val endpoints: Endpoints): KatapultModule {
    override fun config(app: Javalin) {
      app.routes({
        path("/api/admin") {
          get("users") { endpoints.process(ctx, GetUsers::class) }
          get("other") { ctx.process(endpoints, GetUsers::class) } // any better?
          // ...
        }
      })
    }
  }

  Module does not have to know/have/handle the endpoint's dependencies.
  Endpoints resolves dependencies, instantiates, then calls invoke(..).
  Cache the Endpoint instance for each class type (singletons).
  May need to allow Endpoint to indicate that it should be non-singleton (@Disposable annotation?).

  Also introspect endpoint's invoke(ctx: Context, data: MyData) method.
  It must take the context. It may require another data object.
  If so, get the type and call ctx.body(type), pass into invoke.
  This just avoids the endpoint having to call ctx.body<MyData>() itself.
  Could also allow Endpoints.process() to supply mock data, for example.

*/

// TODO: Usage is sloppy; need to separate endpoint building/resolving/registering
//
class Endpoints {

  enum class Method { BEFORE, GET, POST, PUT, DELETE, AFTER }

  private data class Entry(
      val method: Method,
      val path: String,
      val type: KClass<out Endpoint>
  ) {
    var endpoint: Endpoint? = null
    fun process(ctx: Context) {
      val handler = endpoint ?: throw KatapultException("Endpoint not resolved $type")
      handler.invoke(ctx)?.let { ctx.json(it) }
    }
  }

  val classes get() = entries.map { it.type }

  fun resolve(endpoint: Endpoint) {
    entries.forEach {
      if (it.type == endpoint::class) { // exact match
        if (it.endpoint != null) throw KatapultException("Already resolved ${it.type}")
        it.endpoint = endpoint
      }
    }
  }

  private val entries = mutableListOf<Entry>()

  fun add(method: Method, path: String, endpoint: KClass<out Endpoint>) = apply {
    entries.add(Entry(method, path, endpoint))
  }

  fun before(path: String, endpoint: KClass<out Endpoint>) = add(Method.BEFORE, path, endpoint)
  fun get(path: String, endpoint: KClass<out Endpoint>) = add(Method.GET, path, endpoint)
  fun post(path: String, endpoint: KClass<out Endpoint>) = add(Method.POST, path, endpoint)
  fun put(path: String, endpoint: KClass<out Endpoint>) = add(Method.PUT, path, endpoint)
  fun delete(path: String, endpoint: KClass<out Endpoint>) = add(Method.DELETE, path, endpoint)
  fun after(path: String, endpoint: KClass<out Endpoint>) = add(Method.AFTER, path, endpoint)

  private data class Handle(
      val method: Method,
      val path: String,
      val handler: Handler
  )

  private val handlers = mutableListOf<Handle>()

  fun add(method: Method, path: String, handler: Handler) = apply {
    handlers.add(Handle(method, path, handler))
  }

  fun before(path: String, handler: Handler) = add(Method.BEFORE, path, handler)
  fun get(path: String, handler: Handler) = add(Method.GET, path, handler)
  fun post(path: String, handler: Handler) = add(Method.POST, path, handler)
  fun put(path: String, handler: Handler) = add(Method.PUT, path, handler)
  fun delete(path: String, handler: Handler) = add(Method.DELETE, path, handler)
  fun after(path: String, handler: Handler) = add(Method.AFTER, path, handler)

  fun register(app: Javalin) {
    handlers.forEach {
      when (it.method) {
        Method.BEFORE -> app.before(it.path, it.handler)
        Method.GET -> app.get(it.path, it.handler)
        Method.POST -> app.post(it.path, it.handler)
        Method.PUT -> app.put(it.path, it.handler)
        Method.DELETE -> app.delete(it.path, it.handler)
        Method.AFTER -> app.after(it.path, it.handler)
      }
    }
    entries.forEach {
      when (it.method) {
        Method.BEFORE -> app.before(it.path) { ctx -> it.process(ctx) }
        Method.GET -> app.get(it.path) { ctx -> it.process(ctx) }
        Method.POST -> app.post(it.path) { ctx -> it.process(ctx) }
        Method.PUT -> app.put(it.path) { ctx -> it.process(ctx) }
        Method.DELETE -> app.delete(it.path) { ctx -> it.process(ctx) }
        Method.AFTER -> app.after(it.path) { ctx -> it.process(ctx) }
      }
    }
  }

  /*
  private fun resolve(): List<Entry> {
    entries.forEach {
      it.endpoint = it.type.objectInstance
      if (it.endpoint == null && it.type.constructors.size != 1) {
        throw KatapultException("Endpoints must have exactly one constructor")
      }
    }
    val resolved = mutableListOf<Any>().apply {
      addAll(entries.map { it.endpoint }.filterNotNull()) // these endpoints are resolved
      addAll(dependencies) // these are implicitly resolved
    }
    var halt = false
    while (!halt && entries.any { it.unresolved }) {
      halt = true
      entries.filter { it.unresolved }.forEach { entry ->
        val c = entry.type.constructors.first()
        // find all dependencies (parameters) of the constructor
        val deps = mutableListOf<Any?>()
        c.parameters.forEach { param ->
          val type = param.type.classifier as KClass<*>
          val matches = resolved.filter { it::class.isSubclassOf(type) }
          if (matches.size > 1) {
            throw KatapultException("${entry.type} parameter $type fulfilled by multiple candidates: $matches")
          }
          deps.add(matches.firstOrNull()) // may add null
        }
        // if any deps are null (not found), then this module is not ready to be instantiated
        if (deps.none { it == null }) {
          resolved.add(c.call(*deps.toTypedArray()))
          halt = false // something new resolved, keep resolving others
        }
      }
    }
    if (entries.any { it.unresolved }) {
      throw KatapultException("Not all dependencies resolved: ${entries.filter { it.unresolved }}")
    }
    return entries
  }
  */
}