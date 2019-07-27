import Vue from "vue"
import {LoginState} from "./LoginState"
import {Api} from "./Api"
import {Log} from "@/util/Log"

export const AdminState = new Vue({
  data: {
    users: [],
  },

  computed: {
    user() {
      return LoginState.user
    },

    isAdmin() {
      return LoginState.isAdmin
    },
  },

  methods: {
    getUsers() {
      Api.getUsers().then(users => {
        this.users = users
      }).catch(error => {
        Log.error(error)
      })
    },

    addUser(name, pass, role) {
      Api.addUser(name, pass, role).then(response => {
        this.getUsers()
      }).catch(error => {
        Log.error(error)
      })
    },

    removeUser(name) {
      Api.removeUser(name).then(response => {
        this.getUsers()
      }).catch(error => {
        Log.error(error)
      })
    },
  },

  watch: {
    isAdmin(admin) {
      if (admin) {
        this.getUsers()
      } else {
        this.users = []
      }
    },
  },
})
