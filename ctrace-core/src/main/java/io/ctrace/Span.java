package io.ctrace;

import static java.lang.Math.floorDiv;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

/**
 * Represents an in-flight Span that's <strong>manually propagated</strong> within the given
 * process.
 *
 * <p>{@link Span}s are created by the {@link Tracer.SpanBuilder#startManual} method;
 */
@Accessors(fluent = true)
public class Span implements io.opentracing.Span {
  @Getter private final Tracer tracer;
  @Getter private final SpanContext context;
  @Getter private final String serviceName;
  @Getter private String operationName;
  @Getter private String parentId;
  @Getter private long startMillis;
  private long finishMillis;
  private long duration;
  private boolean finished;
  private Map<String, ? super Object> tags;

  Span(
      Tracer tracer,
      String serviceName,
      String operationName,
      long startMicros,
      Map<String, Object> tags,
      SpanContext parentContext) {
    this.operationName = operationName;
    this.serviceName = serviceName;
    this.tags = tags;
    if (parentContext != null) {
      this.context = new SpanContext(parentContext);
      this.parentId = parentContext.spanId();
    } else {
      this.context = new SpanContext();
    }
    this.tracer = tracer;

    if (startMicros <= 0) {
      this.startMillis = Tools.nowMillis();
    } else {
      this.startMillis = floorDiv(startMicros, 1000);
    }

    this.tracer.logger().start(this, new Log(this.startMillis, "Start-Span"));
  }

  @Synchronized
  public long finishMillis() {
    return this.finishMillis;
  }

  @Synchronized
  public long duration() {
    return this.duration;
  }

  @Synchronized
  public boolean finished() {
    return this.finished;
  }

  /**
   * Get tags.
   *
   * @return iterable of tags map entries
   */
  @Synchronized
  public Iterable<? extends Map.Entry<String, ?>> tags() {
    if (this.tags == null) {
      return null;
    }
    return this.tags.entrySet();
  }

  public Iterable<Entry<String, String>> baggage() {
    return this.context.baggageItems();
  }

  /** Set a key:value tag on the Span. */
  @Override
  public Span setTag(String key, String value) {
    return this.setTagInternal(key, value);
  }

  /** Set a key:value tag on the Span for boolean values. */
  @Override
  public Span setTag(String key, boolean value) {
    return this.setTagInternal(key, value);
  }

  /** Set a key:value tag on the Span for numeric values. */
  @Override
  public Span setTag(String key, Number value) {
    return this.setTagInternal(key, value);
  }

  @Synchronized
  private synchronized Span setTagInternal(String key, Object value) {
    if (this.tags == null) {
      this.tags = new LinkedHashMap<>();
    }
    this.tags.put(key, value);
    return this;
  }

  /**
   * Log key:value pairs to the Span with the current walltime timestamp.
   *
   * <p><strong>CAUTIONARY NOTE:</strong> not all Tracer implementations support key:value log
   * fields end-to-end. Caveat emptor.
   *
   * <p>A contrived example (using Guava, which is not required):
   *
   * <pre><code>
   * span.log(
   * ImmutableMap.Builder&lt;String, Object&gt;()
   * .put("event", "soft error")
   * .put("type", "cache timeout")
   * .put("waited.millis", 1500)
   * .build());
   * </code></pre>
   *
   * @param fields key:value log fields. Tracer implementations should support String, numeric, and
   *     boolean values; some may also support arbitrary Objects.
   * @return the Span, for chaining
   * @see Span#log(String)
   */
  @Override
  public Span log(Map<String, ?> fields) {
    return this.log(new Log(fields));
  }

