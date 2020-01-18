import axios from "axios"
import {Log} from "@/util/Log"

// TODO: TypeScript with proper classes (Api interface, AxiosApi impl, MockApi impl)

export const Api = Object.freeze({

  async call(path, args = null, method = (args ? "post" : "get")) {
    const conf = {
      method,
      url: path,
      data: args,
    }
    const response = await axios(conf)
    // TODO: record call metrics
    return response.data
  },

  async getLogin() {
    return await this.call("/api/auth/login")
  },

  async login(user, pass) {
    return await this.call("/api/auth/login", { user, pass })
  },

  async logout() {
    return await this.call("/api/auth/logout")
  },

  async getUsers() {
    return await this.call("/api/admin/users")
  },

  async addUser(name, pass, role) {
    return await this.call("/api/admin/user", { name, pass, role })
  },

  async removeUser(name) {
    return await this.call("/api/admin/user", { name }, "delete")
  },

  async passwd(user, pass) {
    return await this.call("/api/admin/passwd", { user, pass })
  },

  async stats() {
    return await this.call("/api/admin/stats")
  },

})