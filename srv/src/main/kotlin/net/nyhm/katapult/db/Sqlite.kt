package net.nyhm.katapult.db

import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.ModuleSpec
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.sql.Connection

/**
 * Initializes a SQLite db
 */
class SqliteModule(val dataDir: File): KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    val url = "jdbc:sqlite:${dataDir}/data.sqlite" // TODO: parameterize db name
    Database.connect(url, "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // sqlite requires serializable isolation level
    // https://github.com/JetBrains/Exposed/wiki/FAQ
  }
}
//
// TODO: More general db module with url & driver params