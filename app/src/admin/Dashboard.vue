<template>
  <div class="container-fluid">
    <div class="card-columns">
      <card title="Users"><users/></card>
      <card title="Api Stats"><api-stats/></card>
    </div>
  </div>
</template>

<script>
  import Users from "./Users"
  import Card from "./Card"
  import ApiStats from "./ApiStats"

  export default {
    name: "Dashboard",
    components: {Users, Card, ApiStats},
  }
</script>

<style scoped>
  .card-columns {
    column-count: 2;
  }
</style>