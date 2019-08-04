package net.nyhm.katapult

import io.javalin.http.Context
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters

// TODO: Instead of so much loose "convention", use @Endpoint annotation that enforces
//  these endpoint "meta" rules. Maybe @Endpoint class annotation and @Handler method,
//  plus annotation(s) for certain handler method params (see notes in Processor)
//
/**
 * Endpoint provides a convenience interface for implementing api endpoints.
 * It allows a class to encapsulate the logic for handling an api request.
 * Any return value of an Endpoint's invoke function is sent as the JSON response.
 *
 * This is a marker interface for an Endpoint class.
 * Endpoint class rules are by convention (rather than compile-time safety).
 * Endpoints classes constructed with dependency injection.
 * Must contain one `invoke` function, which may take:
 *
 *  - Zero params
 *  - One param (the request Context)
 *  - Two params (Context, request body param)
 *
 * The optional request body param will be extracted from the Context body,
 * based on its type.
 */
interface Endpoint {
  // fun invoke(): Any?
  // fun invoke(ctx: Context): Any?
  // fun invoke(ctx: Context, body: Any): Any?
}

fun Context.process(endpoint: KClass<out Endpoint>) {
  appAttribute(Processor::class.java).process(this, endpoint)
}

/**
 * Report whether this context is an AJAX request.
 * Security note: This is based on headers, which could be spoofed.
 */
fun Context.isAjax() = this.header("X-Requested-With") == "XMLHttpRequest"

class Processor(val resolver: Resolver) {

  // TODO: Cache resolved Endpoints. Only if @Singleton?
  // TODO: Can Endpoints depend on other Endpoints (or Modules?) A: No

  fun process(ctx: Context, endpoint: KClass<out Endpoint>) {
    try {
      invoke(ctx, endpoint)
    } catch (e: Exception) {
      Log.fail("Uncaught exception in endpoint $endpoint", e)
      throw e // rethrow
    }
  }

  private fun invoke(ctx: Context, endpoint: KClass<out Endpoint>) {
    val inst = if (endpoint.objectInstance != null) {
      endpoint.objectInstance!!
    } else {
      resolver.resolve(endpoint)
    }

    val fn = inst::class.functions.find { it.name == "invoke" } ?:
      throw KatapultException("Endpoint must have an invoke(..) function $endpoint")

    val params = fn.valueParameters
    val response = if (params.isEmpty()) {
      fn.call(inst)
    } else if (params.size == 1) {
      if (!(params[0].type.classifier as KClass<*>).isSubclassOf(Context::class)) {
        throw KatapultException("Endpoint invoke(..) must take Context as first param: $endpoint")
      } else {
        fn.call(inst, ctx)
      }
    } else if (params.size > 2) {
      throw KatapultException("Endpoint invoke(..) cannot take more than 2 params: $endpoint")
    } else {
      val type = params[1].type.classifier as KClass<*>
      val param = ctx.bodyAsClass(type.java)
      fn.call(inst, ctx, param)
    }

    if (response != null) ctx.json(response)

    // TODO: Above enforces specific rules about Endpoint.invoke():
    //  If one param must be Context, second must be from context body;
    //  Consider relaxing dependency injection and discovering dependencies:
    //   resolver.call(inst, "invoke", ctx)?.let { ctx.json(it) }
    //  This works, but need a way to identify the body element (@Body on param)
    //  Or to be more general, mark with @Factory and pass resolver.call() a factory;
    //   Resolver uses factory as a callback for any params annotated with @Factory

  }
}