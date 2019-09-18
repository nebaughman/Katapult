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
    //  separate cli command that just initializes tables and exits
  }
}

/**
 * Bind a concrete [DbDriver] class to the injection framework for [ExposedDb] to initialize.
 */
interface DbDriver {
  fun init()
}

data class SqliteSpec(
  val dbFile: File
)

/**
 * Initializes a SQLite db for Exposed ORM/DAO.
 * Exposed uses a single global configuration.
 * Modules that utilize Exposed-based data classes have an invisible dependency on this module.
 */
class SqliteDriver @Inject constructor(private val spec: SqliteSpec): DbDriver {
  override fun init() {
    val url = "jdbc:sqlite:${spec.dbFile}"
    Database.connect(url, "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // sqlite requires serializable isolation level
    // https://github.com/JetBrains/Exposed/wiki/FAQ
  }
}

data class PostgresSpec(
  val host: String,
  val db: String,
  val user: String = "postgres",
  val pass: String = ""
)

class PostgresDriver @Inject constructor(private val spec: PostgresSpec): DbDriver {
  override fun init() {
    val url = "jdbc:postgresql://${spec.host}/${spec.db}"
    Database.connect(url, "org.postgresql.Driver", spec.user, spec.pass)
  }
}