package net.nyhm.katapult

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import io.javalin.security.Role
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Access role
 */
enum class UserRole: Role {
  @JsonProperty("user")
  USER,

  @JsonProperty("admin")
  ADMIN
}

interface User {
  var name: String
  var pass: String
  var role: UserRole
}

@JsonSerialize(converter = UserConverter::class)
data class UserData(
    override var name: String,
    override var pass: String,
    override var role: UserRole
): User {
  companion object {
    fun fromUser(user: User) = UserData(
        user.name,
        user.pass,
        user.role
    )
  }
}

@JsonSerialize(converter = UserConverter::class)
class UserEntity(id: EntityID<Int>): IntEntity(id), User {
  companion object: IntEntityClass<UserEntity>(Users)

  override var name by Users.name
  override var pass by Users.pass
  override var role by Users.role
}

/**
 * Jackson Json converter for UserEntity into UserInfo suitable for sending to client
 */
object UserConverter: StdConverter<User,UserInfo>() {
  override fun convert(value: User) = UserInfo.fromUser(value)
}

/**
 * User fields suitable for sending to client
 */
data class UserInfo(val name: String, val role: UserRole) {
  companion object {
    fun fromUser(user: User) = UserInfo(user.name, user.role)
  }
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
object UserDao {
  fun create(user: UserData): User = transaction {
    UserEntity.new {
      name = user.name
      pass = user.pass
      role = user.role
    }
  }

  fun getById(id: Int): User? = transaction { UserEntity[id] }

  fun findName(name: String): User? = transaction {
    UserEntity.find { Users.name eq name }.firstOrNull()
  }

  fun getUsers(): List<User> = transaction {
    UserEntity.all().toList()
  }

  fun remove(name: String) = transaction {
    UserEntity.find { Users.name eq name }.firstOrNull()?.delete() ?:
      throw IllegalArgumentException("No such user")
  }
}

/* This was not needed due to @JsonSerialize converter
// https://stackoverflow.com/a/25513874
class UserSerializer : JsonSerializer<User>() {
  @Throws(IOException::class, JsonProcessingException::class)
  override fun serialize(value: User, jgen: JsonGenerator, provider: SerializerProvider) {
    val serializableUser = if (value is UserData) value else UserData.fromUser(value)
    val serializer = BeanSerializerFactory.instance.createSerializer(
        provider, provider.typeFactory.constructType(User::class.java))
    serializer.serialize(serializableUser, jgen, provider)
  }
}
*/

/*
object UsersDao: Users {
  override fun getUsers(): List<User> {
    return UserEntity.all().toList()
  }

  override fun getUser(id: Int): User? {
    return UserEntity[id]
  }

  override fun findName(name: String): User? {
    return UserEntity.find { UsersTable.name eq name }.firstOrNull()
  }

  override fun saveUser(user: User): User {

  }
}

 *
 * User DAO
 *
interface Users {
  fun getUsers(): List<User>
  fun getUser(id: Int): User?
  fun findName(name: String): User?
  fun saveUser(user: User): User
}

 *
 * In-memory User store
 *
class UsersCache(init: List<User> = emptyList()): Users {

  private val users = mutableMapOf<Int,User>()

  init {
    init.forEach { saveUser(it) }
  }

  private fun nextId() = users.keys.max()?.plus(1) ?: 1

  override fun getUsers() = users.values.toList()

  override fun getUser(id: Int) = users[id]

  override fun findName(login: String) = users.values.find { it.name == name }

  override fun saveUser(user: User): User {
    val save = if (user.id == null) user.copy(id = nextId()) else user
    users[save.id!!] = save
    return save
  }
}
*/