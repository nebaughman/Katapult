package net.nyhm.katapult.mod

import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.ModuleSpec

/**
 * This module enables static file serving
 */
open class StaticFilesModule(val path: String): KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    spec.app.enableStaticFiles(path)
  }
}

/**
 * Serves an embedded app (Vue-Cli) from resources/app
 */
object AppModule: StaticFilesModule("/app")
//
// TODO: Since Vue-Cli production build includes versioned bundles, consider serving
// from 'immutable' path, so Javalin sets long cache expiration:
// https://javalin.io/documentation#static-files
