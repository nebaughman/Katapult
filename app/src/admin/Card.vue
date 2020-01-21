<template>
  <div class="card">
    <div class="card-body p-3">
      <h5 v-if="title" class="card-title">{{ title }}</h5>
      <slot/>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      title: {
        type: String,
        required: false,
      },
    },
  }
</script>

<style scoped>
</style>