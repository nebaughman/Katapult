package net.nyhm.katapult.mod

import io.javalin.core.JavalinConfig

/**
 * Serves an embedded app (Vue-Cli) from resources/app
 */
object AppModule: StaticFilesModule("/app") {
  override fun config(config: JavalinConfig) {
    super.config(config)
    // TODO: Support multi-page apps by passing more than one single page root
    config.addSinglePageRoot("/", "/app/index.html")
    config.addSinglePageRoot("/admin", "/app/admin/index.html")
  }
}
//
// TODO: Since Vue-Cli production build includes versioned bundles, consider serving
// from 'immutable' path, so Javalin sets long cache expiration:
// https://javalin.io/documentation#static-files
