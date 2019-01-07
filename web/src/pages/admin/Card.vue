<template>
  <div class="card">
    <!--
    <div v-if="title" class="card-header">
      {{ title }}
    </div>
    -->
    <div class="card-body p-3">
      <h5 v-if="title" class="card-title">{{ title }}</h5>
      <slot/>
    </div>
  </div>
</template>

<script>
  export default {
    name: "Card",

    props: {
      title: '',
    },
  }
</script>

<style scoped>
</style>