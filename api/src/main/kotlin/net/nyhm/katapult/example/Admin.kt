package net.nyhm.katapult.example

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import net.nyhm.katapult.*
import org.jetbrains.exposed.sql.transactions.transaction

class AdminModule: KatapultModule {

  private val routes = {

    path("/api/admin") {
      get("users") { it.process(GetUsers::class) }
      post("user") { it.process(NewUser::class) }
      delete("user") { it.process(RemoveUser::class) }
      post("passwd") { it.process(Passwd::class) }
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
  @EndpointHandler
  fun invoke() = userDao.getUsers()
}

data class PasswdData(
    val user: String,
    val pass: String
)

class Passwd(val userDao: UserDao): Endpoint {
  @EndpointHandler
  fun invoke(@Body data: PasswdData) {
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
  @EndpointHandler
  fun invoke(@Body data: NewUserData) {
    userDao.create(UserData(data.name, Auth.hash(data.pass), data.role))
  }
}

data class RemoveUserData(
    val name: String
)

class RemoveUser(val userDao: UserDao): Endpoint {
  @EndpointHandler
  fun invoke(@Body data: RemoveUserData) {
    userDao.remove(data.name)
  }
}