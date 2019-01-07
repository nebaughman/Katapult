import Vue from "vue"
import axios from "axios"
import * as log from "loglevel"

export const Store = new Vue({
  data: {
    user: null,
    users: [],
  },

  // TODO: All api methods should return Promise with response (see LoginState)
  //
  methods: {
    updateState() {
      this.getUser()
      this.getUsers()
    },

    getUser() {
      axios.get("/api/auth/login").then(response => {
        this.user = response.data
      }).catch(error => {
        log.error(error)
        throw error
      })
    },

    getUsers() {
      axios.get("/api/admin/users").then(response => {
        this.users = response.data
      }).catch(error => {
        log.error(error)
        throw error
      })
    },

    addUser(name, pass, role) {
      axios.post("/api/admin/user", { name, pass, role }).then(response => {
        this.getUsers()
      }).catch(error => {
        log.error(error)
        throw error
      })
    },

    removeUser(name) {
      axios.delete("/api/admin/user", { data: { name } }).then(response => {
        this.getUsers()
      }).catch(error => {
        log.error(error)
        throw error
      })
    },
  },

  created() {
    this.updateState()
  },
})
