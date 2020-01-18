import Vue from 'vue'

Vue.config.productionTip = false

import 'bootstrap'
import 'bootstrap/dist/css/bootstrap.min.css'

// Instead of importing FontAwesomeIcon here, using Icon wrapper component (see below)
// for some minor conveniences (see Icon.vue)
/*
// Notice: Much (much) more efficient for app bundle to only import required icons
import { fas } from '@fortawesome/free-solid-svg-icons'
import { far } from '@fortawesome/free-regular-svg-icons'

import { library } from '@fortawesome/fontawesome-svg-core'
library.add(far)
library.add(fas)

import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
Vue.component('fa-icon', FontAwesomeIcon)
*/

/*
 * Allow components to use <fa-icon...> as if directly using FontAwesomeIcon component,
 * but they're using this Icon as a proxy, which helps centralize the icons being used,
 * so they can be more efficiently imported (rather than importing entire FA libraries).
 */
import Icon from "@/util/Icon"
Vue.component("fa-icon", Icon)

// server may check header for ajax vs user actions
import axios from 'axios'
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest'

import * as log from 'loglevel'
import {Log} from '@/util/Log'

if (process.env.NODE_ENV === "development") {
  log.setDefaultLevel('debug')
  Log.debug("Development mode")
  Log.debug(process.env.VUE_APP_NAME + ' v' + process.env.VUE_APP_VERSION)
  Log.debug("Built " + new Date(parseInt(process.env.VUE_APP_BUILD_TIME)).toUTCString())
} else {
  log.setDefaultLevel('error')
}

import VueRouter from 'vue-router'
Vue.use(VueRouter)
import {router} from "./routes"

import Main from '@/pages/main/Main'
new Vue({
  router,
  render: h => h(Main),
}).$mount('#app')
