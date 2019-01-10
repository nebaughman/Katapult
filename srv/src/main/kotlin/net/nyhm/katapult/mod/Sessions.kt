package net.nyhm.katapult.mod

import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.ModuleSpec
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import java.io.File

/**
 * Persists http sessions in files in dataDir/sessions/
 */
class FileSessionHandlerModule(val dataDir: File): KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    spec.app.sessionHandler { fileSessionHandler(dataDir) }
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