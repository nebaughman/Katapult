package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import org.eclipse.jetty.server.Server
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf

/**
 * A module has the opportunity to configure Javalin.
 * Each of the [config] methods is called once per module.
 * Each has a no-op default implementation, if not needed.
 *
 * In addition, Katapult module implementations must have exactly one constructor.
 * The constructor parameters (dependencies) will be injected at startup.
 */
interface KatapultModule {
  /**
   * Configure the [JavalinConfig].
   * Do not use [JavalinConfig.server]; instead use the `config(Server)` method.
   */
  fun config(config: JavalinConfig) {}

  /**
   * Configure the underlying Jetty [Server].
   */
  fun config(server: Server) {}

  /**
   * Configure the [Javalin] app instance.
   * This is where to add routes, for example.
   */
  fun config(app: Javalin) {}
}

// TODO: KatapultFactory<T> with method to report KClass it produces and create(): T method.
//  Allows an instance to be created for each request of the dependent type, and/or to
//  postpone construction of the dependent type until requested (even if a single instance).
//  Also further abstracts the creation of config data from the dependency system.
//
//  Notice, then, that KatapultModules could just as well be created via factory; and
//  probably should. So, the dependency system is just based on factories. But,
//  KatapultModules are still important, because they take part in the service lifecycle.

/**
 * Construct a Katapult server instance with a set of configuration data
 * and list of modules to start. Modules will have their dependencies
 * (other modules and data) injected upon startup.
 *
 * Module dependencies are determined by class type (either other modules
 * or config data). Dependencies must exist and must not be ambiguous.
 */
class Katapult(
    private val config: List<Any>,
    private val modules: List<KClass<out KatapultModule>>
) {
  /**
   * The active Javalin server instance
   */
  private var app: Javalin? = null

  /**
   * Katapult can be started only once
   */
  fun start() {
    if (app != null) throw IllegalStateException("Already started") // only once
    val mods = ModuleResolver(config, modules).resolve()
    val app = Javalin.create { config ->
      config.showJavalinBanner = false
      val server = Server() // allow modules to manipulate the Jetty server instance
      mods.forEach { it.config(config) }
      mods.forEach { it.config(server) }
      config.server { server } // set the (potentially customized) Jetty server
    }
    mods.forEach { it.config(app) }
    app.start() // http(s) connector(s) specif(ies|y) port(s)
    this.app = app // prior exception will not set app
  }

  /**
   * Stop the Javalin app
   */
  fun stop() {
    app?.stop()
    //app = null // TODO: Define whether ok for modules to be re-initialized (if start() called again)
  }
}

// TODO: Improve exception types & messages (more specific)

internal class ModuleResolver(
    private val data: List<Any>,
    private val modules: List<KClass<out KatapultModule>>
) {
  fun resolve(): List<KatapultModule> {

    // TODO: Consider not allowing KatapultModule instances in data

    duplicates(data.map { it::class }).takeIf { it.isNotEmpty() }?.let {
      throw KatapultException("Duplicate data: $it")
    }

    duplicates(modules).takeIf { it.isNotEmpty() }?.let {
      throw KatapultException("Duplicate modules: $it")
    }

    mutableListOf<KClass<*>>().apply {
      addAll(data.map { it::class })
      addAll(modules)
    }.let {
      duplicates(it)
    }.takeIf { it.isNotEmpty() }?.let {
      throw KatapultException("Duplicate data class in modules list: $it")
    }

    val unresolved = modules.toMutableList()
    val resolved = mutableListOf<KatapultModule>()

    var halt = false

    while (!halt && unresolved.isNotEmpty()) {
      halt = true
      val iter = unresolved.listIterator()
      while (iter.hasNext()) {
        val it = iter.next()
        // TODO: or allow injecting non-module objects?
        val obj = it.objectInstance
        if (obj != null) {
          resolved.add(obj)
          iter.remove() // resolved
          halt = false // something new resolved, keep resolving others
        } else {
          if (it.constructors.size != 1) {
            throw KatapultException("Katapult modules must have exactly one constructor")
          }
          val c = it.constructors.first()
          // find all dependencies (parameters) of the constructor
          val deps = mutableListOf<Any?>()
          c.parameters.forEach { param ->
            //if (!isKatapultModule(param)) {
            //  throw KatapultException("Katapult module constructors may only take KatapultModule parameters")
            //}
            val type = param.type.classifier as KClass<*>
            val filter = { it:Any -> it::class.isSubclassOf(type) }
            val matches = mutableListOf<Any>().apply {
              addAll(resolved.filter(filter))
              addAll(data.filter(filter))
            }
            if (matches.size > 1) {
              throw KatapultException("Parameter fulfilled by multiple candidates: $matches")
            }
            deps.add(matches.firstOrNull()) // may add null
          }
          // if any deps are null (not found), then this module is not ready to be instantiated
          if (deps.none { d -> d == null }) {
            resolved.add(c.call(*deps.toTypedArray()))
            iter.remove() // resolved
            halt = false // something new resolved, keep resolving others
          }
        }
      }
    }

    if (unresolved.isNotEmpty()) {
      throw KatapultException("Not all dependencies resolved: $unresolved")
    }

    return resolved
  }

  companion object {
    private fun isKatapultModule(param: KParameter) =
        (param.type.classifier as KClass<*>).isSubclassOf(KatapultModule::class)

    // https://stackoverflow.com/a/47200815
    private fun duplicates(list: Iterable<*>): Set<Any> {
      return list
          .groupingBy { it }
          .eachCount()
          .filter { it.value > 1 }
          .keys
          .filterNotNullTo(mutableSetOf())
    }
  }
}

class KatapultException(message: String, cause: Throwable? = null): Exception(message, cause)