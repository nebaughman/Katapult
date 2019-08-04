package net.nyhm.katapult.example

import net.nyhm.katapult.KatapultModule
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.sql.Connection

data class SqliteSpec(
    val dbFile: File
)

// TODO: Define a DbDriver interface that any DB-based module should depend on.
//   This module would provide a DbDriver instance.
//   It wouldn't actually do anything, except define the dependency,
//   so that this module would have to be started prior to those needing it.
//   But for that to work, modules need to declare what dependencies they provide.
/**
 * Initializes a SQLite db for Exposed ORM/DAO.
 * Exposed uses a single global configuration.
 * Modules that utilize Exposed-based data classes have an invisible dependency on this module.
 */
class SqliteModule(val spec: SqliteSpec): KatapultModule {
  init {
    val url = "jdbc:sqlite:${spec.dbFile}"
    Database.connect(url, "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // sqlite requires serializable isolation level
    // https://github.com/JetBrains/Exposed/wiki/FAQ
  }
}
//
// TODO: More general db module with url & driver params