import axios from "axios"
//import {Log} from "@/util/Log"

// TODO: TypeScript with proper classes (Api interface, AxiosApi impl, MockApi impl)

async function call(path: string, args: any = null, method = (args ? "post" : "get")) {
  const conf = {
    method,
    url: path,
    data: args,
  }
  const response = await axios(conf)
  // TODO: record call metrics
  return response.data
}

export class Api {

  static async getLogin() {
    return await call("/api/auth/login")
  }

  static async login(user: string, pass: string) {
    return await call("/api/auth/login", { user, pass })
  }

  static async logout() {
    return await call("/api/auth/logout")
  }

  static async getUsers(): Promise<any> {
    return await this.call("/api/admin/users")
  }

  static async addUser(name: string, pass: string, role: string) {
    return await call("/api/admin/user", { name, pass, role })
  }

  static async removeUser(name: string) {
    return await call("/api/admin/user", { name }, "delete")
  }

  static async passwd(user: string, pass: string) {
    return await call("/api/admin/passwd", { user, pass })
  }

  static async stats() {
    return await call("/api/admin/stats")
  }

}