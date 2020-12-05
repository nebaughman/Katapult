package net.nyhm.katapult.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import net.nyhm.katapult.*
import net.nyhm.katapult.mod.*
import net.nyhm.pick.DiBuilder
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
      canBeFile = false
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

    val config = DiBuilder()
      .register(SpaSpec())
      .register(UsersSpec(Auth::hash))
      .register(AuthConfig(true, false))
      .register(AdminConfig(false))
      .registerSingleton(UserDao::class) { ExposedUserDao(it.get()) }
      .register(SessionSpec(
        dataDir = if (sessionFiles) dataDir else null,
        timeoutSeconds = sessionTimeout // may be null
      ))
      .register(HttpSpec(httpPort))
      .register(HttpsSpec(dataDir, httpsPort))
      .register(RedirectSpec(
        { it.scheme() == "http" && it.port() == httpPort },
        httpsPort,
        "https"
      ))
      .registerSingleton(Processor::class) { DiProcessor(it) }
      .also { di ->
        when (val db = db) { // awkward
          is SqliteOptions -> {
            di.registerSingleton(DbDriver::class) { SqliteDriver(it.get()) }
            val file = if (db.file.isAbsolute) db.file else dataDir.resolve(db.file) // TODO: test relative path
            di.register(SqliteConfig(file))
          }
          is PostgresOptions -> {
            di.registerSingleton(DbDriver::class) { PostgresDriver(it.get()) }
            di.register(PostgresConfig(db.host, db.name, db.user, db.pass))
          }
        }
      }
      .register(AccessLogger)
      .registerSingleton { ExposedDb(it.get()) }
      // register modules
      .registerSingleton { SessionModule(it.get()) }
      .registerSingleton { AuthModule(it.get()) }
      .register(AppModule)
      .registerSingleton { SpaModule(it.get()) }
      .registerSingleton { UsersModule(it.get(), it.get()) }
      .registerSingleton { AdminModule(it.get()) }
      .register(ErrorModule)
      .registerSingleton { RequestLog().apply { addAll(it.getAll()) } }
      .registerSingleton { ApiStats(it.get()) }
      .register(CorsAllOrigins)
      .also { di ->
        if (http) di.registerSingleton { HttpModule(it.get()) }
        if (https) di.registerSingleton { HttpsModule(it.get()) }
        if (http && https) di.registerSingleton { RedirectHandler(it.get()) } // if both http & https, redirect http to https port
      }
      .registerSingleton { Katapult(it.getAll(), it.get()) }
      .di

    config.get(Katapult::class).start()
  }
}

object BuildProperties {
  private val props: Properties by lazy {
    Properties().apply {
      val url = BuildProperties.javaClass.getResource("/project.properties")
      if (url != null) load(url.openStream())
    }
  }

  private fun value(key: String): String = props[key] as String? ?: ""

  val project: String get() = value("project")
  val version: String get() = value("version")

  val fullTitle: String get() = "$project v$version"
}