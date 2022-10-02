package net.nyhm.katapult.mod

import io.javalin.core.JavalinConfig
import io.javalin.http.staticfiles.Location
import net.nyhm.katapult.KatapultModule

data class StaticFilesSpec(
    val paths: Iterable<String>
)

/**
 * This module enables static file serving
 */
open class StaticFilesModule(
  val spec: StaticFilesSpec,
  val location: Location,
): KatapultModule {
  override fun config(config: JavalinConfig) {
    spec.paths.forEach { config.addStaticFiles(it, location) }
  }
}