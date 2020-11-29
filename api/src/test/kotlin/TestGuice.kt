import com.google.inject.AbstractModule
import com.google.inject.Guice
import org.junit.jupiter.api.Test

interface Fruit

/**
 * A certain [kind] of Apple
 */
data class Apple(val kind: String): Fruit

interface Tropical: Fruit

object Banana: Tropical
object Passion: Tropical

interface Citrus: Fruit

class Orange(peeler: Peeler): Citrus
class Lemon: Citrus

object Peeler

object TestBinding: AbstractModule() {
  override fun configure() {
    bind(Tropical::class.java).toInstance(Banana)
    bind(Apple::class.java).toInstance(Apple("Empire"))
  }
}

class TestGuice {
  @Test
  fun testGuice() {
    val injector = Guice.createInjector(TestBinding)
    println(injector.getInstance(Tropical::class.java))
    println(injector.getInstance(Apple::class.java))
  }
}