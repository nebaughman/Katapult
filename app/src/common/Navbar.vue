<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <a href="/" class="navbar-brand">Katapult</a>

    <template v-if="isAdmin">
      <ul class="navbar-nav">
        <li class="nav-item">
          <!--<a href="/admin" class="nav-link">Admin</a>-->
          <a href="/admin" class="nav-link">Admin</a>
          <!--<router-link to="/admin" class="nav-link">Admin</router-link>-->
        </li>
        <li class="nav-item">
          <!--<a href="/admin/other" class="nav-link"><small>Other</small></a>-->
          <a href="/admin/other" class="nav-link"><small>Other</small></a>
          <!--<router-link to="/admin/other" class="nav-link"><small>Other</small></router-link>-->
        </li>
      </ul>
    </template>

    <LoginLink class="ml-auto text-muted"/>
  </nav>
</template>

<script>
  import LoginLink from "./LoginLink"
  import {LoginState} from "../common/LoginState"

  export default {
    name: "Navbar",
    components: {LoginLink},

    computed: {
      isAdmin() {
        return LoginState.isAdmin
      },
    },
  }
</script>

<style scoped>
</style>