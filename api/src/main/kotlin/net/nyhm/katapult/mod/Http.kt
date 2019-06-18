package net.nyhm.katapult.mod

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.apibuilder.ApiBuilder
import net.nyhm.katapult.KatapultModule
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import java.net.URI

/**
 * Adds an HTTP server listening port
 */
class HttpModule(val port: Int = 80): KatapultModule {
  override fun config(server: Server) {
    server.addConnector(
        ServerConnector(server).also { it.port = port }
    )
  }
}

/**
 * Adds a Javalin redirect router, which will issue a redirect to the same url at different
 * port and scheme. Use this, for example, to redirect HTTP traffic to HTTPS port.
 */
class RedirectModule(
    val predicate: (Context) -> Boolean,
    val targetPort: Int,
    val targetScheme: String = "http"
): KatapultModule {
  override fun config(app: Javalin) {
    app.routes {
      ApiBuilder.before { ctx ->
        if (predicate.invoke(ctx)) {
          val url = URI(ctx.url()).let {
            URI(
                targetScheme,
                it.userInfo,
                it.host,
                targetPort,
                it.path,
                it.query,
                it.fragment
            )
          }.toURL().toExternalForm()
          ctx.redirect(url)
        }
      }
    }
  }
}
