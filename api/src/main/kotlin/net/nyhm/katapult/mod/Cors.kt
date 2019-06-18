package net.nyhm.katapult.mod

import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule

class CorsModule(vararg val origins: String): KatapultModule {
  override fun config(config: JavalinConfig) {
    config.enableCorsForOrigin(*origins)
  }
}

object CorsAllOrigins: KatapultModule {
  override fun config(config: JavalinConfig) {
    config.enableCorsForAllOrigins()
  }
}