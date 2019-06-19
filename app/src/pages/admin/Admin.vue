<template>
  <div>
    <Navbar class="mb-4"/>
    <router-view/>
  </div>
</template>

<script>
  import Navbar from "../../common/Navbar"

  export default {
    name: "Admin",
    components: {Navbar},
  }
</script>

<style scoped>
</style>