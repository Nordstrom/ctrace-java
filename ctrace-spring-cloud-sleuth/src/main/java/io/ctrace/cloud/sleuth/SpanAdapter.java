package io.ctrace.cloud.sleuth;

import io.ctrace.ActiveSpan;
import io.ctrace.Span;
import io.ctrace.ThreadLocalActiveSpan;
import io.ctrace.Tracer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.springframework.cloud.sleuth.Log;
import org.springframework.web.client.RestTemplate;


public class SpanAdapter extends org.springframework.cloud.sleuth.Span {

  private Span span;
  private ActiveSpan activeSpan;

  /**
   * Creates a new span that still tracks tags and logs of the current span. This is crucial when
   * continuing spans since the changes in those collections done in the continued span need to be
   * reflected until the span gets closed.
   */
  SpanAdapter(Tracer tracer, Span span, ActiveSpan activeSpan) {
    // HACK: build a default span to pass in current span.
    super(org.springframework.cloud.sleuth.Span.builder().build(), null);
    this.span = span;
    this.activeSpan = activeSpan;

    RestTemplate
  }

  /**
   * The block has completed, stop the clock
   */
  @Override
  public synchronized void stop() {
    this.activeSpan.deactivate();
  }

  /**
   * Return the total amount of time elapsed since start was called, if running, or difference
   * between stop and start
   *
   * @deprecated use {@link #getAccumulatedMicros()} as it is more precise.
   */
  @Override
  public synchronized long getAccumulatedMillis() {
    return this.span.duration();
  }

  /**
   * Return the total amount of time elapsed since start was called, if running, or difference
   * between stop and start, in microseconds.
   *
   * Note that in case of the spans that have CS / CR events we will not send to Zipkin the
   * accumulated microseconds but will calculate the duration basing on the timestamps of the CS /
   * CR events.
   *
   * @return zero if not running, or a positive number of microseconds.
   */
  @Override
  public synchronized long getAccumulatedMicros() {
    return this.span.duration() > 0 ? this.span.duration() * 1000 : 0;
  }

  /**
   * Has the span been started and not yet stopped?
   */
  @Override
  public synchronized boolean isRunning() {
    return !this.span.finished();
  }

  /**
   * Add a tag or data annotation associated with this span. The tag will be added only if it has a
   * value.
   */
  @Override
  public void tag(String key, String value) {
    this.span.setTag(key, value);
  }

  /**
   * Add an log event to the timeline associated with this span.
   */
  @Override
  public void logEvent(String event) {
    this.span.log(event);
  }

  /**
   * Add a log event to a specific point (a timestamp in milliseconds) in the timeline associated
   * with this span.
   */
  @Override
  public void logEvent(long timestampMilliseconds, String event) {
    this.span.log(timestampMilliseconds * 1000, event);
  }

  /**
   * Sets a baggage item in the Span (and its SpanContext) as a key/value pair.
   *
   * Baggage enables powerful distributed context propagation functionality where arbitrary
   * application data can be carried along the full path of request execution throughout the
   * system.
   *
   * Note 1: Baggage is only propagated to the future (recursive) children of this SpanContext.
   *
   * Note 2: Baggage is sent in-band with every subsequent local and remote calls, so this feature
   * must be used with care.
   *
   * @return this Span instance, for chaining
   */
  @Override
  public SpanAdapter setBaggageItem(String key, String value) {
    this.span.setBaggageItem(key, value);
    return this;
  }

  /**
   * @return the value of the baggage item identified by the given key, or null if no such item
   *     could be found
   */
  @Override
  public String getBaggageItem(String key) {
    return this.span.getBaggageItem(key);
  }

  /**
   * Get tag data associated with this span (read only)
   * <p/>
   * <p/>
   * Will never be null.
   */
  @Override
  public Map<String, String> tags() {
    val map = new LinkedHashMap<String, String>();
    for (val tag : this.span.tags()) {
      map.put(tag.getKey(), tag.getValue().toString());
    }
    return map;
  }

  /**
   * Get any timestamped events (read only)
   * <p/>
   * <p/>
   * Will never be null.
   */
  @Override
  public List<Log> logs() {
    return super.logs();
  }

  /**
   * Returns the saved span. The one that was "current" before this span. <p> Might be null
   */
  @Override
  public org.springframework.cloud.sleuth.Span getSavedSpan() {
    return null;
  }

  @Override
  public boolean hasSavedSpan() {
    return false;
  }

  /**
   * A human-readable name assigned to this span instance. <p>
   */
  @Override
  public String getName() {
    return super.getName();
  }

  /**
   * A pseudo-unique (random) number assigned to this span instance. <p> <p> The span id is
   * immutable and cannot be changed. It is safe to access this from multiple threads.
   */
  @Override
  public long getSpanId() {
    return super.getSpanId();
  }

  /**
   * When non-zero, the trace containing this span uses 128-bit trace identifiers.
   *
   * <p>{@code traceIdHigh} corresponds to the high bits in big-endian format and {@link
   * #getTraceId()} corresponds to the low bits.
   *
   * <p>Ex. to convert the two fields to a 128bit opaque id array, you'd use code like below.
   * <pre>{@code
   * ByteBuffer traceId128 = ByteBuffer.allocate(16);
   * traceId128.putLong(span.getTraceIdHigh());
   * traceId128.putLong(span.getTraceId());
   * traceBytes = traceId128.array();
   * }</pre>
   *
   * @see #traceIdString()
   * @since 1.0.11
   */
  @Override
  public long getTraceIdHigh() {
    return super.getTraceIdHigh();
  }

  /**
   * Unique 8-byte identifier for a trace, set on all spans within it.
   *
   * @see #getTraceIdHigh() for notes about 128-bit trace identifiers
   */
  @Override
  public long getTraceId() {
    return super.getTraceId();
  }

  /**
   * Return a unique id for the process from which this span originated. <p> Might be null
   */
  @Override
  public String getProcessId() {
    return super.getProcessId();
  }

  /**
   * Returns the parent IDs of the span. <p> <p> The collection will be empty if there are no
   * parents.
   */
  @Override
  public List<Long> getParents() {
    return super.getParents();
  }

  /**
   * Flag that tells us whether the span was started in another process. Useful in RPC tracing when
   * the receiver actually has to add annotations to the senders span.
   */
  @Override
  public boolean isRemote() {
    return super.isRemote();
  }

  /**
   * Get the start time, in milliseconds
   */
  @Override
  public long getBegin() {
    return super.getBegin();
  }

  /**
   * Get the stop time, in milliseconds
   */
  @Override
  public long getEnd() {
    return super.getEnd();
  }

  /**
   * Is the span eligible for export? If not then we may not need accumulate annotations (for
   * instance).
   */
  @Override
  public boolean isExportable() {
    return super.isExportable();
  }

  /**
   * Returns the 16 or 32 character hex representation of the span's trace ID
   *
   * @since 1.0.11
   */
  @Override
  public String traceIdString() {
    return super.traceIdString();
  }

  /**
   * Converts the span to a {@link SpanBuilder} format
   */
  @Override
  public SpanBuilder toBuilder() {
    return super.toBuilder();
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  public ActiveSpan activeSpan() {
    return this.activeSpan;
  }

  public Span span() {
    return this.span;
  }
}
