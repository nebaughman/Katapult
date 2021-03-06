package net.nyhm.katapult.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import io.javalin.core.security.Role
import net.nyhm.katapult.ExposedDb
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.Log
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @param hash function that takes a plaintext password and produces a hashed
 * version suitable for db storage
 */
data class UsersSpec(
    val hash: (String) -> String
)

/**
 * Sample Users module, which initializes users table.
 * Requires a DB module to have been initialized first.
 */
class UsersModule(val spec: UsersSpec, val userDao: UserDao): KatapultModule {

  init {

    // create admin user with initial default password
    transaction {
      if (userDao.findName("admin") == null) {
        val pass = spec.hash("pass")
        val admin = userDao.create(UserData("admin", pass, UserRole.ADMIN))
        Log.info(this) { "Created user ${admin.name}" }
      }
    }
    // TODO: Store a pre-hashed/salted default password for admin; insert this into the db;
    // upon login, if admin user's hash matches this, then force pw change;
    // alternatively, add flag to user to force pw change on next login
  }
}

/**
 * Access role
 */
enum class UserRole: Role {
  @JsonProperty("user")
  USER,

  @JsonProperty("admin")
  ADMIN,

  @JsonProperty("guest")
  GUEST
}

// TODO: Define specific userId type
//data class UserId(val id: Int)

interface User {
  var name: String
  var pass: String
  var role: UserRole
}

interface UserDao {
  fun create(user: User): User
  fun getById(id: Int): User?
  fun findName(name: String): User?
  fun getUsers(): List<User>
  fun remove(name: String)
}

@JsonSerialize(converter = UserConverter::class)
data class UserData(
    override var name: String,
    override var pass: String,
    override var role: UserRole
): User {
  constructor(user: User): this(user.name, user.pass, user.role)
}

@JsonSerialize(converter = UserConverter::class)
class UserEntity(id: EntityID<Int>): IntEntity(id), User {
  companion object: IntEntityClass<UserEntity>(Users)

  //override var userId: UserId? = UserId(id.value)
  override var name by Users.name
  override var pass by Users.pass
  override var role by Users.role
}

/**
 * User fields suitable for sending to client
 */
data class UserInfo(val name: String, val role: UserRole) {
  constructor(user: User): this(user.name, user.role)
}

/**
 * Jackson Json converter for UserEntity into UserInfo suitable for sending to client
 */
object UserConverter: StdConverter<User, UserInfo>() {
  override fun convert(value: User) = UserInfo(value)
}

/**
 * Users table
 */
object Users: IntIdTable("users") {
  val name = text("name").uniqueIndex()
  val pass = text("pass")
  val role = enumerationByName("role", 16, UserRole::class)
}

/**
 * Provides access to User data objects.
 * These methods may be called without invoking a [transaction].
 * Any mutation to the data object must be wrapped in a transaction for the mutation to be persisted.
 *
 * Note that data objects may be fetched without a transaction, then later wrapped in a transaction
 * for mutation; or the entire operation may be wrapped in a transaction.
 */
class ExposedUserDao(db: ExposedDb): UserDao {

  init {
    db.init(Users)
  }

  override fun create(user: User): User = transaction {
    UserEntity.new {
      name = user.name
      pass = user.pass
      role = user.role
    }
  }

  override fun getById(id: Int): User? = transaction { UserEntity[id] }

  override fun findName(name: String): User? = transaction {
    UserEntity.find { Users.name eq name }.firstOrNull()
  }

  override fun getUsers(): List<User> = transaction {
    UserEntity.all().toList()
  }

  override fun remove(name: String) = transaction {
    UserEntity.find { Users.name eq name }.firstOrNull()?.delete() ?:
      throw IllegalArgumentException("No such user")
  }
}
