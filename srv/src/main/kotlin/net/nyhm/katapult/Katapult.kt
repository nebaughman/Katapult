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

class Katapult {

  /**
   * Allow modules to manipulate the Jetty Server instance.
   * Start with the Javalin default server configuration.
   */
  private val server = JettyServerUtil.defaultServer()

  private val app = createApp()

  // TODO: Make this the constructor; start() performs initialization process
  fun init(vararg modules: KatapultModule): Katapult = apply {
    // TODO: error if already started?
    val spec = ModuleSpec(app, server)
    modules.forEach { it.initialize(spec) }
    app.server { server }
  }

  fun start() {
    // TODO: guard starting only once
    app.start() // http(s) connector(s) specif(ies|y) port(s)
  }

  fun stop() {
    app.stop()
  }

  private fun createApp() = Javalin.create().apply {
    disableStartupBanner()

    // TODO: Consider modularizing exception and error handling (there won't be much left here!)

    exception(Exception::class.java) { e,ctx ->
      Log.fail(this, e)
      ctx.status(500)
      //throw InternalServerErrorResponse()
      //
      // TODO: Not sure of intent in Javalin exception handling:
      // - Could set ctx.status(500) instead of exception.
      // - ctx.status(500) triggers error handler, which can return custom message/page.
      // - Throwing exception here short-circuits before error handler.
      //   (browser shows its own 500 Server Error page).
      // - Likewise, if setting ctx.status(500) here, but have no 500 error handler,
      //   browser will show its own 500 Server Error page.
    }

    error(500) { ctx ->
      Log.info(this) { "Internal server error [${ctx.path()}]" }
      ctx.result("Internal server error")
      // TODO: Which is best practice:
      // - Set a custom response (or page content), or
      // - Send 500 with no content (browser shows its own 500 page)
    }

    error(401) { ctx ->
      Log.warn(this) { "Unauthorized [${ctx.path()}]" }
    }

    //error(404) { ctx ->
    //  Log.info(this) { "Not found [${ctx.path()}]" }
    //}
  }
}