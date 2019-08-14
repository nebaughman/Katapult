package net.nyhm.katapult.mod

import com.google.inject.Inject
import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import java.io.File

data class SessionSpec(
    val dataDir: File
)

/**
 * Persists http sessions in files in dataDir/sessions/
 * Sessions and data must be [Serializable]
 */
class FileSessionHandlerModule @Inject constructor(val spec: SessionSpec): KatapultModule {
  override fun config(config: JavalinConfig) {
    config.sessionHandler { fileSessionHandler(spec.dataDir) }
  }

  /**
   * https://javalin.io/tutorials/jetty-session-handling-kotlin
   */
  private fun fileSessionHandler(dataDir: File) = SessionHandler().apply { // create the session handler
    sessionCache = DefaultSessionCache(this).apply { // attach a cache to the handler
      sessionDataStore = FileSessionDataStore().apply { // attach a store to the cache
        this.storeDir = File(dataDir, "sessions").apply { mkdir() }
      }
    }
    //maxInactiveInterval = 60*30 // seconds // TODO: arguments
  }
}