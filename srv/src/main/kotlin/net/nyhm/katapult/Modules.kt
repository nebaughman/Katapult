package net.nyhm.katapult

import com.github.mustachejava.DefaultMustacheFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.rendering.template.JavalinMustache
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import java.io.File

/**
 * This module enables static file serving
 */
open class StaticFilesModule(val path: String): KatapultModule {
  override fun initialize(app: Javalin) {
    app.enableStaticFiles(path)
  }
}

/**
 * Serves an embedded Vue-Cli app from resources/app
 */
object VueAppModule: StaticFilesModule("/app")
//
// TODO: Since Vue-Cli production build includes versioned bundles, consider serving
// from 'immutable' path, so Javalin sets long cache expiration:
// https://javalin.io/documentation#static-files

/**
 * Persists http sessions in files in dataDir/sessions/
 */
class FileSessionHandlerModule(val dataDir: File): KatapultModule {
  override fun initialize(app: Javalin) {
    app.sessionHandler { fileSessionHandler(dataDir) }
  }

  /**
   * https://javalin.io/tutorials/jetty-session-handling-kotlin
   */
  private fun fileSessionHandler(dataDir: File) = SessionHandler().apply { // create the session handler
    sessionCache = DefaultSessionCache(this).apply { // attach a cache to the handler
      sessionDataStore = FileSessionDataStore().apply { // attach a store to the cache
        this.storeDir = File(dataDir, "sessions").apply { mkdir() }
      }
    }
    //maxInactiveInterval = 60*30 // seconds // TODO: arguments
  }
}

/**
 * Serve mustache templates.
 * Template files are to be stored in resources under the given templatePath.
 * The list of routes must match the internal structure of templatePath.
 * Template files must have ".mustache" extension.
 *
 * For example, if a route is "some/endpoint/file", then there should be
 * "resources/template/some/endpoint/file.mustache". This will be served
 * when "some/endpoint/file" is visited (get request).
 *
 * Notice: This is very particular and not well tested. Like everything,
 * it's for demonstration and experimentation... but even more so in this case.
 */
class MustacheModule(
    val templatePath: String = "/template",
    vararg val routePaths: String
): KatapultModule {
  override fun initialize(app: Javalin) {
    JavalinMustache.configure(DefaultMustacheFactory())
    routePaths.forEach {
      val path = "$templatePath/$it.mustache" // hacky
      app.routes {
        get(it) { ctx -> ctx.render(path) }
      }
    }
  }
}