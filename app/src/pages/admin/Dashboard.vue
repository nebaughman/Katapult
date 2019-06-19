<template>
  <div class="container-fluid">
    <div class="card-columns">
      <Card title="Users"><Users/></Card>
    </div>
  </div>
</template>

<script>
  import Users from "./Users"
  import Card from "./Card"

  export default {
    name: "Dashboard",
    components: {Users, Card},
  }
</script>