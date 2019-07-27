package net.nyhm.katapult.example

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.Log
import net.nyhm.katapult.Endpoint
import net.nyhm.katapult.process
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.io.Serializable

data class AuthSpec(
    val allowRegistration: Boolean = true
)

/**
 * Implements a sample session-based (non-REST) authentication api.
 */
class AuthApi(val spec: AuthSpec, val userDao: UserDao): KatapultModule {

  private val routes = {
    path("/api/auth") {
      get("login") { it.process(GetLogin(userDao)) }
      post("login") { it.process(Login(userDao)) }
      get("logout") { it.authSession().logout() }
      post("passwd") { it.process(ChangePassword(userDao)) }
      if (spec.allowRegistration) post("register") { it.process(Register(userDao)) }
    }

    // handler for logout link (rather than api call)
    get("/logout") { ctx ->
      ctx.authSession().logout()
      ctx.redirect("/")
    }

    // logout before loading login page
    before("/login") { it.authSession().logout() }

    before("/*", LoginFilter)
  }

  override fun config(app: Javalin) {
    app.routes(routes)
  }
}

/**
 * This filter guards that a user is logged in.
 */
object LoginFilter: Handler {

  /**
   * Path prefixes that do not require a user to be logged in.
   */
  private val publicPrefixes = listOf(
      "/login",
      "/css",
      "/js",
      "/favicon.ico",
      "/api/auth/login"
  )

  private fun isPublic(ctx: Context) = isPublic(ctx.path())
  private fun isPublic(path: String) = publicPrefixes.any { path.startsWith(it) }

  private val handler = RedirectHandler("/login") {
    !isPublic(it) && !it.authSession().isLoggedIn()
  }

  override fun handle(ctx: Context) {
    handler.handle(ctx)
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
  fun isLoggedIn() = user != null
  fun hasRole(role: UserRole) = user?.let { it.role == role } ?: false

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
class GetLogin(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context): UserInfo? {
    val login = ctx.authSession().user?.name ?: return null
    //val userDao = ctx.appAttribute(UserDao::class.java)
    return userDao.findName(login)?.let { UserInfo(it) }
  }
}

class Login(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context): LoginResponse {
    val session = ctx.authSession()
    session.logout()
    val creds = ctx.body<Creds>()
    //val userDao = ctx.appAttribute(UserDao::class.java)
    val user = userDao.findName(creds.user) ?: throw UnauthorizedResponse("No such user")
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse("Invalid password")
    session.login(user)
    Log.info(this) { "Login ${user.name}" }
    return LoginResponse(true, UserInfo(user))
  }
}

data class ChangePasswordData(
    val oldPass: String,
    val newPass: String
)

class ChangePassword(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context) {
    val data = ctx.body<ChangePasswordData>()
    val login = ctx.authSession().user?.name ?: throw UnauthorizedResponse()
    //val userDao = ctx.appAttribute(UserDao::class.java)
    val user = userDao.findName(login) ?: throw UnauthorizedResponse()
    val creds = Creds(user.name, data.oldPass)
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse()
    transaction { user.pass = Auth.hash(data.newPass) }
  }
}

data class RegisterData(
    val name: String,
    val pass: String
)

class Register(val userDao: UserDao): Endpoint {
  override fun invoke(ctx: Context): UserInfo {
    val data = ctx.body<RegisterData>()
    if (data.name.isEmpty()) throw BadRequestResponse("Invalid user name")
    if (data.pass.isEmpty()) throw BadRequestResponse("Invalid password")
    if (userDao.findName(data.name) != null) throw BadRequestResponse("User name exists")
    val user = userDao.create(UserData(data.name, Auth.hash(data.pass), UserRole.USER))
    return UserInfo(user)
  }
}