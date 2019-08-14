package net.nyhm.katapult.example

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import net.nyhm.katapult.*
import org.jetbrains.exposed.sql.transactions.transaction

class AdminModule: KatapultModule {

  private val routes = {

    path("/api/admin") {
      get("users") { it.process(::users) }
      post("user") { it.process(::newUser) }
      delete("user") { it.process(::removeUser) }
      post("passwd") { it.process(::passwd) }
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

  fun users(userDao: UserDao) = userDao.getUsers()

  fun newUser(@Body data: NewUserData, userDao: UserDao) {
    userDao.create(UserData(data.name, Auth.hash(data.pass), data.role))
  }

  fun passwd(@Body data: PasswdData, userDao: UserDao) {
    transaction {
      userDao.findName(data.user)?.let { it.pass = Auth.hash(data.pass) } ?: BadRequestResponse()
    }
  }

  fun removeUser(@Body data: RemoveUserData, userDao: UserDao) {
    userDao.remove(data.name)
  }
}

data class PasswdData(
    val user: String,
    val pass: String
)

data class NewUserData(
    val name: String,
    val pass: String,
    val role: UserRole
)

data class RemoveUserData(
    val name: String
)
