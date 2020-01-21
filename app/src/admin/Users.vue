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
  import {AdminState} from "@/state/AdminState"
  import AddUser from "./AddUser"
  import Passwd from "./Passwd"
  import ConfirmButton from "@/common/ConfirmButton"

  export default {
    name: "Users",
    components: {AddUser, Passwd, ConfirmButton},

    computed: {
      users() {
        return AdminState.users
      },
    },

    methods: {
      remove(name) {
        AdminState.removeUser(name)
      },

      deletable(user) {
        return user.name !== (AdminState.user ? AdminState.user.name : null)
      },
    },
  }
</script>

<style scoped>
</style>