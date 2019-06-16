package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.core.util.JettyServerUtil
import org.eclipse.jetty.server.Server

data class ModuleSpec(
    val app: Javalin,
    val server: Server
)

interface KatapultModule {
  /**
   * Called once per module, before the server starts.
   * To augment the internal Jetty Server, use the given server instance.
   * Do not replace the app's server (ie, do not call app.server(..)).
   * Post-initialization, this server instance will be set into the Javalin app.
   */
  fun initialize(spec: ModuleSpec)
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

    // Allow modules to manipulate the Jetty Server instance.
    // Start with the Javalin default server configuration.
    val server = JettyServerUtil.defaultServer()
    val app = Javalin.create().apply { disableStartupBanner() }
    val spec = ModuleSpec(app, server)
    modules.forEach { it.initialize(spec) }
    app.server { server } // replace Jetty server
    app.start() // http(s) connector(s) specif(ies|y) port(s)
    this.app = app // prior exception will not set app
  }

  fun stop() {
    app?.stop()
    //app = null // TODO: Define whether ok for modules to be re-initialized (if start() called again)
  }
}