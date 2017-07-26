package io.ctrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Span implements io.opentracing.Span {
    private Tracer tracer;
    private String serviceName;
    private String operationName;
    private SpanContext context;
    private String parentId;
    private long startMicros;
    private long finishMicros;
    private long duration;
    private String encodedPrefix;
    private Map<String, Object> tags;
    private ArrayList<LogEntry> logs;
    private LogEntry log;

    Span(Tracer tracer,
         String serviceName,
         String operationName,
         long startMicros,
         Map<String, Object> tags,
         SpanContext parentContext) {
        this.tracer = tracer;
        this.serviceName = serviceName;
        this.operationName = operationName;
        this.startMicros = startMicros;
        this.tags = tags;

        if (parentContext != null) {
            this.context = new SpanContext(parentContext);
            this.parentId = parentContext.spanId();
        } else {
            this.context = new SpanContext();
        }
        this.log(startMicros, "Start-Span");
    }

    /**
     * Retrieve the associated SpanContext.
     * <p>
     * This may be called at any time, including after calls to finish().
     *
     * @return the SpanContext that encapsulates Span state that should propagate across process boundaries.
     */
    @Override
    public SpanContext context() {
        return this.context;
    }

    /**
     * Get trace id
     *
     * @return traceId
     */
    public String traceId() {
        return this.context.traceId();
    }

    /**
     * Get span id
     *
     * @return spanId
     */
    public String spanId() {
        return this.context.spanId();
    }

    /**
     * Get parent span id
     *
     * @return parentId
     */
    String parentId() {
        return this.parentId;
    }


    String serviceName() {
        return this.serviceName;
    }

    /**
     * Get operation name for the logical operation this span represents.
     *
     * @return operationName
     */
    String operationName() {
        return this.operationName;
    }


    /**
     * Get start time microseconds
     *
     * @return startMicros
     */
    long startMicros() {
        return this.startMicros;
    }

    long finishMicros() {
        return this.finishMicros;
    }

    long duration() {
        return this.duration;
    }

    Iterable<Map.Entry<String, Object>> tagEntries() {
        if (this.tags == null) {
            return null;
        }
        return this.tags.entrySet();
    }

    Iterable<Map.Entry<String, String>> baggageItems() {
        return this.context.baggageItems();
    }

    Iterable<LogEntry> logEntries() {
        return this.logs;
    }

    LogEntry log() {
        return this.log;
    }

    Span setEncodedPrefix(String prefix) {
        this.encodedPrefix = prefix;
        return this;
    }

    String encodedPrefix() {
        return this.encodedPrefix;
    }

    /**
     * Set a key:value tag on the Span.
     *
     * @param key
     * @param value
     */
    @Override
    public Span setTag(String key, String value) {
        return this._setTag(key, value);
    }

    /**
     * Same as {@link #setTag(String, String)}, but for boolean values.
     *
     * @param key
     * @param value
     */
    @Override
    public Span setTag(String key, boolean value) {
        return this._setTag(key, value);
    }

    /**
     * Same as {@link #setTag(String, String)}, but for numeric values.
     *
     * @param key
     * @param value
     */
    @Override
    public Span setTag(String key, Number value) {
        return this._setTag(key, value);
    }

    private synchronized Span _setTag(String key, Object value) {
        if (this.tags == null) {
            this.tags = new HashMap<>();
        }
        this.tags.put(key, value);
        return this;
    }

    /**
     * Log key:value pairs to the Span with the current walltime timestamp.
     * <p>
     * <p><strong>CAUTIONARY NOTE:</strong> not all Tracer implementations support key:value log fields end-to-end.
     * Caveat emptor.
     * <p>
     * <p>A contrived example (using Guava, which is not required):
     * <pre><code>
     * span.log(
     * ImmutableMap.Builder<String, Object>()
     * .put("event", "soft error")
     * .put("type", "cache timeout")
     * .put("waited.millis", 1500)
     * .build());
     * </code></pre>
     *
     * @param fields key:value log fields. Tracer implementations should support String, numeric, and boolean values;
     *               some may also support arbitrary Objects.
     * @return the Span, for chaining
     * @see Span#log(String)
     */
    @Override
    public Span log(Map<String, ?> fields) {
        return this.log(Tools.nowMicros(), fields);
    }

    /**
     * Like log(Map&lt;String, Object&gt;), but with an explicit timestamp.
     * <p>
     * <p><strong>CAUTIONARY NOTE:</strong> not all Tracer implementations support key:value log fields end-to-end.
     * Caveat emptor.
     *
     * @param timestampMicros The explicit timestamp for the log record. Must be greater than or equal to the
     *                        Span's start timestamp.
     * @param fields          key:value log fields. Tracer implementations should support String, numeric, and boolean values;
     *                        some may also support arbitrary Objects.
     * @return the Span, for chaining
     * @see Span#log(long, String)
     */
    @Override
    public synchronized Span log(long timestampMicros, Map<String, ?> fields) {
        LogEntry logEntry = new LogEntry(timestampMicros, fields);
        if (this.tracer.singleSpanOutput()) {
            // Single Event: Add log to list.
            if (this.logs == null) {
                this.logs = new ArrayList<>();
            }
            this.logs.add(logEntry);
            return this;
        }

        // Multi Event:  Set single log and report.
        this.log = logEntry;
        this.tracer.report(this);
        return this;

    }

    /**
     * Record an event at the current walltime timestamp.
     * <p>
     * Shorthand for
     * <p>
     * <pre><code>
     * span.log(Collections.singletonMap("event", event));
     * </code></pre>
     *
     * @param event the event value; often a stable identifier for a moment in the Span lifecycle
     * @return the Span, for chaining
     */
    @Override
    public Span log(String event) {
        return this.log(Tools.nowMicros(), event);
    }

    /**
     * Record an event at a specific timestamp.
     * <p>
     * Shorthand for
     * <p>
     * <pre><code>
     * span.log(timestampMicroseconds, Collections.singletonMap("event", event));
     * </code></pre>
     *
     * @param timestampMicros The explicit timestamp for the log record. Must be greater than or equal to the
     *                        Span's start timestamp.
     * @param event           the event value; often a stable identifier for a moment in the Span lifecycle
     * @return the Span, for chaining
     */
    @Override
    public Span log(long timestampMicros, String event) {
        Map<String, String> fields = new HashMap<>(1);
        fields.put("event", event);
        return this.log(timestampMicros, fields);
    }

    /**
     * Sets a baggage item in the Span (and its SpanContext) as a key/value pair.
     * <p>
     * Baggage enables powerful distributed context propagation functionality where arbitrary application data can be
     * carried along the full path of request execution throughout the system.
     * <p>
     * Note 1: Baggage is only propagated to the future (recursive) children of this SpanContext.
     * <p>
     * Note 2: Baggage is sent in-band with every subsequent local and remote calls, so this feature must be used with
     * care.
     *
     * @param key
     * @param value
     * @return this Span instance, for chaining
     */
    @Override
    public Span setBaggageItem(String key, String value) {
        this.context.setBaggageItem(key, value);
        return this;
    }

    /**
     * @param key
     * @return the value of the baggage item identified by the given key, or null if no such item could be found
     */
    @Override
    public String getBaggageItem(String key) {
        return this.context.getBaggageItem(key);
    }

    /**
     * Sets the string name for the logical operation this span represents.
     *
     * @param operationName
     * @return this Span instance, for chaining
     */
    @Override
    public Span setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    /**
     * @param eventName
     * @param payload
     * @deprecated use {@link #log(Map)} like this
     * {@code span.log(Map.of("event", "timeout"))}
     * or
     * {@code span.log(timestampMicroseconds, Map.of("event", "exception", "payload", stackTrace))}
     */
    @Override
    public Span log(String eventName, Object payload) {
        return this.log(Tools.nowMicros(), eventName, payload);
    }

    /**
     * @param timestampMicroseconds
     * @param eventName
     * @param payload
     * @deprecated use {@link #log(Map)} like this
     * {@code span.log(timestampMicroseconds, Map.of("event", "timeout"))}
     * or
     * {@code span.log(timestampMicroseconds, Map.of("event", "exception", "payload", stackTrace))}
     */
    @Override
    public Span log(long timestampMicroseconds, String eventName, Object payload) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("event", eventName);
        if (payload != null) {
            fields.put("payload", payload);
        }
        return this.log(timestampMicroseconds, fields);
    }

    /**
     * Sets the end timestamp to now and records the span.
     * <p>
     * <p>With the exception of calls to {@link #context}, this should be the last call made to the span instance.
     * Future calls to {@link #finish} are defined as noops, and future calls to methods other than {@link #context}
     * lead to undefined behavior.
     *
     * @see Span#context()
     */
    @Override
    public void finish() {
        this.finish(Tools.nowMicros());
    }

    /**
     * Sets an explicit end timestamp and records the span.
     * <p>
     * <p>With the exception of calls to Span.context(), this should be the last call made to the span instance, and to
     * do otherwise leads to undefined behavior.
     *
     * @param finishMicros an explicit finish time, in microseconds since the epoch
     * @see Span#context()
     */
    @Override
    public void finish(long finishMicros) {
        this.finishMicros = finishMicros;
        this.duration = finishMicros - this.startMicros;
        this.log(finishMicros, "Stop-Span");
        this.tracer.report(this);
    }
}
