package net.nyhm.katapult.mod

import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.ModuleSpec

class CorsModule(vararg val origins: String): KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    spec.app.enableCorsForOrigin(*origins)
  }
}

object CorsAllOrigins: KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    spec.app.enableCorsForAllOrigins()
  }
}