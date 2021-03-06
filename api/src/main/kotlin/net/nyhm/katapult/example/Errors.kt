package net.nyhm.katapult.example

import io.javalin.Javalin
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.Log

/**
 * Sample error handling module. This is rather experimental, and likely not suitable for production.
 */
object ErrorModule: KatapultModule {
  override fun config(app: Javalin) {

    app.exception(Exception::class.java) { e,ctx ->
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

    app.error(500) { ctx ->
      Log.warn(this) { "Internal server error [${ctx.path()}]" }
      ctx.result("Internal server error")
      //
      // TODO: Which is best practice:
      // - Set a custom response (or page content), or
      // - Send 500 with no content (browser shows its own 500 page)
    }

    app.error(401) { ctx ->
      Log.warn(this) { "Unauthorized [${ctx.path()}]" }
    }

    app.error(404) { ctx ->
      Log.info(this) { "Not found [${ctx.path()}]" }
    }
  }
}