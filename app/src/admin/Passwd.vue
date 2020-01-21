<template>
  <div class="form-row">
    <div class="col-auto">
      <select class="custom-select custom-select-sm mr-2" v-model="user">
        <option v-for="user in users" :key="user.name" :value="user.name">{{ user.name }}</option>
      </select>
    </div>

    <div class="col">
      <input
        class="form-control form-control-sm mr-2"
        type="password"
        placeholder="Password"
        v-model="pass"
        :disabled="busy"
      />
    </div>

    <div class="col">
      <input
        class="form-control form-control-sm mr-2"
        type="password"
         placeholder="Confirm"
         v-model="conf"
         :disabled="busy"
       />
    </div>

    <div class="col-auto">
      <button
        class="btn btn-primary btn-sm"
        @click="changePassword"
        :disabled="busy || !ready"
      >Change</button>
    </div>
  </div>
</template>

<script>
  import {LoginState} from "@/state/LoginState"
  import {AdminState} from "@/state/AdminState"
  import {Api} from "@/state/Api"
  import {Log} from "@/util/Log"

  export default {
    name: "Passwd",

    data() {
      return {
        user: '',
        pass: '',
        conf: '',
        busy: false,
      }
    },

    computed: {
      ready() {
        return this.user && this.pass && this.pass === this.conf
      },

      users() {
        return AdminState.users
      },

      currentUser() {
        return LoginState.user ? LoginState.user.name : null
      },
    },

    methods: {
      changePassword() {
        this.busy = true
        Api.passwd(this.user, this.pass).then(() => {
          this.pass = ''
          this.conf = ''
          this.busy = false
        }).catch(error => {
          Log.error(error)
          this.busy = false // TODO: show error
        })
      },
    },

    watch: {
      currentUser: {
        immediate: true,
        handler(value) {
          if (value && !this.user) this.user = value
        }
      },
    },
  }
</script>

<style scoped>
</style>