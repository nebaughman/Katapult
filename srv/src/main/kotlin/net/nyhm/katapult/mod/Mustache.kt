package net.nyhm.katapult.mod

import com.github.mustachejava.DefaultMustacheFactory
import io.javalin.apibuilder.ApiBuilder
import io.javalin.rendering.template.JavalinMustache
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.ModuleSpec

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
class MustacheModule(
    val templatePath: String = "/template",
    vararg val routePaths: String
): KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    JavalinMustache.configure(DefaultMustacheFactory())
    routePaths.forEach {
      val path = "$templatePath/$it.mustache" // hacky
      spec.app.routes {
        ApiBuilder.get(it) { ctx -> ctx.render(path) }
      }
    }
  }
}