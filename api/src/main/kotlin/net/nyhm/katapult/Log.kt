package net.nyhm.katapult

import org.slf4j.LoggerFactory

/**
 * Logging service (wrapper)
 */
object Log {
  private fun logger(source: Any) = LoggerFactory.getLogger(source.javaClass)

  fun info(source: Any, msg: () -> Any?) = logger(source).info(msg()?.toString())

  fun warn(source: Any, e: Exception? = null, msg: () -> Any? = { e?.message }) =
    logger(source).warn(msg()?.toString(), e)

  fun fail(source: Any, e: Exception = Exception(), msg: () -> Any? = { e.message }) =
    logger(source).error(msg()?.toString(), e)
}

fun Any.info(msg: () -> Any?) = Log.info(this, msg)
fun Any.warn(e: Exception? = null, msg: () -> Any? = { e?.message }) = Log.warn(this, e, msg)
fun Any.fail(e: Exception = Exception(), msg: () -> Any? = { e.message }) = Log.fail(this, e, msg)