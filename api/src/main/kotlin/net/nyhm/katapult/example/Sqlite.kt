package net.nyhm.katapult.example

import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.sql.Connection

data class SqliteSpec(
    val dbFile: File
)

/**
 * Initializes a SQLite db
 */
class SqliteModule(val spec: SqliteSpec): KatapultModule {
  override fun config(config: JavalinConfig) {
    val url = "jdbc:sqlite:${spec.dbFile}"
    Database.connect(url, "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // sqlite requires serializable isolation level
    // https://github.com/JetBrains/Exposed/wiki/FAQ
  }
}
//
// TODO: More general db module with url & driver params