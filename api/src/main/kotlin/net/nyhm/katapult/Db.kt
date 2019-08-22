package net.nyhm.katapult

import com.google.inject.Inject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

interface DbDriver {
  fun init()
}

/**
 * Database-based modules should depend on [Db] to ensure database has been initialized.
 */
class Db @Inject constructor(driver: DbDriver) {

  init {
    driver.init()
  }

  fun init(table: Table) = transaction { SchemaUtils.create(table) }
}

data class SqliteSpec(
    val dbFile: File
)

/**
 * Initializes a SQLite db for Exposed ORM/DAO.
 * Exposed uses a single global configuration.
 * Modules that utilize Exposed-based data classes have an invisible dependency on this driver.
 * That is, it must be initialized prior to use.
 * To ensure the driver is initialized, add a dependency to the Guice injection system like:
 *
 *   bind(DbDriver::class.java).to(SqliteDriver::class.java)
 *
 * Also have any module that creates Exposed dao/orm bindings depend on Db.
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
    val user: String = "",
    val pass: String = ""
)

class PostgresDriver @Inject constructor(private val spec: PostgresSpec): DbDriver {
  override fun init() {
    val url = "jdbc:postgresql://${spec.host}/${spec.db}"
    Database.connect(url, "org.postgresql.Driver", spec.user, spec.pass)
  }
}