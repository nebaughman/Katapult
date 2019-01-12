<template>
  <div>
    <Navbar class="mb-4"/>
    <div id="app" class="container">
      <InfoTitle/>
    </div>
  </div>
</template>

<script>
  import Navbar from "../../common/Navbar"
  import InfoTitle from "./InfoTitle"

  export default {
    name: "Main",
    components: {Navbar,InfoTitle},
  }
</script>

<style scoped>
</style>
