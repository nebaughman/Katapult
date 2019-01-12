<template>
  <div>
    <Navbar class="mb-4"/>
    <div class="container-fluid">
      <div class="card-columns">
        <Card title="Users"><Users/></Card>
      </div>
    </div>
  </div>
</template>

<script>
  import Navbar from "../../common/Navbar"
  import Users from "./Users"
  import Card from "./Card"

  export default {
    name: "Admin",
    components: {Navbar, Users, Card},
  }
</script>

<style scoped>
</style>