<template>
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-4">
        <h1 class="h1 mt-5 mb-3 text-center">Katapult</h1>
        <h1 class="h3 mb-3 font-weight-normal text-center">
          Who goes there?
        </h1>

        <input
          type="text"
          id="user"
          class="mb-2 form-control"
          placeholder="Username"
          :disabled="disabled"
          v-model="user"
          required
          autofocus
        >

        <input
          type="password"
          id="pass"
          class="form-control"
          placeholder="Password"
          @keyup.enter="login"
          :disabled="disabled"
          v-model="pass"
          required
        >

        <!--
        <div class="checkbox">
          <label>
            <input type="checkbox" value="remember-me"> Remember me
          </label>
        </div>
        -->

        <button
          class="my-3 btn btn-lg btn-primary btn-block"
          @click="login"
          :disabled="!ready"
        >
          Login <fa-icon icon="sign-in-alt"/>
        </button>

        <p v-if="error" class="text-danger">{{ error }}</p>

      </div>
    </div>
  </div>
</template>

<script>
  import {LoginState} from "../../common/LoginState"
  import * as log from "loglevel"

  export default {
    name: "Login",

    data() {
      return {
        user: '',
        pass: '',
        disabled: false,
        error: '',
      }
    },

    computed: {
      ready() {
        return !this.disabled && this.user && this.pass
      },
    },

    methods: {
      login() {
        this.disabled = true
        LoginState.performLogin(this.user, this.pass).then(response => {
          if (response.data.login) {
            if (response.data.redirect) {
              window.location.href = response.data.redirect
            } else {
              window.location.href = '/'
            }
          } else {
            this.error = 'Unable to log in'
          }
        }).catch(error => {
          log.warn(error)
          this.error = 'Unable to log in'
        }).finally(() => {
          this.disabled = false
        })
      },
    },
  }
</script>

<style scoped>
</style>