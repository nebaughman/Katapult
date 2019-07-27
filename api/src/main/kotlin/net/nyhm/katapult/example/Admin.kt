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

class AdminModule(private val userDao: UserDao): KatapultModule {

  private val routes = {

    path("/api/admin") {
      get("users") { it.process(GetUsers(userDao)) }
      post("user") { it.process(NewUser(userDao)) }
      delete("user") { it.process(RemoveUser(userDao)) }
      post("passwd") { it.process(Passwd(userDao)) }
    }

    val adminApiFilter = UnauthorizedHandler { !it.authSession().hasRole(UserRole.ADMIN) }
    before("/api/admin/*", adminApiFilter)

    // reject by redirect for admin links (rather than api)
    val adminFilter = RedirectHandler("/login") { !it.authSession().hasRole(UserRole.ADMIN) }
    before("/admin", adminFilter)
    before("/admin/*", adminFilter)
  }

  override fun config(app: Javalin) {
    app.routes(routes)
  }
}

class GetUsers(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context) = userDao.getUsers()
}

data class PasswdData(
    val user: String,
    val pass: String
)

class Passwd(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context) {
    val data = ctx.body<PasswdData>()
    transaction {
      userDao.findName(data.user)?.let { it.pass = Auth.hash(data.pass) } ?: BadRequestResponse()
    }
  }
}

data class NewUserData(
    val name: String,
    val pass: String,
    val role: UserRole
)

class NewUser(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context) {
    val data = ctx.body<NewUserData>()
    userDao.create(UserData(data.name, Auth.hash(data.pass), data.role))
  }
}

data class RemoveData(
    val name: String
)

class RemoveUser(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context) {
    val data = ctx.body<RemoveData>()
    userDao.remove(data.name)
  }
}