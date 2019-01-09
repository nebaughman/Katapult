package net.nyhm.katapult

import io.javalin.Javalin

interface KatapultModule {
  fun initialize(app: Javalin)
}

class Katapult {

  private val app = createApp()

  fun init(vararg modules: KatapultModule): Katapult = apply {
    // TODO: error if already started?
    modules.forEach { it.initialize(app) }
  }

  // TODO: Make port vs no port implicit (module can set flag upon init)
  fun start(port: Int) {
    // TODO: guard starting only once
    if (port > 0) app.start(port)
    else app.start() // https connector specifies port(s)
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
    //error(404) { ctx ->
    //  Log.info(this) { "Not found [${ctx.path()}]" }
    //}
  }
}