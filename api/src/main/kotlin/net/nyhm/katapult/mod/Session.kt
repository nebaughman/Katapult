package net.nyhm.katapult.mod

import com.google.inject.Inject
import io.javalin.core.JavalinConfig
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.info
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import java.io.File

data class SessionSpec(
  val dataDir: File? = null,
  val timeoutSeconds: Int? = null
) {
  val empty = dataDir == null && timeoutSeconds == null
}

/**
 * Optionally persists http sessions in files in dataDir/sessions/
 * Sessions and data must be [Serializable]
 *
 * Optionally set a session timeout
 */
class SessionModule @Inject constructor(private val spec: SessionSpec): KatapultModule {
  override fun config(config: JavalinConfig) {

    if (spec.empty) return // no custom session handler

    val sessionHandler = SessionHandler()

    // https://javalin.io/tutorials/jetty-session-handling-kotlin
    if (spec.dataDir != null) {
      val sessionDir = File(spec.dataDir, "sessions").apply { mkdir() }
      info { "Saving sessions to: ${sessionDir.absolutePath}" }
      sessionHandler.sessionCache = DefaultSessionCache(sessionHandler).apply { // attach a cache to the handler
        sessionDataStore = FileSessionDataStore().apply { // attach a store to the cache
          this.storeDir = sessionDir
        }
      }
    }

    // TODO: Timeout needs to be tested; simple localhost testing seemed to work,
    //  but "production" deployment had mixed results
    if (spec.timeoutSeconds != null) {
      info { "Session timeout ${spec.timeoutSeconds} seconds" }
      sessionHandler.maxInactiveInterval = spec.timeoutSeconds // maxInactiveInterval in seconds
    }

    config.sessionHandler { sessionHandler } // config Javalin with custom handler
  }
}