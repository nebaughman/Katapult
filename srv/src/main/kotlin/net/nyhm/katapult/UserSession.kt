package net.nyhm.katapult

import io.javalin.Context
import java.io.Serializable

fun Context.session(): UserSession = UserSession.get(this)

// TODO: This needs to be better tested; for one, if serving http & https, session does
// not seem to be stable; try something like this:
//
// - Start Katapult on 7000 http & 7001 https
// - Visit http, log in, then visit 7001 (in Chrome)
// - Stop Katapult, restart with 7000 https (note: 7000 was http)
// - Log in; log in does not fail, but after redirect (to / or /admin),
//   user is not logged in, so re-redirects back to /login
//
class UserSession private constructor(
    private val ctx: Context,
    private val data: SessionData
) {
  companion object {
    private const val KEY = "session"

    fun clear(ctx: Context) = ctx.sessionAttribute(KEY, null)

    fun get(ctx: Context) = UserSession(ctx, ctx.sessionAttribute<SessionData>(KEY)
        ?: SessionData())
  }

  fun clear() = clear(ctx)

  fun save() = ctx.sessionAttribute(KEY, data)

  fun logout() = login(null)

  fun login(user: User?) = apply { data.login = user?.name }.save()

  /**
   * Get the logged in user, null if not logged in
   */
  val user: User? get() = data.login?.let { UserDao.findName(it) }

  /**
   * Whether a user is logged in and an ADMIN
   */
  fun isAdmin() = user?.let { it.role == UserRole.ADMIN } ?: false
}

// TODO: This could be loaded twice (two calls), different values changed,
// then last one to be saved would overwrite old value of one property;
// This could happen for a single session variable, but seems like more of
// a problem when _all_ session variables are being loaded/saved in one object.
// Note that there is only one session property so far (login).
//
private class SessionData: Serializable {
  // TODO: Session based on user name is not best security practice.
  // Suppose user 'alice' was deleted (and session not cleared); if another
  // 'alice' is created, the prior 'alice' session might be restored.
  // Instead, this should probably be userId.
  /**
   * Logged in username
   */
  var login: String? = null
}