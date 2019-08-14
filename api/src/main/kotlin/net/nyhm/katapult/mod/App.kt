package net.nyhm.katapult.mod

/**
 * Serves an embedded app (Vue-Cli) from resources/app.
 * Also see [Spa] for single-page and multi-page app support.
 */
object AppModule: StaticFilesModule(StaticFilesSpec(listOf("/app")))
//
// TODO: Since Vue-Cli production build includes versioned bundles, consider serving
// from 'immutable' path, so Javalin sets long cache expiration:
// https://javalin.io/documentation#static-files
