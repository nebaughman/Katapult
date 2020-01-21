<template>
  <div>

    <ul v-if="stats" class="list-group list-group-flush">
      <li v-for="stat in stats" :key="stat.path" class="list-group-item">
        <div class="d-flex justify-content-between align-items-baseline">
          <span class="text-monospace m-0">{{ stat.path }}</span>
          <span class="text-muted"><small>{{ stat.hits }}</small></span>
        </div>
        <p class="text-muted text-monospace m-0 line-short"><small>
          Sum: {{ stat.time }} ms, {{ stat.size }} bytes
        </small></p>
        <p class="text-muted text-monospace m-0 line-short"><small>
          Avg: {{ stat.avgTime }} ms, {{ stat.avgSize }} bytes
        </small></p>
      </li>
    </ul>

    <p v-else class="text-center text-muted my-2">
      <fa-icon icon="ellipsis-h"/>
    </p>

  </div>
</template>

<script>
  import {Api} from "@/state/Api"

  export default {
    data() {
      return {
        stats: null,
        timer: null,
      }
    },

    methods: {
      update() {
        Api.stats().then(stats => {
          this.stats = stats
          this.timer = setTimeout(this.update, 15000)
        })
      },
    },

    created() {
      this.update()
    },

    destroyed() {
      if (this.timer) clearTimeout(this.timer)
      this.timer = null
    },
  }
</script>

<style scoped>
</style>