import Vue from "vue"
import axios from "axios"
import * as log from "loglevel"

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
    updateState() {
      this.updateUser()
    },

    updateUser() {
      axios.get("/api/auth/login").then(response => {
        this.user = response.data
        return response
      }).catch(error => {
        log.error(error)
        throw error // rethrow
      })
    },

    performLogin(user, pass) {
      return axios.post('/api/auth/login', { user, pass }).then(response => {
        if (response.data.login && response.data.user) this.user = response.data.user
        return response
      }).catch(error => {
        log.error(error)
        throw error // rethrow
      })
    },
  },

  created() {
    this.updateState()
  },
})
