package net.nyhm.katapult.mod

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Context
import net.nyhm.katapult.KatapultModule
import net.nyhm.katapult.info
import net.nyhm.katapult.process
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// TODO: update interval configuration from injector
class ApiStats(requestLog: RequestLog) : KatapultModule {

  private val reporter = EndpointReporter()
  private val logger = ApiLogger(reporter)

  init {
    requestLog.add(logger)
  }

  override fun config(app: Javalin) {
    app.events { event ->
      event.serverStarting { reporter.start() }
      event.serverStopping { reporter.stop() }
    }

    // TODO: include update interval in response, client can adjust polling (better: use web socket)
    // TODO: rather, use web socket
    app.routes {
      get("/api/admin/stats") { it.process(::stats) }
    }
  }

  fun stats() = reporter.stats
}

/**
 * Collects reports for the reporter from the RequestLog system
 */
class ApiLogger(private val reporter: EndpointReporter): RequestLogger {
  override fun log(ctx: Context, ms: Float) {
    val bytes = countResponseBytes(ctx)
    val time = if (ms > 1) ms.toInt().toString() else "0" //"<1" // "%.2f".format(ms)
    val endpointPath = "${ctx.method()} ${ctx.matchedPath()}"
    reporter.add(EndpointRecord(endpointPath, 1, bytes.toLong(), time.toLong()))
  }

  private fun countResponseBytes(ctx: Context): Int {
    return ctx.resultString()?.length ?: 0 * 2 // same as stream length
    //return if (stream == null) 0L else IO.readBytes(stream).size
  }
}

class EndpointReporter {

  /**
   * The latest stats
   */
  val stats get() = latestReport.get()

  private var latestReport: AtomicReference<EndpointReport?> = AtomicReference()

  private val calculator = EndpointCalculator()

  private val delayTime = 30L
  private val delayUnits = TimeUnit.SECONDS

  private val pending = ConcurrentLinkedQueue<EndpointRecord>()
  private val executor = Executors.newScheduledThreadPool(1)
  private var future: ScheduledFuture<*>? = null

  fun add(record: EndpointRecord) {
    pending.offer(record)
  }

  fun start() {
    future = executor.scheduleAtFixedRate(Runnable(::report), delayTime, delayTime, delayUnits)
  }

  fun stop() {
    future?.cancel(false) // TODO: is this needed, or is shutdown() sufficient?
    executor.shutdown()
  }

  /**
   * Produces a new report, storing it in [latestReport]
   */
  private fun report() {
    if (pending.isEmpty()) return // nothing new

    // consume pending records (thread safe)
    var r: EndpointRecord? = pending.poll()
    while (r != null) {
      calculator.add(r)
      r = pending.poll()
    }

    latestReport.set(calculator.totals)

    val size = stats?.totalSize() ?: 0L
    info { "Total bytes sent: $size" }
  }
}

@JsonIgnoreProperties("average")
data class EndpointRecord(
  val path: String,
  val hits: Long = 0,
  val size: Long = 0, // bytes
  val time: Long = 0 // ms
) {
  /**
   * [path] is the [rhs] record's path
   */
  operator fun plus(rhs: EndpointRecord) = EndpointRecord(
    path = rhs.path,
    hits = hits + rhs.hits,
    size = size + rhs.size,
    time = time + rhs.time
  )

  /**
   * An endpoint record representing the average size and time of this record
   */
  val average by lazy {
    copy(
      size = if (hits > 0) size / hits else 0,
      time = if (hits > 0) time / hits else 0
    )
  }
}

typealias EndpointReport = List<EndpointRecord>

fun EndpointReport.totalTime() = this.fold(0L) { total, record -> total + record.time }
fun EndpointReport.totalSize() = this.fold(0L) { total, record -> total + record.size }

class EndpointCalculator {

  val totals: List<EndpointRecord> get() = records.values.toList()

  val totalTime get() = totals.fold(0L) { total, record -> total + record.time }
  val totalSize get() = totals.fold(0L) { total, record -> total + record.size }

  private val records = mutableMapOf<String,EndpointRecord>()

  fun add(record: EndpointRecord) {
    val path = record.path
    val total = records.getOrPut(path) { EndpointRecord(path) }
    records[path] = total + record
  }
}

// dropping last N averages for now
/*
data class EndpointReport(val averages: List<EndpointRecord>) {
  val overallAverage by lazy {
    EndpointHistory("").apply {
      averages.forEach { add(it) }
    }.avg?.copy(path = "")
  }
}

class EndpointHistory(val path: String) {

  private val history = mutableListOf<EndpointRecord>()

  val avg: EndpointRecord? get() {
    if (history.isEmpty()) return null
    if (history.size == 1) return history.first()
    var size = 0.0
    var time = 0.0
    history.forEach { size += it.size; time += it.time }
    val items = history.size.toDouble()
    return EndpointRecord(
      history.first().path, // assume all same
      (size / items).toLong(),
      (time / items).toLong()
    )
  }

  fun add(record: EndpointRecord) {
    history.add(record)
    while (history.size > 10) history.removeAt(0)
  }
}
*/