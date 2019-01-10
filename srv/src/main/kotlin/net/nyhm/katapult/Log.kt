package net.nyhm.katapult

import org.slf4j.LoggerFactory

object Log {
  private fun logger(source: Any) = LoggerFactory.getLogger(source.javaClass)

  fun info(source: Any, msg: () -> Any?) = logger(source).info(msg()?.toString())

  fun warn(source: Any, e: Exception? = null, msg: () -> Any? = { e?.message }) =
    logger(source).warn(msg()?.toString(), e)

  fun fail(source: Any, e: Exception = Exception(), msg: () -> Any? = { e.message }) =
    logger(source).error(msg()?.toString(), e)
}
