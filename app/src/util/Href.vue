<template>
  <router-link :to="to"><slot>{{ name }}</slot></router-link>
</template>

<script>
  /**
   * Provides a link using <router-link>.
   */
  export default {
    name: "Href",

    props: {
      name: {
        type: String,
        required: false,
      },

      to: {
        type: String,
        required: true,
      },
    },
  }
</script>

<style scoped>
</style>