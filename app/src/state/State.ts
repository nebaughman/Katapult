import Vue from "vue"
import {Component} from "vue-property-decorator"
import {LoginState} from "./LoginState"
import {AdminState} from "./AdminState"

@Component
class Store extends Vue {
  public readonly login = new LoginState()
  public readonly admin = new AdminState()
}

/**
 * Instead of Vuex, export a singleton shared state instance, made up of simple TypeScript classes,
 * which may also use "@Component ... extends Vue" to take advantage of computed getters, watchers,
 * and ensure reactivity within other Vue components.
 *
 * Singleton application State instance
 */
export const State = new Store()
