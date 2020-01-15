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
      /**
       * String name can be provided here (or use the <slot> to insert content)
       */
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