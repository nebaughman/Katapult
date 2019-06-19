import "../common"

import Vue from "vue"
import Admin from "./Admin"

import Dashboard from "./Dashboard"
import Other from "./Other"
import NotFound from "../../common/NotFound"

import VueRouter from 'vue-router'
Vue.use(VueRouter)

// vue routes (paths relative to this page)
const routes = [
  { path: "*", component: NotFound },
  { path: "/", component: Dashboard },
  { path: "/other", component: Other },
]

export const router = new VueRouter({
  mode: "history",
  routes,
});

new Vue({
  router,
  render: h => h(Admin),
}).$mount('#app')
