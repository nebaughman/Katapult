package net.nyhm.katapult.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.*
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

class SqliteOptions: OptionGroup(name = "--db=sqlite options") {
  val file by option("--db-file", envvar = "SQLITE_FILE").file().required()
}

// TODO: Make these match https://www.postgresql.org/docs/current/libpq-envars.html
class PostgresOptions: OptionGroup(name = "--db=postgres options") {
  val host by option("--db-host", envvar = "PG_HOST").required()
  val name by option("--db-name", envvar = "PG_NAME").required()
  val user by option("--db-user", envvar = "PG_USER").required()
  val pass by option("--db-pass", envvar = "PG_PASS").required()
}

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
      "--http-port",
      help = "HTTP listen port (0 to disable)"
  ).int().default(80)

  val httpsPort by option(
      "--https-port",
      help = "HTTPS listen port (0 to disable)"
  ).int().default(443)

  val http: Boolean get() { return httpPort > 0 }
  val https: Boolean get() { return httpsPort > 0 }

  /*
  val httpRedirect by option(
      "--http-redirect",
      help = "Redirect HTTP to HTTPS port"
  ).flag("--no-http-redirect")
  */

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

  val sessionTimeout by option(
    "--session-timeout",
    help = "Session timeout (in seconds)"
  ).int()

  val db by option("--db", envvar = "DB_TYPE").groupChoice(
    "sqlite" to SqliteOptions(),
    "postgres" to PostgresOptions()
  ).required()

  override fun run() {

    Log.info(this) { "Starting ${BuildProperties.fullTitle}" }

    dataDir.mkdirs()

    val config = object : AbstractModule() {
      override fun configure() {
        bind(SpaSpec::class.java).toInstance(SpaSpec())
        bind(UsersSpec::class.java).toInstance(UsersSpec(Auth::hash))
        bind(AuthConfig::class.java).toInstance(AuthConfig(true, false))
        bind(AdminConfig::class.java).toInstance(AdminConfig(false))

        bind(UserDao::class.java).to(ExposedUserDao::class.java)

        when (val it = db) {
          is SqliteOptions -> {
            bind(DbDriver::class.java).to(SqliteDriver::class.java)
            val file = if (it.file.isAbsolute) it.file else dataDir.resolve(it.file) // TODO: test relative path
            bind(SqliteConfig::class.java).toInstance(SqliteConfig(file))
          }
          is PostgresOptions -> {
            bind(DbDriver::class.java).to(PostgresDriver::class.java)
            bind(PostgresConfig::class.java).toInstance(PostgresConfig(
              it.host, it.name, it.user, it.pass
            ))
          }
        }

        bind(SessionSpec::class.java).toInstance(SessionSpec(
          dataDir = if (sessionFiles) dataDir else null,
          timeoutSeconds = sessionTimeout // may be null
        ))

        bind(HttpSpec::class.java).toInstance(HttpSpec(httpPort))
        bind(HttpsSpec::class.java).toInstance(HttpsSpec(dataDir, httpsPort))
        bind(RedirectSpec::class.java).toInstance(RedirectSpec(
            { it.scheme() == "http" && it.port() == httpPort },
            httpsPort,
            "https"
        ))
        bind(Processor::class.java).to(InjectedProcessor::class.java)

        // Guice doesn't understand Kotlin object singletons, so have to give it an instance
        bind(RequestLog::class.java).toInstance(RequestLog)
      }
    }

    val modules = mutableListOf(
        SessionModule::class,
        AuthModule::class,
        AppModule::class,
        SpaModule::class,
        UsersModule::class,
        AdminModule::class,
        ErrorModule::class,
        RequestLog::class,
        ApiStats::class
    )

    if (http) modules.add(HttpModule::class)

    if (https) modules.add(HttpsModule::class)

    // if both http & https, redirect http to https port
    if (http && https) modules.add(RedirectModule::class)

    val injector = Guice.createInjector(config)

    // if there is a RequestLog module, add AccessLogger
    injector.getInstance(RequestLog::class.java)?.add(AccessLogger)
    // TODO: instead, this could/should be done via a module

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