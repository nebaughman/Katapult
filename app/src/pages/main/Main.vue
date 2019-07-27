<template>
  <div>
    <NavBar class="mb-4"/>
    <div id="app" class="container">
      <router-view>
        <Title/>
      </router-view>
    </div>
  </div>
</template>

<script>
  import NavBar from "@/common/NavBar"
  import Title from "./Title"

  export default {
    name: "Main",
    components: {NavBar,Title},
  }
</script>

<style scoped>
</style>
