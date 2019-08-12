package net.nyhm.katapult

import com.google.inject.Injector
import io.javalin.http.Context
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

/**
 * Report whether this context is an AJAX request.
 * Security note: This is based on headers, which could be spoofed.
 */
fun Context.isAjax() = this.header("X-Requested-With") == "XMLHttpRequest"

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

fun Context.process(endpoint: KClass<*>) {
  appAttribute(Processor::class.java).process(this, endpoint)
}

fun Context.process(endpoint: KFunction<*>) {
  appAttribute(Processor::class.java).process(this, endpoint)
}

@Target(AnnotationTarget.FUNCTION)
annotation class EndpointHandler // TODO: Better name that doesn't clash with Javalin Handler

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Body

class Processor(val injector: Injector) {

  // TODO: Cache resolved Endpoints. Only if @Singleton?
  // TODO: Can Endpoints depend on other Endpoints (or Modules?) A: No

  fun process(ctx: Context, endpoint: KClass<*>) {
    try {
      invoke(ctx, endpoint)
    } catch (e: Exception) {
      Log.fail("Uncaught exception in endpoint $endpoint", e)
      throw e // rethrow
    }
  }

  fun process(ctx: Context, endpoint: KFunction<*>) = invoke(ctx, endpoint) // null instance

  private fun invoke(ctx: Context, endpoint: KClass<*>) {

    val instance = if (endpoint.objectInstance != null) {
      endpoint.objectInstance!!
    } else {
      injector.getInstance(endpoint.java)
    }

    invoke(ctx, instance)
  }

  private fun invoke(ctx: Context, endpoint: Any) {

    // TODO: If endpoint has only one function, assume that one
    val handlerFn = endpoint::class.functions.find {
      it.findAnnotation<EndpointHandler>() != null
    } ?: throw KatapultException("Endpoint has no @EndpointHandler ${endpoint::class}")

    invoke(ctx, handlerFn, endpoint)
  }

  private fun invoke(ctx: Context, endpoint: KFunction<*>, instance: Any? = null) {

    val bodyParam =
      endpoint.valueParameters.find {
        it.findAnnotation<Body>() != null
      }?.let {
        ctx.bodyAsClass((it.type.classifier as KClass<*>).java)
      }

    val inject = mutableListOf<Any>().apply {
      add(ctx)
      if (bodyParam != null) add(bodyParam)
    }

    val deps = mutableListOf<Any>()
    if (endpoint.instanceParameter != null && instance == null) {
      throw IllegalArgumentException("Cannot call instance function without instance")
    }
    instance?.let { deps.add(instance) }
    endpoint.valueParameters.forEach {
      val p = it.type.classifier as KClass<*>
      inject.find { p.isSubclassOf(it::class) }?.let { deps.add(it) } ?: deps.add(injector.getInstance(p.java))
    }
    val result = endpoint.call(*deps.toTypedArray())

    if (result != null) ctx.json(result)

    /*
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
    */

    // TODO: Above enforces specific rules about Endpoint.invoke():
    //  If one param must be Context, second must be from context body;
    //  Consider relaxing dependency injection and discovering dependencies:
    //   resolver.call(inst, "invoke", ctx)?.let { ctx.json(it) }
    //  This works, but need a way to identify the body element (@Body on param)
    //  Or to be more general, mark with @Factory and pass resolver.call() a factory;
    //   Resolver uses factory as a callback for any params annotated with @Factory

  }
}