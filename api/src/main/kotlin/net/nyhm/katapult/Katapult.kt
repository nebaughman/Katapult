package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import net.nyhm.pick.Di
import org.eclipse.jetty.server.Server

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

/**
 * Construct a Katapult server instance with a set of modules to start.
 * The injector should be configured with any external dependencies.
 */
class Katapult(
  private val modules: List<KatapultModule>,
  private val processor: Processor
) {
  /**
   * The active Javalin server instance
   */
  private var app: Javalin? = null

  /**
   * Katapult can be started only once
   */
  fun start() = apply {
    if (app != null) throw IllegalStateException("Already started") // only once
    //val mods = di.getAll(KatapultModule::class)
    val app = Javalin.create { config ->
      config.showJavalinBanner = false
      val server = Server() // allow modules to manipulate the Jetty server instance
      modules.forEach { it.config(config) } // modules can config config
      modules.forEach { it.config(server) } // modules can config server
      config.server { server } // set the (potentially customized) Jetty server
    }
    // request context Processor stored as attribute
    app.attribute("processor", processor)
    modules.forEach { it.config(app) } // modules can config Javalin app
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
