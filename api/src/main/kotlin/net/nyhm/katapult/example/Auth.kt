package net.nyhm.katapult.example

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.*
import net.nyhm.katapult.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.io.Serializable

data class AuthConfig(
  /**
   * Provide an api endpiont for new user registration
   */
  val allowRegistration: Boolean = true,

  /**
   * Whether to check requests for login status and redirect to the login page if not.
   * Some resources do not require login (hard-coded below).
   *
   * Do not use this if server does not need to guard endpoints. For instance,
   * running an SPA, in which the server is either a pure API service or only serves
   * the main page content with no need to guard other paths. In these cases, everything
   * is bundled in the app (even the admin pages), and the api methods must guard access
   * to the api endpoints. The client-side routing should turn non-admins away from
   * admin pages, but this is just for user experience.
   *
   * Note: Presently only using SPA mode. Will experiment with other bundling schemes
   * again later. For example:
   *
   *   - Login in a separate endpoint & bundle, so non-logged-in page hits can be
   *     server-redirected to the login page and not incur full app download (if you
   *     don't want to serve any content unless logged in).
   *
   *   - Admin in a separate endpoint & bundle, only served if user is already
   *     logged in and an admin. This prevents exposing the admin pages to non-admins,
   *     which might help obscure the admin api endpoints. However, the real security
   *     is guarding the endpoints. Maybe more importantly, non-admins would not need to
   *     download the admin bundle, saving some bandwidth and page load time.
   */
  val guardPathAccess: Boolean = true
)

class AuthModule(private val config: AuthConfig): KatapultModule {

  private val routes = {

    path("/api/auth") {
      get("login") { it.process(::getLogin) }
      post("login") { it.process(::login) }
      get("logout") { it.authSession().logout() }
      post("passwd") { it.process(::changePassword) }
      if (config.allowRegistration) post("register") { it.process(::register) }
    }

    if (config.guardPathAccess) {
      before("/*", LoginFilter)

      // logout page request (not api)
      get("/logout") {
        it.authSession().logout()
        it.redirect("/login")
      }

      before("/login") { it.authSession().logout() }
    }
  }

  override fun config(app: Javalin) {
    app.routes(routes)
  }

  fun login(ctx: Context, @Body creds: Creds, userDao: UserDao): LoginResponse {
    val session = ctx.authSession()
    session.logout()
    val user = userDao.findName(creds.user) ?: throw UnauthorizedResponse("No such user")
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse("Invalid password")
    session.login(user)
    info { "Login ${user.name}" }
    return LoginResponse(true, UserInfo(user))
  }

  /**
   * Get the currently logged-in user info (null if no user logged in)
   */
  fun getLogin(ctx: Context, userDao: UserDao): UserInfo? {
    val login = ctx.authSession().user?.name ?: return null // TODO: allow AuthSession to be injected
    return userDao.findName(login)?.let { UserInfo(it) }
  }

  fun changePassword(ctx: Context, @Body data: ChangePasswordData, userDao: UserDao) {
    val login = ctx.authSession().user?.name ?: throw UnauthorizedResponse()
    val user = userDao.findName(login) ?: throw UnauthorizedResponse()
    val creds = Creds(user.name, data.oldPass)
    if (!Auth.verify(user, creds)) throw UnauthorizedResponse()
    transaction { user.pass = Auth.hash(data.newPass) }
  }

  fun register(@Body data: RegisterData, userDao: UserDao): UserInfo {
    if (!config.allowRegistration) throw MethodNotAllowedResponse("Registration not allowed", emptyMap())
    if (data.name.isEmpty()) throw BadRequestResponse("Invalid user name")
    if (data.pass.isEmpty()) throw BadRequestResponse("Invalid password")
    if (userDao.findName(data.name) != null) throw BadRequestResponse("User name exists")
    val user = userDao.create(UserData(data.name, Auth.hash(data.pass), UserRole.USER))
    return UserInfo(user)
  }
}

data class ChangePasswordData(
    val oldPass: String,
    val newPass: String
)
data class RegisterData(
    val name: String,
    val pass: String
)

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