<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <a href="/" class="navbar-brand">Katapult</a>
    <a v-if="isAdmin" href="/admin" class="navbar-text">Admin</a>
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