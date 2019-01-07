<template>
  <button class="btn btn-sm btn-outline-danger" @mouseleave="cancel" @click="click" :disabled="disabled">
    <span v-if="confirming">{{ confirmMessage }}</span>
    <fa-icon v-else icon="times"/>
  </button>
</template>

<script>
  export default {
    name: "ConfirmButton",

    data() {
      return {
        confirming: false,
      }
    },

    props: {
      confirmMessage: {
        type: String,
        default: "Confirm"
      },

      disabled: {
        type: Boolean,
        default: false,
      },
    },

    methods: {
      click() {
        if (this.confirming) this.$emit('click')
        this.confirming = !this.confirming
      },

      cancel() {
        this.confirming = false
      },
    },
  }
</script>

<style scoped>
</style>