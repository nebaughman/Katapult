package net.nyhm.katapult.mod

import com.github.mustachejava.DefaultMustacheFactory
import com.google.inject.Inject
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.plugin.rendering.template.JavalinMustache
import net.nyhm.katapult.KatapultModule

data class MustacheSpec(
    val templatePath: String = "/template",
    val routePaths: Iterable<String>
)

/**
 * Serve mustache templates.
 * Template files are to be stored in resources under the given templatePath.
 * The list of routes must match the internal structure of templatePath.
 * Template files must have ".mustache" extension.
 *
 * For example, if a route is "some/endpoint/file", then there should be
 * "resources/template/some/endpoint/file.mustache". This will be served
 * when "some/endpoint/file" is visited (get request).
 *
 * Notice: This is very particular and not well tested. Like everything,
 * it's for demonstration and experimentation... but even more so in this case.
 */
class MustacheModule @Inject constructor(val spec: MustacheSpec): KatapultModule {
  override fun config(app: Javalin) {
    JavalinMustache.configure(DefaultMustacheFactory())
    spec.routePaths.forEach {
      val path = "${spec.templatePath}/$it.mustache" // hacky
      app.routes {
        ApiBuilder.get(it) { ctx -> ctx.render(path) }
      }
    }
  }
}