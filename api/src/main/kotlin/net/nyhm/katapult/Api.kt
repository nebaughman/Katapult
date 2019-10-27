package net.nyhm.katapult

import com.google.inject.Inject
import com.google.inject.Injector
import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

/**
 * Report whether this context indicates it is an AJAX request.
 *
 * Security note: This is based on headers sent by the client, so there is no guarantee of accuracy.
 */
fun Context.isAjax() = this.header("X-Requested-With") == "XMLHttpRequest"

// TODO: Consider whether to re-introduce passing a KClass; To identify handler method,
//   either need convention (eg, "handler" name), @Handler annotation. This seems only
//   useful for stateful (instance-based) endpoints, which don't seem useful.
//
//fun Context.process(endpoint: KClass<*>) {
//  appAttribute(Processor::class.java).process(this, endpoint)
//}

/**
 * Resolve and process the endpoint with this request Context
 * (using the internally configured [Processor]).
 */
fun Context.process(endpoint: KFunction<*>) {
  appAttribute(Processor::class.java).process(this, endpoint)
}

/**
 * A processor resolves an endpoint function to handle a request Context.
 */
interface Processor {
  fun process(ctx: Context, endpoint: KFunction<*>)
}

/**
 * A [Processor] that uses an [Injector] to resolve dependencies.
 */
class InjectedProcessor @Inject constructor(@Inject val injector: Injector): Processor {

  override fun process(ctx: Context, endpoint: KFunction<*>) {

    val deps = mutableListOf<Any>()
    endpoint.valueParameters.forEach {
      val paramClass = it.type.classifier as KClass<*>
      if (paramClass == Context::class) deps.add(ctx)
      else if (isBodyParam(it)) deps.add(ctx.bodyAsClass(paramClass.java))
      else deps.add(injector.getInstance(paramClass.java))
    }

    try {
      val result = endpoint.call(*deps.toTypedArray())
      if (result != null) ctx.json(result)
    } catch (e:InvocationTargetException) {
      if (e.cause is HttpResponseException) throw e.cause as HttpResponseException
      else throw e
    }
  }

  private fun isBodyParam(param: KParameter) = param.findAnnotation<Body>() != null
}

/**
 * Annotate an endpoint handler function parameter with @Body to have the
 * request Context body parsed and injected.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Body