import net.nyhm.katapult.HttpsUtil
import org.junit.Test
import java.io.File

class TestHttps {
  /**
   * This method drives the []HttpsUtil] methods, but does not actually test that the results are valid.
   * Https files must be present.
   */
  @Test
  fun testHttps() {
    val pass = "password".toCharArray()
    val ks = HttpsUtil.loadKeystore(
        File("data/fullchain.pem"),
        File("data/privkey.pem"),
        pass
    )
    HttpsUtil.createSSLContext(ks, pass)
  }
}