<template>
  <router-link v-if="routing" :to="link" class="nav-link"><slot>{{ name }}</slot></router-link>
  <a v-else :href="link" class="nav-link"><slot>{{ name }}</slot></a>
</template>

<script>
  /**
   * Use this to generate links _if_ using multi-page/SPA hybrid server mode,
   * in which links may be intra-SPA Vue-Router links or links to other
   * top-level entry points.
   *
   * NavLink provides either a hard link or a router-link, depending on whether
   * a $router instance is active and the specified link is in the set of handled
   * routes. Make sure given links are the same format as routed links.
   *
   * This allows links to work between multi-pages (page reload) or as intra-page
   * Vue routed links.
   *
   * The content of the link may be provided in the `name` prop or slot.
   */
  export default {
    name: "NavLink",

    props: {
      name: String,
      link: String,
    },

    computed: {
      routing() {
        if (!this.$router) return false // not routing
        return this.$router.options.routes.some(r => r.path.startsWith(this.link))
      },
    },
  }
</script>

<style scoped>
</style>