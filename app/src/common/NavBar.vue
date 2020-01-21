<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <href to="/" class="navbar-brand">Katapult</href>

    <ul v-if="user" class="navbar-nav">
      <li class="nav-item">
        <href to="/user" name="User" class="nav-link"/>
      </li>
    </ul>

    <ul v-if="isAdmin" class="navbar-nav">
      <li class="nav-item">
        <href to="/admin" name="Admin" class="nav-link"/>
      </li>
      <li class="nav-item">
        <href to="/admin/other" class="nav-link"><small>Other</small></href>
      </li>
    </ul>

    <LoginLink class="ml-auto text-muted"/>
  </nav>
</template>

<script>
  import LoginLink from "./LoginLink"
  import {State} from "@/state/State"
  import Href from "@/util/Href"

  export default {
    name: "NavBar",
    components: {LoginLink, Href},

    computed: {
      user() {
        return State.login.user
      },

      isAdmin() {
        return State.login.isAdmin
      },
    },
  }
</script>

<style scoped>
</style>