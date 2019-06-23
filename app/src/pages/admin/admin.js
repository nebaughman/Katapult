import "../common"

import Vue from "vue"
import Admin from "./Admin"

import Dashboard from "./Dashboard"
import Other from "./Other"
import NotFound from "../../common/NotFound"

import VueRouter from 'vue-router'
Vue.use(VueRouter)

const routes = [
  { path: "/admin", component: Dashboard },
  { path: "/admin/other", component: Other },
  { path: "/admin/*", component: NotFound },
]

const router = new VueRouter({
  //base: "/admin/",
  mode: "history",
  routes,
});

new Vue({
  router,
  render: h => h(Admin),
}).$mount('#app')
