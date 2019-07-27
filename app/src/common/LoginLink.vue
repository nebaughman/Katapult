<template>
  <a v-if="user" class="navbar-link" href="/logout">
    Logout ({{ user.name }})
  </a>
  <a v-else class="navbar-link" href="/login">
    Login
  </a>
</template>

<script>
  import {LoginState} from "@/state/LoginState"

  export default {
    name: "LoginLink",

    computed: {
      user() {
        return LoginState.user
      },
    },
  }
</script>

<style scoped>
</style>