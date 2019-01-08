package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.before
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.io.File
import java.net.URI
import java.util.*

data class KatapultSpec(
    val port: Int,
    val dataDir: File,
    val sessionFiles: Boolean,
    val https: Boolean,
    val httpRedirect: Boolean,
    val httpPort: Int
)

data class KatapultContext(
    val spec: KatapultSpec,
    val app: Javalin
)

class Katapult(val spec: KatapultSpec) {

  private val app = createApp()

  fun init(vararg modules: (KatapultContext) -> Unit): Katapult = apply {
    // TODO: error if already started?
    val context = KatapultContext(spec, app)
    modules.forEach { it.invoke(context) }
  }

  fun start() {
    // TODO: guard starting only once
    if (spec.https) app.start() // https connector specifies port(s)
    else app.start(spec.port)
  }

  fun stop() {
    app.stop()
  }

  private fun createApp(): Javalin {
    spec.dataDir.mkdir()

    val app = Javalin.create()

    app.apply {
      disableStartupBanner()

      // serve bundled vue app
      enableStaticFiles("/app")
      //
      // TODO: Since Vue-Cli production build includes versioned bundles, consider serving
      // from 'immutable' path, so Javalin sets long cache expiration:
      // https://javalin.io/documentation#static-files

      exception(Exception::class.java) { e,ctx ->
        Log.fail(this, e)
        ctx.status(500)
        //throw InternalServerErrorResponse()
        //
        // TODO: Not sure of intent in Javalin exception handling:
        // - Could set ctx.status(500) instead of exception.
        // - ctx.status(500) triggers error handler, which can return custom message/page.
        // - Throwing exception here short-circuits before error handler.
        //   (browser shows its own 500 Server Error page).
        // - Likewise, if setting ctx.status(500) here, but have no 500 error handler,
        //   browser will show its own 500 Server Error page.
      }

      error(500) { ctx ->
        Log.info(this) { "Internal server error [${ctx.path()}]" }
        ctx.result("Internal server error")
        // TODO: Which is best practice:
        // - Set a custom response (or page content), or
        // - Send 500 with no content (browser shows its own 500 page)
      }
      //error(404) { ctx ->
      //  Log.info(this) { "Not found [${ctx.path()}]" }
      //}
    }

    /*
    // mustache template example
    JavalinMustache.configure(DefaultMustacheFactory())
    app.routes {
      get("test") { ctx -> ctx.render("/template/test.mustache", mapOf("name" to "Bruce"))}
    }
    */

    // if non-secure http redirect requested, rewrite url with https scheme
    if (spec.https && spec.httpRedirect) {
      app.routes(httpRedirect(spec.port))
    }

    // persist sessions in file store
    if (spec.sessionFiles) {
      app.sessionHandler { fileSessionHandler(spec.dataDir) }
    }

    if (spec.https) {
      app.server {
        Server().apply {
          addConnector(
              ServerConnector(this, sslContextFactory()).also {
                it.port = spec.port // 443 for standard HTTPS
              }
          )
          if (spec.httpRedirect) {
            addConnector(
                ServerConnector(this).also { it.port = spec.httpPort }
            )
          }
        }
      }
    }

    return app
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
    //maxInactiveInterval = 60*30 // seconds
  }

  /**
   * https://javalin.io/documentation#custom-server
   * https://github.com/tipsy/javalin/blob/master/src/test/java/io/javalin/examples/HelloWorldSecure.java
   */
  private fun sslContextFactory(): SslContextFactory {
    val sslContextFactory = SslContextFactory()
    val storePass = UUID.randomUUID().toString()
    sslContextFactory.keyStore = HttpsUtil.loadKeystore(
        File(spec.dataDir, "fullchain.pem"),
        File(spec.dataDir, "privkey.pem"),
        storePass.toCharArray()
    )
    sslContextFactory.setKeyStorePassword(storePass)
    return sslContextFactory
  }

  /**
   * Javalin redirect router, which will issue a redirect to the same url with https scheme.
   * Use this when https is configured and http redirect is enabled.
   */
  private fun httpRedirect(securePort: Int) = {
    before { ctx ->
      if (ctx.scheme() == "http") {
        val https = URI(ctx.url()).let {
          URI(
              "https",
              it.userInfo,
              it.host,
              securePort,
              it.path,
              it.query,
              it.fragment
          )
        }.toURL().toExternalForm()
        ctx.redirect(https)
      }
    }
  }
}