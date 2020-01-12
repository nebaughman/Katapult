package net.nyhm.katapult.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.google.inject.AbstractModule
import com.google.inject.Guice
import net.nyhm.katapult.*
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

    val config = object : AbstractModule() {
      override fun configure() {
        bind(SpaSpec::class.java).toInstance(SpaSpec())
        bind(UsersSpec::class.java).toInstance(UsersSpec(Auth::hash))
        bind(AuthSpec::class.java).toInstance(AuthSpec(true))

        bind(UserDao::class.java).to(ExposedUserDao::class.java)

        bind(DbDriver::class.java).to(SqliteDriver::class.java)
        bind(SqliteSpec::class.java).toInstance(SqliteSpec(File(dataDir, "data.sqlite")))

        //bind(DbDriver::class.java).to(PostgresDriver::class.java)
        //bind(PostgresSpec::class.java).toInstance(PostgresSpec("localhost", "katapult", "postgres", "postgres"))
        // TODO: db settings from command-line (or env)

        bind(SessionSpec::class.java).toInstance(SessionSpec(dataDir))
        bind(HttpSpec::class.java).toInstance(HttpSpec(httpPort))
        bind(HttpsSpec::class.java).toInstance(HttpsSpec(dataDir, httpsPort))
        bind(RedirectSpec::class.java).toInstance(RedirectSpec(
            { it.scheme() == "http" && it.port() == httpPort },
            httpsPort,
            "https"
        ))
        bind(Processor::class.java).to(InjectedProcessor::class.java)
      }
    }

    val modules = mutableListOf(
        AuthModule::class,
        AppModule::class,
        SpaModule::class,
        UsersModule::class,
        AdminModule::class,
        ErrorModule::class,
        RequestLog::class
    )

    // persist sessions in file store
    if (sessionFiles) modules.add(FileSessionHandlerModule::class)

    if (http) modules.add(HttpModule::class)

    if (https) modules.add(HttpsModule::class)

    // if both http & https, redirect http to https port
    if (http && https) modules.add(RedirectModule::class)

    val injector = Guice.createInjector(config)

    Katapult(modules, injector).start()
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