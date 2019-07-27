import Vue from 'vue'
import Main from './Main'

import '../common'

Vue.config.productionTip = false

import VueRouter from 'vue-router'
Vue.use(VueRouter)

import Title from "./Title"
import UserPage from "./UserPage"
import Dashboard from "@/pages/admin/Dashboard"
import Other from "@/pages/admin/Other"
import NotFound from "@/common/NotFound"

const routes = [
  { path: "/user", component: UserPage },
  { path: "/admin", component: Dashboard },
  { path: "/admin/other", component: Other },
  { path: "/", component: Title },
  { path: "*", component: NotFound },
]

const router = new VueRouter({
  //base: "/",
  mode: "history",
  routes,
});

new Vue({
  router,
  render: h => h(Main),
}).$mount('#app')
