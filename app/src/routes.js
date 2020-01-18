import Title from "@/pages/main/Title"
import UserPage from "@/pages/main/UserPage"
import Admin from "@/pages/admin/Admin"
import Dashboard from "@/pages/admin/Dashboard"
import Other from "@/pages/admin/Other"
import Login from "@/pages/login/Login"
import NotFound from "@/common/NotFound"
import {LoginState} from "@/state/LoginState"
import VueRouter from 'vue-router'

const routes = [
  {
    name: "logout",
    path: "/logout",
    redirect: { name: "login" },
  },
  {
    name: "login",
    path: "/login",
    component: Login,
    beforeEnter: beforeLogin,
  },
  {
    path: "/user",
    component: UserPage,
  },
  {
    path: "/admin",
    component: Admin,
    beforeEnter: beforeAdmin,
    children: [
      {
        path: "",
        component: Dashboard,
      },
      {
        path: "other",
        component: Other,
      },
    ],
  },
  {
    path: "/",
    component: Title,
  },
  {
    name: "not-found",
    path: "*",
    component: NotFound,
  },
]

async function beforeLogin(to, from, next) {
  await LoginState.logout()
  next()
}

/**
 * Prevent non-admin users from seeing the admin pages.
 * This is not really a security feature (the admin pages are part of the bundle).
 * Consider it better user experience.
 * It's a bit sneaky, because it shows the not-found page for non-admins.
 * Without this guard, non-admin users could navigate to /admin and see the page,
 * but all admin api calls are guarded by the server, so no content would be available.
 */
async function beforeAdmin(to, from, next) {
   // await user to be loaded from server (if not already)
   const user = await LoginState.awaitUser()
   if (!user) next({ name: "login" }) // redundant to router.beforeEach guard (below)
   else if (user.role === "admin") next() // all is well
   else next({ name: "not-found", params: { "0": to.fullPath } }) // logged in, but not an admin
   // must set param "0" to path, because target path has "*" path
   // https://github.com/vuejs/vue-router/issues/724#issuecomment-349966378
 }

export const router = new VueRouter({
  //base: "/",
  mode: "history",
  routes,

  // https://router.vuejs.org/guide/advanced/scroll-behavior.html#scroll-behavior
  scrollBehavior (to, from, savedPosition) {
    if (to.hash) {
      return { selector: to.hash }
    } else if (savedPosition) {
      return savedPosition
    } else {
      return { x: 0, y: 0 }
    }
  },

});

router.beforeEach(async (to, from, next) => {
  if (to.name === "login") {
    next()
  } else {
    const user = await LoginState.awaitUser()
    user ? next() : next({ name: "login" })
  }
})