package net.nyhm.katapult.mod

import com.google.inject.Inject
import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule

data class StaticFilesSpec(
    val paths: Iterable<String>
)

/**
 * This module enables static file serving
 */
open class StaticFilesModule @Inject constructor(val spec: StaticFilesSpec): KatapultModule {
  override fun config(config: JavalinConfig) {
    spec.paths.forEach { config.addStaticFiles(it) }
  }
}