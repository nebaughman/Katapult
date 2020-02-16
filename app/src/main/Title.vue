<template>
  <div class="jumbotron p-5">
    <h3 class="display-4">
      <span class="title">Katapult</span>
      <small class="text-muted font-italic subtitle pl-2">Fire at will!</small>
    </h3>
    <p class="lead">
      <a href="https://kotlinlang.org/">Kotlin</a>-oriented
      single-jar API+Web app starter kit using
      <a href="https://javalin.io">Javalin</a> and
      <a href="https://cli.vuejs.org/">Vue-Cli</a>
    </p>
    <hr class="my-4">

    <Section title="Web App">
      <fa-icon icon="far:hand-point-right"/> Vue, Vue-Cli, Vue-Router, TypeScript, Bootstrap, FontAwesome, Moment, Axios
    </Section>

    <Section title="API Server">
      <fa-icon icon="far:hand-point-right"/> Kotlin, Javalin, Exposed, Guice, Clikt, Postgres|SQLite
    </Section>

    <p class="text-center text-muted mt-5">See project README for details</p>
  </div>
</template>

<script>
  import Section from "./Section"

  export default {
    components: { Section },
  }
</script>

<style scoped>
  .title {
    font-weight: bold;
  }

  .subtitle {
    font-size: 24px;
    font-family: serif;
  }
</style>