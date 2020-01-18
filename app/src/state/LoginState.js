import Vue from "vue"
import {Api} from "./Api"
import {Log} from "@/util/Log"

export const LoginState = new Vue({
  data: {
    user: null,
  },

  computed: {
    /**
     * Whether the logged in user (if any) is an admin
     */
    isAdmin() {
      return this.user && this.user.role === "admin"
    },
  },

  methods: {
    updateUser() {
      Api.getLogin().then(user => {
        this.user = user
      }).catch(error => {
        Log.error(error)
      })
    },

    async logout() {
      await Api.logout()
      this.user = null
    },

    /**
     * Provides a Promise of the user object (or null if not logged in).
     * If already logged in, the Promise is immediately resolved.
     * If not, the current user is fetched (and recorded).
     */
    async awaitUser() {
      if (!this.user) {
        const user = await Api.getLogin()
        this.user = user
      }
      return this.user
    },
  },

  created() {
    this.updateUser()
  },
})
