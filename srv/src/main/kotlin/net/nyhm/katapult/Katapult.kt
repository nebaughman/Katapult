package net.nyhm.katapult

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.before
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URI
import java.sql.Connection
import java.util.*

fun main(args: Array<String>) = Katapult()
    .versionOption(
        version = Version.version,
        help = "Show version and exit",
        message = { "Katapult v$it" }
    )
    .main(args)

/**
 * Katapult command line processor
 */
class Katapult: CliktCommand(
    help = "Katapult API/Web server"
) {

  val port by option(
      "--port",
      help = "API/Web server port"
  ).int().default(7000)

  val dataDir by option(
      "--data-dir",
      help = "Data directory"
  ).file(
      fileOkay = false
  ).default(File(System.getProperty("user.dir"), "data"))

  val sessionFiles by option(
      "--session-files",
      help = "Store sessions in files"
  ).flag("--no-session-files")

  /**
   * Enable HTTPS connections. Set httpRedirect and httpPort to listen
   * for HTTP (on httpPort) and redirect to HTTPS on the secure port.
   */
  val secure by option(
      "--secure",
      help = "Enable HTTPS (only)"
  ).flag()

  val httpRedirect by option(
      "--http-redirect",
      help = "Enable HTTP redirect to secure port"
  ).flag("--no-http-redirect")

  val httpPort by option(
      "--http-port",
      help = "HTTP listen port (will redirect to secure port)"
  ).int().default(80)

  override fun run() {
    initDb()
    initApp()
  }

  private fun initDb() {
    val url = "jdbc:sqlite:${dataDir}/data.sqlite" // TODO: parameterize db url & driver
    Database.connect(url, "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // only sqlite requires serializable isolation level
    // https://github.com/JetBrains/Exposed/wiki/FAQ

    transaction {
      SchemaUtils.create(Users)
    }

    transaction {
      // create admin user with initial default password
      if (UserDao.findName("admin") == null) {
        val pass = Auth.hash("pass") // TODO: force change on initial login (if this matches)
        val admin = UserDao.create(UserData("admin", pass, UserRole.ADMIN))
        Log.info(this) { "Created user ${admin.name}" }
      }
    }
  }

  private fun initApp() {
    val app = Javalin.create().apply {
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
    if (secure && httpRedirect) {
      app.routes(httpRedirect(port))
    }

    app.routes(AuthApi.routes)
    app.routes(AdminApi.routes)

    // persist sessions in file store
    if (sessionFiles) {
      app.sessionHandler { fileSessionHandler(dataDir) }
    }

    if (secure) {
      app.server {
        Server().apply {
          addConnector(
            ServerConnector(this, sslContextFactory()).also {
              it.port = port // 443 for standard HTTPS
            }
          )
          if (httpRedirect) {
            addConnector(
              ServerConnector(this).also { it.port = httpPort }
            )
          }
        }
      }
      app.start()
    } else {
      app.start(port)
    }
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
        File(dataDir, "fullchain.pem"),
        File(dataDir, "privkey.pem"),
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

/**
 * Contains the build version
 */
object Version {
  val version: String by lazy {
    val props = Properties()
    props.load(javaClass.getResource("/version.properties").openStream())
    props["version"] as String
  }
}