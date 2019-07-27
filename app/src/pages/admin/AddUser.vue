<template>
  <div class="form-row">
    <div class="col">
      <input class="form-control form-control-sm mr-2" type="text" placeholder="User" v-model="name"/>
    </div>

    <div class="col">
      <input class="form-control form-control-sm mr-2" type="password" placeholder="Pass" v-model="pass"/>
    </div>

    <div class="col-auto">
      <select class="custom-select custom-select-sm mr-2" v-model="role">
        <option value="user">User</option>
        <option value="admin">Admin</option>
      </select>
    </div>

    <div class="col-auto">
      <button class="btn btn-primary btn-sm" @click="addUser" :disabled="!ready">Add</button>
    </div>
  </div>
</template>

<script>
  import {AdminState} from "@/state/AdminState"

  export default {
    name: "AddUser",

    data() {
      return {
        name: '',
        pass: '',
        role: 'user',
      }
    },

    computed: {
      ready() {
        return this.name && this.pass && this.role
      },
    },

    methods: {
      addUser() {
        AdminState.addUser(this.name, this.pass, this.role)
        this.name = ''
        this.pass = ''
        this.role = 'user'
      },
    },
  }
</script>

<style scoped>
</style>