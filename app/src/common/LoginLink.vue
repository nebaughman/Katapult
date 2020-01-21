<template>
  <a v-if="user" class="navbar-link" href="/logout">
    Logout ({{ user.name }})
  </a>
  <a v-else class="navbar-link" href="/login">
    Login
  </a>
</template>

<script>
  import {State} from "@/state/State"

  export default {
    name: "LoginLink",

    computed: {
      user() {
        return State.login.user
      },
    },
  }
</script>

<style scoped>
</style>