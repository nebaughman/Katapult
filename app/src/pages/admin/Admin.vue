<template>
  <div>
    <NavBar class="mb-4"/>
    <router-view/>
  </div>
</template>

<script>
  import NavBar from "../../common/NavBar"

  export default {
    name: "Admin",
    components: {NavBar},
  }
</script>

<style scoped>
</style>