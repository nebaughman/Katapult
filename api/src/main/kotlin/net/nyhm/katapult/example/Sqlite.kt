package net.nyhm.katapult.example

import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.ModuleSpec
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.sql.Connection

/**
 * Initializes a SQLite db
 */
class SqliteModule(val dbFile: File): KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    val url = "jdbc:sqlite:${dbFile}"
    Database.connect(url, "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // sqlite requires serializable isolation level
    // https://github.com/JetBrains/Exposed/wiki/FAQ
  }
}
//
// TODO: More general db module with url & driver params