package net.nyhm.katapult

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.io.File
import java.net.URI
import java.security.KeyFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class HttpsModule(
    val dataDir: File,
    val port: Int = 443,
    val httpPort: Int = 80 // > 0 to enable http redirection
): KatapultModule {

  override fun initialize(app: Javalin) {
    // if non-secure http redirect requested, rewrite url with https scheme
    if (httpPort > 0) {
      app.routes(httpRedirect(port))
    }

    app.server {
      Server().apply {
        addConnector(
            ServerConnector(this, sslContextFactory()).also {
              it.port = port // 443 for standard HTTPS
            }
        )
        if (httpPort > 0) {
          addConnector(
              ServerConnector(this).also { it.port = httpPort }
          )
        }
      }
    }
  }

  /**
   * Javalin redirect router, which will issue a redirect to the same url with https scheme.
   * Use this when https is configured and http redirect is enabled.
   */
  private fun httpRedirect(securePort: Int) = {
    ApiBuilder.before { ctx ->
      if (ctx.scheme() == "http") {
        val https = URI(ctx.url()).let {
          URI(
              "https",
              it.userInfo,
              it.host,
              securePort,
              it.path,
              it.query,
              it.fragment
          )
        }.toURL().toExternalForm()
        ctx.redirect(https)
      }
    }
  }

  /**
   * https://javalin.io/documentation#custom-server
   * https://github.com/tipsy/javalin/blob/master/src/test/java/io/javalin/examples/HelloWorldSecure.java
   */
  private fun sslContextFactory(): SslContextFactory {
    val sslContextFactory = SslContextFactory()
    val storePass = UUID.randomUUID().toString()
    sslContextFactory.keyStore = HttpsUtil.loadKeystore(
        File(dataDir, "fullchain.pem"),
        File(dataDir, "privkey.pem"),
        storePass.toCharArray()
    )
    sslContextFactory.setKeyStorePassword(storePass)
    return sslContextFactory
  }
}

object HttpsUtil {
  /**
   * Create an [SSLContext], which can be used to configure SSL sockets.
   */
  fun createSSLContext(keyStore: KeyStore, storePass: CharArray?, rand: SecureRandom = SecureRandom()): SSLContext {
    val kmf = KeyManagerFactory.getInstance("SunX509")
    //KeyManagerFactory.getDefaultAlgorithm());

    kmf.init(keyStore, storePass)

    val tmf = TrustManagerFactory.getInstance("SunX509")
    //TrustManagerFactory.getDefaultAlgorithm())

    tmf.init(keyStore)

    val context = SSLContext.getInstance("TLS")

    context.init(
        kmf.keyManagers,
        tmf.trustManagers,
        rand)

    return context
  }

  /**
   * Create an in-memory [KeyStore] from the given certificate chain and private key files.
   * These files are expected to be PEM files, just like those provided by Let's Encrypt.
   * storePass is used to protect the private key entry in the store (required by KeyStore
   * for private key entries). Tip: Unless you plan to save the resulting KeyStore to a
   * file, this could just be a random password.
   */
  fun loadKeystore(fullchain: File, privkey: File, storePass: CharArray): KeyStore {
    val keySpec = loadKey(privkey)
    val kf = KeyFactory.getInstance("RSA") // TODO: do not assume RSA
    val privateKey = kf.generatePrivate(keySpec)
    val chain = loadChain(fullchain)
    val ks = KeyStore.getInstance("JKS")
    ks.load(null, storePass) // must init keystore
    ks.setEntry(
        "cert", // appears to be arbitrary for single-entry keystores
        KeyStore.PrivateKeyEntry(privateKey, chain),
        KeyStore.PasswordProtection(storePass)
    )
    return ks
  }

  private fun loadKey(privkey: File): KeySpec {
    val base64 = privkey.readLines().filter { !it.startsWith("-----") }.joinToString("")
    return PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64))
  }

  private fun loadChain(fullchain: File): Array<Certificate> {
    val cf = CertificateFactory.getInstance("X.509")
    return cf.generateCertificates(fullchain.inputStream()).toTypedArray()
  }
}