<template>
  <div class="mb-4 pl-2 border-left">
    <h5 v-if="title" class="card-title">{{ title }}</h5>
    <slot/>
  </div>
</template>

<script>
  export default {
    props: {
      title: {
        type: String,
        required: true,
      },
    },
  }
</script>

<style scoped>
</style>