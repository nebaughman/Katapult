package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import org.eclipse.jetty.server.Server
import kotlin.reflect.KClass

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

typealias Modules = List<KClass<out KatapultModule>>

/**
 * Construct a Katapult server instance with a set of configuration data
 * and list of modules to start. Modules will have their dependencies
 * (other modules and data) injected upon startup.
 *
 * Module dependencies are determined by class type (either other modules
 * or config data). Dependencies must exist and must not be ambiguous.
 */
class Katapult(val modules: Modules, val injector: Injector = Injector()) {
  /**
   * The active Javalin server instance
   */
  private var app: Javalin? = null

  /**
   * Katapult can be started only once
   */
  fun start() = apply {
    if (app != null) throw IllegalStateException("Already started") // only once
    val resolver = Resolver(injector)
    // modules may be dependent on one another, resolve as group
    val mods = resolver.resolveGroup(modules.toSet()).map { it as KatapultModule }
    val app = Javalin.create { config ->
      config.showJavalinBanner = false
      val server = Server() // allow modules to manipulate the Jetty server instance
      mods.forEach { it.config(config) } // modules can config config
      mods.forEach { it.config(server) } // modules can config server
      config.server { server } // set the (potentially customized) Jetty server
    }
    // request context get Processor from app attribute
    app.attribute(Processor::class.java, Processor(resolver))
    mods.forEach { it.config(app) } // modules can config Javalin app
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

class KatapultException(message: String, cause: Throwable? = null): Exception(message, cause)
