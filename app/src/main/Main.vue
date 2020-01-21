<template>
  <div>
    <nav-bar class="mb-4"/>
    <div id="app" class="container">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
  import NavBar from "@/common/NavBar"

  export default {
    components: {NavBar},
  }
</script>

<style scoped>
</style>
