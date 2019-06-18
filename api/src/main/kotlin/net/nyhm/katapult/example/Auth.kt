package net.nyhm.katapult.example

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.Log
import net.nyhm.katapult.Endpoint
import net.nyhm.katapult.process
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.io.Serializable

/**
 * Implements a sample session-based (non-REST) authentication api.
 */
class AuthApi(
    private val allowRegistration: Boolean = true
): KatapultModule {

  private val routes = {
    path("/api/auth") {
      get("login") { it.process(GetLogin) }
      post("login") { it.process<Login>() }
      get("logout") { it.authSession().logout() }
      post("passwd") { it.process<ChangePassword>() }
      if (allowRegistration) post("register") { it.process<Register>() }
    }

    // handler for logout link (rather than api call)
    get("/logout") { ctx ->
      ctx.authSession().logout()
      ctx.redirect("/")
    }
  }

  override fun config(app: Javalin) {
    app.routes(routes)
    //app.attribute(AuthDao::class.java, TempUserStore)
  }
}

/**
 * Authenticated user
 */
data class AuthUser(
    val name: String,
    val role: UserRole
): Serializable {
  constructor(user: User): this(user.name, user.role)
}

/**
 * Everything stored in the session must be [Serializable]
 */
data class AuthSession(
    var user: AuthUser? = null
): Serializable {
  fun logout() { user = null }
  fun login(user: AuthUser) { this.user = user }
  fun login(user: User) { login(AuthUser(user)) }

  companion object {
    private const val AUTH_SESSION_KEY = "auth_session"

    fun from(ctx: Context): AuthSession {
      var session = ctx.sessionAttribute<AuthSession>(AUTH_SESSION_KEY)
      if (session == null) {
        session = AuthSession()
        ctx.sessionAttribute(AUTH_SESSION_KEY, session)
      }
      return session
    }
  }
}

/**
 * Convenience extension function to retrieve the [AuthSession] from a [Context]
 */
fun Context.authSession() = AuthSession.from(this)

/**
 * Represents user credentials (sent from client to log in)
 */
data class Creds(val user: String, val pass: String)

data class LoginResponse(val login: Boolean, val user: UserInfo)

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
object GetLogin: Endpoint {
  override fun invoke(ctx: Context): UserInfo? {
    val login = ctx.authSession().user?.name ?: return null
    //val authDao = ctx.appAttribute(AuthDao::class.java)
    val userDao = ctx.appAttribute(UserDao::class.java)
    return userDao.findName(login)?.let { UserInfo(it) }
  }
}

data class Login(val user: String, val pass: String): Endpoint {
  override fun invoke(ctx: Context): LoginResponse {
    val session = ctx.authSession()
    session.logout()
    val creds = Creds(user, pass)
    //val authDao = ctx.appAttribute(AuthDao::class.java)
    val userDao = ctx.appAttribute(UserDao::class.java)
    val user = userDao.findName(user) ?: throw UnauthorizedResponse("No such user")
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse("Invalid password")
    session.login(user)
    Log.info(this) { "Login ${user.name}" }
    return LoginResponse(true, UserInfo(user))
  }
}

data class ChangePassword(val oldPass: String, val newPass: String): Endpoint {
  override fun invoke(ctx: Context) {
    val login = ctx.authSession().user?.name ?: throw UnauthorizedResponse()
    //val authDao = ctx.appAttribute(AuthDao::class.java)
    val userDao = ctx.appAttribute(UserDao::class.java)
    val user = userDao.findName(login) ?: throw UnauthorizedResponse()
    val creds = Creds(user.name, oldPass)
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse()
    transaction { user.pass = Auth.hash(newPass) }
  }
}

data class Register(val name: String, val pass: String): Endpoint {
  override fun invoke(ctx: Context): UserInfo {
    if (name.isEmpty()) throw BadRequestResponse("Invalid user name")
    if (pass.isEmpty()) throw BadRequestResponse("Invalid password")
    val userDao = ctx.appAttribute(UserDao::class.java)
    if (userDao.findName(name) != null) throw BadRequestResponse("User name exists")
    val user = userDao.create(UserData(name, Auth.hash(pass), UserRole.USER))
    return UserInfo(user)
  }
}