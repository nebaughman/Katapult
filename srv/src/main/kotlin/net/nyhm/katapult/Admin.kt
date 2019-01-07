package net.nyhm.katapult

import io.javalin.*
import io.javalin.apibuilder.ApiBuilder.*
import org.jetbrains.exposed.sql.transactions.transaction

object AdminApi {
  /**
   * The routing, which should be added to the Javalin app's routes
   */
  val routes = {
    before("/api/admin/*", AdminFilter())
    path("/api/admin") {
      get("users") { it.process(GetUsers) }
      post("user") { it.process<NewUser>() }
      delete("user") { it.process<RemoveUser>() }
      post("passwd") { it.process<Passwd>() }
    }
    // reject by redirect for admin links (rather than api)
    before("/admin/*", AdminFilter { ctx -> ctx.redirect("/login") })
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
    val role = ctx.session().user?.role
    if (role == null || role != UserRole.ADMIN) rejectHandler(ctx)
  }
}

object GetUsers: Action {
  override fun invoke(session: UserSession) = UserDao.getUsers()
}

data class Passwd(val user: String, val pass: String): Action {
  override fun invoke(session: UserSession) = transaction {
    UserDao.findName(user)?.let { it.pass = Auth.hash(pass) } ?: BadRequestResponse()
  }
}

data class NewUser(val name: String, val pass: String, val role: UserRole): Action {
  override fun invoke(session: UserSession) =
      UserDao.create(UserData(name, Auth.hash(pass), role))
}

data class RemoveUser(val name: String): Action {
  override fun invoke(session: UserSession) = UserDao.remove(name)
}