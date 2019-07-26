package net.nyhm.katapult.mod

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.apibuilder.ApiBuilder
import net.nyhm.katapult.KatapultModule
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import java.net.URI

data class HttpSpec(
    val port: Int = 80
)

/**
 * Adds an HTTP server listening port
 */
class HttpModule(val spec: HttpSpec): KatapultModule {
  override fun config(server: Server) {
    server.addConnector(
        ServerConnector(server).also { it.port = spec.port }
    )
  }
}

data class RedirectSpec(
    val predicate: (Context) -> Boolean,
    val targetPort: Int,
    val targetScheme: String = "http"
)

/**
 * Adds a Javalin redirect router, which will issue a redirect to the same url at different
 * port and scheme. Use this, for example, to redirect HTTP traffic to HTTPS port.
 */
class RedirectModule(val spec: RedirectSpec): KatapultModule {
  override fun config(app: Javalin) {
    app.routes {
      ApiBuilder.before { ctx ->
        if (spec.predicate.invoke(ctx)) {
          val url = URI(ctx.url()).let {
            URI(
                spec.targetScheme,
                it.userInfo,
                it.host,
                spec.targetPort,
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
