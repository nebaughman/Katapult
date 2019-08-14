package net.nyhm.katapult.mod

import com.google.inject.Inject
import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule

data class CorsSpec(
    val origins: List<String>
)

class CorsModule @Inject constructor(val spec: CorsSpec): KatapultModule {
  override fun config(config: JavalinConfig) {
    config.enableCorsForOrigin(*spec.origins.toTypedArray())
  }
}

object CorsAllOrigins: KatapultModule {
  override fun config(config: JavalinConfig) {
    config.enableCorsForAllOrigins()
  }
}