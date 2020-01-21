<template>
  <div>
    <table class="table table-hover border-top border-bottom mb-0">
      <tbody>
        <tr v-for="user in users" :key="user.name">
          <td class="align-middle">{{ user.name }}</td>
          <td class="align-middle">{{ user.role }}</td>
          <td class="text-right align-middle w-100">
            <ConfirmButton
              v-show="deletable(user)"
              class="py-0"
              confirmMessage="Delete?"
              @click="remove(user.name)"
              :disabled="!deletable(user)"
            />
          </td>
        </tr>
      </tbody>
    </table>

    <p v-if="users.length === 0" class="m-0 text-muted">No users</p>

    <h5 class="card-title mt-4">Add User</h5>
    <AddUser/>

    <h5 class="card-title mt-4">Change Password</h5>
    <Passwd/>
  </div>
</template>

<script>
  import {State} from "@/state/State"
  import AddUser from "./AddUser"
  import Passwd from "./Passwd"
  import ConfirmButton from "@/common/ConfirmButton"

  export default {
    name: "Users",
    components: {AddUser, Passwd, ConfirmButton},

    computed: {
      users() {
        return State.admin.users
      },
    },

    methods: {
      remove(name) {
        State.admin.removeUser(name)
      },

      deletable(user) {
        return user.name !== (State.admin.user ? State.admin.user.name : null)
      },
    },
  }
</script>

<style scoped>
</style>