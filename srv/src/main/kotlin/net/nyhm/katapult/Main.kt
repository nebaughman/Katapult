package net.nyhm.katapult

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
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

    dataDir.mkdir() // TODO: mkdirs()

    val katapult = Katapult().init(
        AppModule,
        UsersModule(dataDir),
        AuthModule,
        AdminModule
    )

    if (http) katapult.init(HttpModule(httpPort))

    // if both http & https, redirect http to https port
    if (http && https) {
      katapult.init(RedirectModule(
          { it.scheme() == "http" && it.port() == httpPort },
          httpsPort, "https"
      ))
    }

    if (https) katapult.init(HttpsModule(dataDir, httpsPort))

    // persist sessions in file store
    if (sessionFiles) katapult.init(FileSessionHandlerModule(dataDir))

    katapult.start()
  }
}

/**
 * Sample Users module, which initializes users table in a SQLite db
 */
class UsersModule(val dataDir: File): KatapultModule {
  override fun initialize(spec: ModuleSpec) {
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