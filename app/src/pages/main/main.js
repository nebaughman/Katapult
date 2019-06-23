import Vue from 'vue'
import Main from './Main'

import '../common'

Vue.config.productionTip = false

/*
import VueRouter from 'vue-router'
Vue.use(VueRouter)

import InfoTitle from "./InfoTitle"
import NotFound from "../../common/NotFound"

const routes = [
  { path: "/", component: InfoTitle },
  { path: "*", component: NotFound },
]

const router = new VueRouter({
  //base: "/",
  mode: "history",
  routes,
});
*/

new Vue({
  //router,
  render: h => h(Main),
}).$mount('#app')
