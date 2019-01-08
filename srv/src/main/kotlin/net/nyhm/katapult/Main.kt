package net.nyhm.katapult

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection
import java.util.*

const val BRAND = "Katapult"

fun main(args: Array<String>) = Cli()
    .versionOption(
        version = Version.version,
        help = "Show version and exit",
        message = { "$BRAND v$it" }
    )
    .main(args)

/**
 * Command line interpreter
 */
class Cli: CliktCommand(
    name = BRAND,
    help = "$BRAND API/Web server"
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
  val https by option(
      "--https",
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

    // currently all the command-line options, but more command-line options
    // could be added, which are not for Katapult itself (eg, module options)
    val spec = KatapultSpec(
        port = port,
        dataDir = dataDir,
        sessionFiles = sessionFiles,
        https = https,
        httpRedirect = httpRedirect,
        httpPort = httpPort
    )

    Katapult(spec).init(::dbModule,::apiModule).start()
  }

  /**
   * Sample SQLite DB module, which initializes Users table
   */
  private fun dbModule(context: KatapultContext) {
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

  /**
   * Sample API modules
   */
  private fun apiModule(context: KatapultContext) {
    context.app.routes(AuthApi.routes)
    context.app.routes(AdminApi.routes)
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