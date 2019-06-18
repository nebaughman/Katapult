package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
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

class Katapult(
    private vararg val modules: KatapultModule
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
    val app = Javalin.create { config ->
      val server = Server() // allow modules to manipulate the Jetty server instance
      config.showJavalinBanner = false
      modules.forEach { it.config(config) }
      modules.forEach { it.config(server) }
      config.server { server } // set the (potentially customized) Jetty server
    }
    modules.forEach { it.config(app) }
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