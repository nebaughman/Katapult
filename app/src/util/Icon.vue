<template>
  <font-awesome-icon :icon="faIcon" v-bind="$attrs"/>
</template>

<script>
  import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
  //Vue.component('fa-icon', FontAwesomeIcon)

  import { library } from '@fortawesome/fontawesome-svg-core'
  //import { far } from '@fortawesome/free-regular-svg-icons'
  //import { fas } from '@fortawesome/free-solid-svg-icons'
  //library.add(far)
  //library.add(fas)

  // loading only needed icons
  import {
    faTimes, faSpinner, faSignInAlt, faEllipsisH,
  } from "@fortawesome/free-solid-svg-icons"

  library.add(
    faTimes, faSpinner, faSignInAlt, faEllipsisH,
  )

  // other libraries (for example)
  //import {
  //  faCheckCircle
  //} from "@fortawesome/free-regular-svg-icons"
  //
  //library.add(
  //  faCheckCircle,
  //)

  // TODO: Instead of contaminating this component with application-specific icons,
  //  load icons in main.js (app configuration)

  /**
   * This component wraps a FontAwesomeIcon component.
   *
   * All icons used by the app must be (manually) imported and registered above.
   * This makes a _significant_ difference in the resulting (webpacked) bundle size
   * over importing full icon libraries.
   *
   * FontAwesomeIcon component supports passing effects (spin, pulse, size, etc)
   * as component properties. Icon uses v-bind="$attrs" so these properties should
   * be passed down to the font-awesome-icon component.
   *
   *   <icon icon="spinner" size="lg" pulse />
   *
   * Alternatively, to add special effects, attach FontAwesome classes, such as:
   *
   *   <icon icon="spinner" class="fa-spin fa-2x" />
   *
   * FontAwesomeIcon has an array-based naming scheme for icon libraries.
   * This is supported, or you can use a colon to separate the library from the icon name:
   *
   *   <icon :icon="['far','check-circle']" />
   *   <icon icon="far:check-circle" />
   */
  export default {

    components: {FontAwesomeIcon},

    props: {
      /**
       * Supports these formats:
       *
       *  - FontAwesome-style array: ['far','check-circle']
       *  - Colon-delimited string: "far:check-circle"
       *  - Just plain strings (for icons in default package): "spinner"
       */
      icon: {
        type: [String, Array],
        required: true,
        validator(value) {
          return true // TODO: ensure in library?
        },
      },
    },

    computed: {
      faIcon() {
        if (Array.isArray(this.icon)) return this.icon
        return this.icon.includes(":") ? this.icon.split(":") : this.icon
      },
    },
  }
</script>

<style scoped>
</style>