package net.nyhm.katapult.example

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import net.nyhm.katapult.*
import net.nyhm.katapult.Endpoint
import net.nyhm.katapult.process
import org.jetbrains.exposed.sql.transactions.transaction

class AdminModule(val userDao: UserDao): KatapultModule {

  val routes = {
    before("/api/admin/*", AdminFilter())
    path("/api/admin") {
      get("users") { it.process(GetUsers(userDao)) }
      post("user") { it.process<NewUser>() }
      delete("user") { it.process<RemoveUser>() }
      post("passwd") { it.process<Passwd>() }
    }
    // reject by redirect for admin links (rather than api)
    before("/admin/*", AdminFilter { ctx -> ctx.redirect("/login") })
  }

  override fun config(app: Javalin) {
    app.routes(routes)
  }
}


/**
 * Reject non-[UserRole.ADMIN] users (or if not logged in).
 * Provide an optional reject handler (throws [UnauthorizedResponse] by default).
 */
class AdminFilter(
    val rejectHandler: (Context) -> Unit = { throw UnauthorizedResponse() }
): Handler {
  override fun handle(ctx: Context) {
    val role = ctx.authSession().user?.role
    if (role != UserRole.ADMIN) rejectHandler(ctx)
  }
}

class GetUsers(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context) = userDao.getUsers()
}

data class Passwd(val user: String, val pass: String): Endpoint {
  override fun invoke(ctx: Context) = transaction {
    UserDao.findName(user)?.let { it.pass = Auth.hash(pass) } ?: BadRequestResponse()
  }
}

data class NewUser(val name: String, val pass: String, val role: UserRole): Endpoint {
  override fun invoke(ctx: Context) =
      UserDao.create(UserData(name, Auth.hash(pass), role))
}

data class RemoveUser(val name: String): Endpoint {
  override fun invoke(ctx: Context) = UserDao.remove(name)
}