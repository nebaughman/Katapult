package net.nyhm.katapult

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import io.javalin.Javalin
import net.nyhm.katapult.mod.*
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
  //
  // TODO: Support independent http without redirect?
  // If not, drop httpRedirect flag and just use httpPort > 0 as flag
  //
  val httpPort by option(
      "--http-port",
      help = "HTTP listen port (will redirect to secure port)"
  ).int().default(80)

  override fun run() {

    dataDir.mkdir() // TODO: mkdirs()

    val katapult = Katapult().init(
        AppModule,
        UsersModule(dataDir),
        AuthModule,
        AdminModule
    )

    if (https) {
      // TODO: This is confusing (see note in Katapult.start())
      val redirectPort = if (httpRedirect) httpPort else 0
      katapult.init(HttpsModule(dataDir, port, redirectPort))
    }

    // persist sessions in file store
    if (sessionFiles) {
      katapult.init(FileSessionHandlerModule(dataDir))
    }

    katapult.start(port)
  }
}

/**
 * Sample Users module, which initializes users table in a SQLite db
 */
class UsersModule(val dataDir: File): KatapultModule {
  override fun initialize(app: Javalin) {
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
        val pass = Auth.hash("pass")
        val admin = UserDao.create(UserData("admin", pass, UserRole.ADMIN))
        Log.info(this) { "Created user ${admin.name}" }
      }
    }
    // TODO: Store a pre-hashed/salted default password for admin; insert this into the db;
    // upon login, if admin user's hash matches this, then force pw change;
    // alternatively, add flag to user to force pw change on next login
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