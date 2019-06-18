package net.nyhm.katapult.mod

import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule

/**
 * This module enables static file serving
 */
open class StaticFilesModule(val path: String): KatapultModule {
  override fun config(config: JavalinConfig) {
    config.addStaticFiles(path)
  }
}

// TODO: also addSinglePageRoot
/**
 * Serves an embedded app (Vue-Cli) from resources/app
 */
object AppModule: StaticFilesModule("/app")
//
// TODO: Since Vue-Cli production build includes versioned bundles, consider serving
// from 'immutable' path, so Javalin sets long cache expiration:
// https://javalin.io/documentation#static-files
