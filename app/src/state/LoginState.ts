import {Api} from "./Api"
import {Log} from "@/util/Log"

export class LoginState {

  user: any = null

  /**
   * Whether the logged in user (if any) is an admin
   */
  get isAdmin() {
    return this.user && this.user.role === "admin"
  }

  async updateUser() {
    try {
      this.user = Api.getLogin()
    } catch (error) {
      Log.error(error)
    }
  }

  async logout() {
    await Api.logout()
    this.user = null
  }

  /**
   * Provides a Promise of the user object (or null if not logged in).
   * If already logged in, the Promise is immediately resolved.
   * If not, the current user is fetched (and recorded).
   * Do not use this method if you only need to _react_ to the user property,
   * rather than triggering the current login to be fetched.
   */
  async awaitUser() {
    if (!this.user) {
      const user = await Api.getLogin()
      this.user = user
    }
    return this.user
  }

  created() {
    // do not do this here, else api could be called before axios is configured
    //this.updateUser()
  }
}