  /**
   * Like log(Map&lt;String, Object&gt;), but with an explicit timestamp.
   *
   * <p><strong>CAUTIONARY NOTE:</strong> not all Tracer implementations support key:value log
   * fields end-to-end. Caveat emptor.
   *
   * @param timestampMicros The explicit timestamp for the log record. Must be greater than or equal
   *     to the Span's start timestamp.
   * @param fields key:value log fields. Tracer implementations should support String, numeric, and
   *     boolean values; some may also support arbitrary Objects.
   * @return the Span, for chaining
   * @see Span#log(long, String)
   */
  @Override
  public Span log(long timestampMicros, Map<String, ?> fields) {
    return this.log(new Log(floorDiv(timestampMicros, 1000), fields));
  }

  /**
   * Record an event at the current walltime timestamp.
   *
   * <p>Shorthand for
   *
   * <pre><code>
   * span.log(Collections.singletonMap("event", event));
   * </code></pre>
   *
   * @param event the event value; often a stable identifier for a moment in the Span lifecycle
   * @return the Span, for chaining
   */
  @Override
  public Span log(String event) {
    return this.log(new Log(event));
  }

  /**
   * Record an event at a specific timestamp.
   *
   * <p>Shorthand for
   *
   * <pre><code>
   * span.log(timestampMicroseconds, Collections.singletonMap("event", event));
   * </code></pre>
   *
   * @param timestampMicros The explicit timestamp for the log record. Must be greater than or equal
   *     to the Span's start timestamp.
   * @param event the event value; often a stable identifier for a moment in the Span lifecycle
   * @return the Span, for chaining
   */
  @Override
  public Span log(long timestampMicros, String event) {
    return this.log(new Log(floorDiv(timestampMicros, 1000), event));
  }

  @Synchronized
  private Span log(Log log) {
    this.tracer.logger().log(this, log);
    return this;
  }

  /**
   * Sets a baggage item in the Span (and its SpanContext) as a key/value pair.
   *
   * <p>Baggage enables powerful distributed context propagation functionality where arbitrary
   * application data can be carried along the full path of request execution throughout the system.
   *
   * <p>Note 1: Baggage is only propagated to the future (recursive) children of this SpanContext.
   *
   * <p>Note 2: Baggage is sent in-band with every subsequent local and remote calls, so this
   * feature must be used with care.
   *
   * @return this Span instance, for chaining
   */
  @Override
  public Span setBaggageItem(String key, String value) {
    this.context.setBaggageItem(key, value);
    return this;
  }

  /**
   * Get a baggage item based on key.
   *
   * @return the value of the baggage item identified by the given key, or null if no such item
   *     could be found
   */
  @Override
  public String getBaggageItem(String key) {
    return this.context.getBaggageItem(key);
  }

  /**
   * Sets the string name for the logical operation this span represents.
   *
   * <p>NOTE: this is not a thread safe operation.
   *
   * @return this Span instance, for chaining
   */
  @Override
  public Span setOperationName(String operationName) {
    this.operationName = operationName;
    return this;
  }

  /**
   * Sets the end timestamp to now and records the span.
   *
   * <p>With the exception of calls to {@link #context}, this should be the last call made to the
   * span instance. Future calls to {@link #finish} are defined as noops, and future calls to
   * methods other than {@link #context} lead to undefined behavior.
   *
   * @see Span#context()
   */
  @Override
  public void finish() {
    this.finishInternal(Tools.nowMillis());
  }

  /**
   * Sets an explicit end timestamp and records the span.
   *
   * <p>With the exception of calls to Span.context(), this should be the last call made to the span
   * instance, and to do otherwise leads to undefined behavior.
   *
   * @param finishMicros an explicit finish time, in microseconds since the epoch
   * @see Span#context()
   */
  @Override
  public void finish(long finishMicros) {
    this.finishInternal(floorDiv(finishMicros, 1000));
  }

  @Synchronized
  private void finishInternal(long finishMillis) {
    this.finished = true;
    this.finishMillis = finishMillis;
    this.duration = finishMillis - this.startMillis;
    this.tracer.logger().finish(this, new Log(finishMillis, "Stop-Span"));
  }
}
