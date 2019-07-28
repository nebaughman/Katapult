package net.nyhm.katapult.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import net.nyhm.katapult.Endpoints
import net.nyhm.katapult.Katapult
import net.nyhm.katapult.Log
import net.nyhm.katapult.mod.*
import java.io.File
import java.util.*

fun main(args: Array<String>) = Cli()
    .versionOption(
        version = BuildProperties.version,
        help = "Show version and exit",
        message = { BuildProperties.fullTitle }
    )
    .main(args)

/**
 * Command line interpreter
 */
class Cli: CliktCommand(
    name = BuildProperties.project,
    help = "${BuildProperties.project} API/Web server"
) {

  /**
   * If HTTP and HTTPS are enabled, HTTP will be redirected to the HTTPS port.
   * (Otherwise, experienced session issues serving both HTTP and HTTPS.)
   */
  val httpPort by option(
      "--httpPort",
      help = "HTTP listen port (0 to disable)"
  ).int().default(0) // TODO: production 80, dev 7080

  /*
  val httpRedirect by option(
      "--http-redirect",
      help = "Redirect HTTP to HTTPS port"
  ).flag("--no-http-redirect")
  */

  val httpsPort by option(
      "--httpsPort",
      help = "HTTPS listen port (0 to disable)"
  ).int().default(0) // TODO: production 443, dev 7443

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

  val http: Boolean get() { return httpPort > 0 }
  val https: Boolean get() { return httpsPort > 0 }

  override fun run() {

    Log.info(this) { "Starting ${BuildProperties.fullTitle}" }

    dataDir.mkdir() // TODO: mkdirs()

    val config = mutableListOf(
        SpaSpec(listOf()),
        SqliteSpec(File(dataDir, "data.sqlite")),
        UsersSpec(Auth::hash),
        AuthSpec(true),
        ExposedUserDao
    )

    val endpoints = Endpoints()
        .before("/*", LoginFilter)
        .get("/logout", Logout::class)
        .after("/logout", LoginRedirect::class)
        .before("/login", Logout::class)
        .get("/api/auth/login", GetLogin::class)
        .post("/api/auth/login", Login::class)
        .get("/api/auth/logout", Logout::class)
        .post("/api/auth/passwd", ChangePassword::class)
        .post("/api/auth/register", Register::class)

    val modules = mutableListOf(
        AppModule::class,
        SpaModule::class,
        SqliteModule::class,
        UsersModule::class,
        AdminModule::class,
        SampleErrorModule::class,
        RequestLog::class
    )

    // persist sessions in file store
    if (sessionFiles) {
      config.add(SessionSpec(dataDir))
      modules.add(FileSessionHandlerModule::class)
    }

    if (http) {
      config.add(HttpSpec(httpPort))
      modules.add(HttpModule::class)
    }

    if (https) {
      config.add(HttpsSpec(dataDir, httpsPort))
      modules.add(HttpsModule::class)
    }

    // if both http & https, redirect http to https port
    if (http && https) {
      config.add(RedirectSpec(
          { it.scheme() == "http" && it.port() == httpPort },
          httpsPort,
          "https"
      ))
      modules.add(RedirectModule::class)
    }

    Katapult(config, endpoints, modules).start()
  }
}

object BuildProperties {
  private val props: Properties by lazy {
    Properties().apply {
      load(BuildProperties.javaClass.getResource("/project.properties").openStream())
    }
  }

  private fun value(key: String): String = props[key] as String? ?: ""

  val project: String get() = value("project")
  val version: String get() = value("version")

  val fullTitle: String get() = "$project v$version"
}