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
  },

  created() {
    this.updateUser()
  },
})
