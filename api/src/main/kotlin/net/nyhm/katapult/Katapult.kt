package net.nyhm.katapult

import com.google.inject.Guice
import com.google.inject.Injector
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import org.eclipse.jetty.server.Server
import kotlin.reflect.KClass

/**
 * A module has the opportunity to configure Javalin.
 * Each of the [config] methods is called once per module.
 * Each has a no-op default implementation, if not needed.
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
 * Construct a Katapult server instance with a set of modules to start.
 * The [Injector] should be configured with any external dependencies that
 * cannot be just-in-time resolved by the injector. (See Guice documentation
 * to understand what that means.)
 */
class Katapult(val modules: Modules, val injector: Injector = Guice.createInjector()) {
  /**
   * The active Javalin server instance
   */
  private var app: Javalin? = null

  /**
   * Katapult can be started only once
   */
  fun start() = apply {
    if (app != null) throw IllegalStateException("Already started") // only once
    val mods = modules.map {
      if (it.objectInstance != null) it.objectInstance!!
      else injector.getInstance(it.java)
    }
    val app = Javalin.create { config ->
      config.showJavalinBanner = false
      val server = Server() // allow modules to manipulate the Jetty server instance
      mods.forEach { it.config(config) } // modules can config config
      mods.forEach { it.config(server) } // modules can config server
      config.server { server } // set the (potentially customized) Jetty server
    }
    // request context Processor stored as attribute
    app.attribute(Processor::class.java, InjectedProcessor(injector))
    mods.forEach { it.config(app) } // modules can config Javalin app
    app.start() // http(s) connector(s) specif(y|ies) port(s)
    this.app = app // prior exception will not set app
  }

  /**
   * Stop the Javalin server
   */
  fun stop() {
    app?.stop()
    //app = null // TODO: Define whether ok for modules to be re-initialized (if start() called again)
  }
}

class KatapultException(message: String, cause: Throwable? = null): Exception(message, cause)
