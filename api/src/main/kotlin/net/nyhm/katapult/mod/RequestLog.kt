package net.nyhm.katapult.mod

import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.Log

object RequestLog: KatapultModule {
  override fun config(config: JavalinConfig) {
    config.requestLogger { ctx, ms -> Log.info(this) { ctx.path() } }
  }
}