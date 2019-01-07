package net.nyhm.katapult

import io.javalin.Context
import java.io.Serializable

fun Context.session(): UserSession = UserSession.get(this)

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