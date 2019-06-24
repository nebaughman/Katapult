<template>
  <div>
    <NavBar class="mb-4"/>
    <div id="app" class="container">
      <!--
      <router-view>
        <InfoTitle/>
      </router-view>
      -->
      <InfoTitle/>
    </div>
  </div>
</template>

<script>
  import NavBar from "../../common/NavBar"
  import InfoTitle from "./InfoTitle"

  export default {
    name: "Main",
    components: {NavBar,InfoTitle},
  }
</script>

<style scoped>
</style>
