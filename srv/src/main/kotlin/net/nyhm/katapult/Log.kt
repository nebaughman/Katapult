package net.nyhm.katapult

import org.slf4j.LoggerFactory

object Log {
  //val logger: Logger = LoggerFactory.getLogger(Log.javaClass)

  private fun logger(source: Any) = LoggerFactory.getLogger(source.javaClass)

  fun info(source: Any, msg: () -> Any?) = logger(source).info(msg()?.toString())

  fun fail(source: Any, e: Exception = Exception()) = fail(source, e) {}

  fun fail(source: Any, e: Exception = Exception(), msg: () -> Any?) =
    logger(source).error(msg()?.toString(), e)
}
