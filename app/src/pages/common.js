import Vue from 'vue'

import 'bootstrap'
import 'bootstrap/dist/css/bootstrap.min.css'

// Notice: Much (much) more efficient for app bundle to only import required icons
import { fas } from '@fortawesome/free-solid-svg-icons'
import { far } from '@fortawesome/free-regular-svg-icons'

import { library } from '@fortawesome/fontawesome-svg-core'
library.add(far)
library.add(fas)

import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
Vue.component('fa-icon', FontAwesomeIcon)

import * as log from 'loglevel'
if (process.env.NODE_ENV === "development") {
  log.setDefaultLevel('debug')
  log.debug('Development mode')
} else {
  log.setDefaultLevel('error')
}