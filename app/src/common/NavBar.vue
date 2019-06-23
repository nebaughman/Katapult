<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <a href="/" class="navbar-brand">Katapult</a>

    <template v-if="isAdmin">
      <ul class="navbar-nav">
        <li class="nav-item">
          <nav-link link="/admin" name="Admin"/>
        </li>
        <li class="nav-item">
          <nav-link link="/admin/other"><small>Other</small></nav-link>
        </li>
      </ul>
    </template>

    <LoginLink class="ml-auto text-muted"/>
  </nav>
</template>

<script>
  import LoginLink from "./LoginLink"
  import NavLink from "./NavLink"
  import {LoginState} from "../common/LoginState"

  export default {
    name: "NavBar",
    components: {LoginLink, NavLink},

    computed: {
      isAdmin() {
        return LoginState.isAdmin
      },
    },
  }
</script>

<style scoped>
</style>