package net.nyhm.katapult.mod

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.http.Context
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.Log

interface RequestLogger {
  fun config(app: Javalin) {}
  fun log(ctx: Context, ms: Float)
}

/**
 * Javalin can have only one request logger. If more than one is needed, use this module
 * and register RequestLogger instances here. They'll each be given the chance to log.
 */
class RequestLog: KatapultModule {

  private val loggers = mutableListOf<RequestLogger>()

  fun add(logger: RequestLogger) = loggers.add(logger)

  fun addAll(loggers: List<RequestLogger>) = this.loggers.addAll(loggers)

  override fun config(config: JavalinConfig) {
    config.requestLogger { ctx, ms ->
      loggers.forEach { it.log(ctx, ms) }
    }
  }
}

/**
 * Log endpoint access
 */
object AccessLogger: RequestLogger {
  override fun log(ctx: Context, ms: Float) {
    val status = ctx.status()
    val msg = {
      val time = if (ms > 1) ms.toInt().toString() else "<1" // "%.2f".format(ms)
      "(${time}ms) [$status] ${ctx.method()} ${ctx.path()}"
    }
    if (status >= 400) Log.warn(this, msg = msg)
    else Log.info(this, msg)
  }
}