package net.nyhm.katapult.mod

import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.*
import net.nyhm.katapult.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object AuthModule: KatapultModule {
  override fun initialize(spec: ModuleSpec) {
    spec.app.routes(AuthApi.routes)
  }
}

object AuthApi {
  val routes = {
    path("/api/auth") {
      get("login") { it.process(GetLogin) }
      post("login") { it.process<Login>() }
      get("logout") { it.process(Logout) }
      post("passwd") { it.process<ChangePassword>() }
    }
    // handler for logout link (rather than api call)
    get("/logout") { ctx ->
      UserSession.get(ctx).logout()
      ctx.redirect("/login")
    }
  }
}

/**
 * Represents user credentials (sent from client to log in)
 */
data class Creds(val user: String, val pass: String)

data class LoginResponse(val login: Boolean, val user: String, val redirect: String)

/**
 * Authentication utilities
 */
object Auth {
  /**
   * Verify that the given user matches the given credentials
   */
  fun verify(user: User, creds: Creds): Boolean {
    return user.name == creds.user && BCrypt.checkpw(creds.pass, user.pass)
  }

  /**
   * Utility method to perform password hashing.
   */
  fun hash(pass: String): String = BCrypt.hashpw(pass, BCrypt.gensalt())
}

/**
 * Get the currently logged-in user info (null if no logged in user)
 */
object GetLogin: Action {
  override fun invoke(session: UserSession) = session.user
}

object Logout: Action {
  override fun invoke(session: UserSession) = session.logout()
  //if (!ctx.isAjax()) ctx.redirect("/login") // only if not ajax call
}

data class Login(val user: String, val pass: String): Action {
  override fun invoke(session: UserSession): Any? {
    session.logout()
    val creds = Creds(user, pass)
    val user = UserDao.findName(creds.user) ?: throw UnauthorizedResponse("No such user")
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse("Invalid password")
    session.login(user)
    val redirect = if (user.role == UserRole.ADMIN) "/admin" else "/"
    Log.info(this) { "Login ${user.name}" }
    return LoginResponse(true, user.name, redirect)
  }
}

data class ChangePassword(val oldPass: String, val newPass: String): Action {
  override fun invoke(session: UserSession) = transaction {
    val user = session.user ?: throw UnauthorizedResponse()
    val creds = Creds(user.name, oldPass)
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse()
    user.pass = Auth.hash(newPass)
  }
}