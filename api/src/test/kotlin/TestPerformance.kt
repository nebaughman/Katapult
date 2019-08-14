import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.post
import net.nyhm.katapult.*
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpHeader
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class TestModule: KatapultModule {

  private val routes = {
    post("/api/reflect") { it.process(::handle) }

    post("/api/direct") { ctx ->
      val value = ctx.body<TestData>().value
      ctx.json(TestData(value.reversed()))
    }
  }

  override fun config(app: Javalin) {
    app.routes(routes)
  }

  fun handle(@Body data: TestData) = TestData(data.value.reversed())
}

data class TestData(
    val value: String
)

class Client(val url: String): AutoCloseable {

  private val mapper = jacksonObjectMapper()

  private val client = HttpClient()

  init {
    client.start()
  }

  fun <R:Any> send(payload: Any, responseType: KClass<R>): R {
    val content = if (payload is String) payload else mapper.writeValueAsString(payload)
    val request = client.POST(url)
    request.header(HttpHeader.CONTENT_TYPE, "application/json")
    request.content(StringContentProvider(content))
    val response = request.send()
    return mapper.readValue(response.contentAsString, responseType.java)
  }

  override fun close() {
    client.stop()
  }
}

/**
 * This does not measure absolute performance of call handling.
 * At best, this measures relative performance of _direct_ Javalin vs Katapult _reflect_ calls.
 * These numbers tend to be skewed based on which is called first, so consider calling in separate runs.
 * This will also be useful for testing improvements to Katapult (eg, endpoint caching).
 *
 * As of the time of this writing, these sparse/non-definitive tests show that a direct call to
 * Javalin is faster (maybe significantly) than using reflection-based Katapult calls atop Javalin.
 * No surprise there. The real question is whether the reflection adds significant overhead
 * (which it might) and whether that can be improved (which it certainly can) to an acceptable
 * degree (which depends).
 */
class TestPerformance {
  @Test fun testDirect() = testPerformance("direct")
  @Test fun testReflect() = testPerformance("reflect")

  private fun testPerformance(endpoint: String) {
    val katapult = Katapult(listOf(TestModule::class)).start()
    val ms = call(endpoint, TestData("asdf"), 8000)
    println("$endpoint ${ms}ms")
    katapult.stop()
  }

  private fun call(endpoint: String, payload: Any, count: Int): Long {

    Thread.sleep(2000)

    // warm-up (not counted in timing)
    Client("http://localhost:7000/api/$endpoint").use { client ->
      val response = client.send(payload, TestData::class)
      //println(response)
    }

    Thread.sleep(2000)

    val start = System.nanoTime()
    Client("http://localhost:7000/api/$endpoint").use { client ->
      for (i in 1..count) client.send(payload, TestData::class)
      //val response = client.send(payload, TestData::class)
      //println(response)
    }
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
  }
}