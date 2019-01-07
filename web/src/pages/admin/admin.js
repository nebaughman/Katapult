import Vue from 'vue'
import Admin from './Admin'

import '../common'

Vue.config.productionTip = false

new Vue({
  render: h => h(Admin),
}).$mount('#app')
