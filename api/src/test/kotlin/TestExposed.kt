import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.sql.Connection

/**
 * This is not a unit test for Katapult.
 * Just a sample driver to experiment with Kotlin Exposed.
 *
 * https://github.com/JetBrains/Exposed/wiki/DAO
 */
class TestExposed {
  @Test
  fun testExposed() {

    Database.connect("jdbc:sqlite:data/test.sqlite", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    // only sqlite requires serializable isolation level

    transaction {
      SchemaUtils.create(Widgets)
    }

    transaction {
      // create
      val widget = Widget.new {
        name = "WidgetA"
        count = 3
      }
    }
  }
}

object Widgets: IntIdTable() {
  val name = text("name")
  val count = integer("count")
}

class Widget(id: EntityID<Int>): IntEntity(id) {
  companion object: IntEntityClass<Widget>(Widgets)

  var name by Widgets.name
  var count by Widgets.count
}