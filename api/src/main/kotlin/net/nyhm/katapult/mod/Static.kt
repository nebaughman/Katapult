package net.nyhm.katapult.mod

import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule

/**
 * This module enables static file serving
 */
open class StaticFilesModule(vararg val paths: String): KatapultModule {
  override fun config(config: JavalinConfig) {
    paths.forEach { config.addStaticFiles(it) }
  }
}