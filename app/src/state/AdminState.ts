import Vue from "vue"
import {Component,Watch} from "vue-property-decorator"
import {State} from "./State"
import {Api} from "./Api"
import {Log} from "@/util/Log"

@Component
export class AdminState extends Vue {

  users: any[] = []

  user() {
    return State.login.user
  }

  isAdmin() {
    return State.login.isAdmin
  }

  async getUsers() {
    try {
      this.users = await Api.getUsers()
    } catch (error) {
      Log.error(error)
    }
  }

  addUser(name: string, pass: string, role: string) {
    Api.addUser(name, pass, role).then(response => {
      this.getUsers()
    }).catch(error => {
      Log.error(error)
    })
  }

  removeUser(name: string) {
    Api.removeUser(name).then(response => {
      this.getUsers()
    }).catch(error => {
      Log.error(error)
    })
  }

  @Watch("isAdmin")
  private watchIsAdmin(admin: boolean) {
    if (admin) {
      this.getUsers()
    } else {
      this.users = []
    }
  }

}
