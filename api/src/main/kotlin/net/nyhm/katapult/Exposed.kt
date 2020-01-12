package net.nyhm.katapult

import com.google.inject.Inject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

/**
 * Exposed database dao classes should depend on [ExposedDb] to ensure database has been initialized.
 */
class ExposedDb @Inject constructor(driver: DbDriver) {

  init {
    driver.init()
  }

  /**
   * Exposed database dao classes should call this method to initialize tables.
   */
  fun init(table: Table) = transaction {
    SchemaUtils.create(table)
    //SchemaUtils.createMissingTablesAndColumns(table)
    // TODO: Cli flag whether to do this or not, as well as
    //  separate cli command to initialize tables and exit
  }
}

/**
 * Bind a concrete [DbDriver] class to the injection framework for [ExposedDb] to initialize.
 */
interface DbDriver {
  fun init()
}

/**
 * Marker interface for db config
 */
interface DbConfig

data class SqliteConfig(
  val dbFile: File
): DbConfig

/**
 * Initializes a SQLite db for Exposed ORM/DAO.
 * Exposed uses a single global configuration.
 * Modules that utilize Exposed-based data classes have an invisible dependency on this module.
 */
class SqliteDriver @Inject constructor(private val config: SqliteConfig): DbDriver {
  override fun init() {
    val url = "jdbc:sqlite:${config.dbFile}"
    Database.connect(url, "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // sqlite requires serializable isolation level
    // https://github.com/JetBrains/Exposed/wiki/FAQ
  }
}

data class PostgresConfig(
  val host: String,
  val db: String,
  val user: String = "postgres",
  val pass: String = ""
): DbConfig

class PostgresDriver @Inject constructor(private val config: PostgresConfig): DbDriver {
  override fun init() {
    val url = "jdbc:postgresql://${config.host}/${config.db}"
    Database.connect(url, "org.postgresql.Driver", config.user, config.pass)
  }
}