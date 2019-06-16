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

// server may check header for ajax vs user actions
import axios from 'axios'
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest'

import * as log from 'loglevel'
import {Log} from '../common/Log.ts'

if (process.env.NODE_ENV === "development") {
  log.setDefaultLevel('debug')
  Log.debug("Development mode")
  Log.debug(process.env.VUE_APP_NAME + ' v' + process.env.VUE_APP_VERSION)
  Log.debug("Built " + new Date(parseInt(process.env.VUE_APP_BUILD_TIME)).toUTCString())
} else {
  log.setDefaultLevel('error')
}