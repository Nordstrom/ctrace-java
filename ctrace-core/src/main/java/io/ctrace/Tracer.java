package io.ctrace;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpanSource;
import io.opentracing.BaseSpan;
import io.opentracing.References;
import io.opentracing.propagation.Format;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Synchronized;


/**
 * Tracer is a simple, thin interface for Span creation and propagation across arbitrary
 * transports.
 */
public final class Tracer implements io.opentracing.Tracer {

  private static final String SERVICE_NAME_VAR = "ctrace_service_name";
  private static final String[] TRACE_ID_EXTRACT_HEADERS = new String[]{
      "x-correlation-id",
      "x_correlation_id",
      "correlation-id",
      "correlation_id",
      "correlationid",
      "x-trace-id",
      "x_trace_id",
      "trace-id",
      "trace_id",
      "traceid"
  };
  private static final String[] SPAN_ID_EXTRACT_HEADERS = new String[]{
      "x-request-id",
      "x_request_id",
      "request-id",
      "request_id",
      "requestid",
      "x-span-id",
      "x_span_id",
      "span-id",
      "span_id",
      "spanid"
  };

  private ActiveSpanSource spanSource = new ThreadLocalActiveSpanSource();
  private Propagator propagator;

  private Reporter reporter;
  private String serviceName;
  private boolean singleSpanOutput = true;

  public Tracer() {
    this(new TracerBuilder());
  }

  private Tracer(TracerBuilder builder) {
    if (builder.reporter != null) {
      this.reporter = builder.reporter;
    } else {
      this.reporter = new StreamReporter(builder.stream, new JsonEncoder());
    }

    if (builder.propagator != null) {
      this.propagator = builder.propagator;
    } else {
      this.propagator = new Propagator(builder.traceIdExtractHeaders,
          builder.spanIdExtractHeaders,
          builder.traceIdInjectHeaders,
          builder.spanIdInjectHeaders);
    }

    this.singleSpanOutput = builder.singleSpanOutput;
    this.serviceName = builder.serviceName;
  }

  public static TracerBuilder withReporter(Reporter reporter) {
    return new TracerBuilder().withReporter(reporter);
  }

  public static TracerBuilder withStream(OutputStream stream) {
    return new TracerBuilder().withStream(stream);
  }

  public static TracerBuilder withSingleSpanOutput(boolean singleSpanOutput) {
    return new TracerBuilder().withSingleSpanOutput(singleSpanOutput);
  }

  public static TracerBuilder withServiceName(String serviceName) {
    return new TracerBuilder().withServiceName(serviceName);
  }

  public static TracerBuilder withPropagator(Propagator propagator) {
    return new TracerBuilder().withPropagator(propagator);
  }

  public static TracerBuilder withTraceIdExtractHeaders(String... traceIdExtractHeaders) {
    return new TracerBuilder().withTraceIdExtractHeaders(traceIdExtractHeaders);
  }

  public static TracerBuilder withSpanIdExtractHeaders(String... spanIdExtractHeaders) {
    return new TracerBuilder().withSpanIdExtractHeaders(spanIdExtractHeaders);
  }

  public static TracerBuilder withTraceIdInjectHeaders(String... traceIdInjectHeaders) {
    return new TracerBuilder().withTraceIdInjectHeaders(traceIdInjectHeaders);
  }

  public static TracerBuilder withSpanIdInjectHeaders(String... spanIdInjectHeaders) {
    return new TracerBuilder().withSpanIdInjectHeaders(spanIdInjectHeaders);
  }

  /**
   * Return a new SpanBuilder for a Span with the given `operationName`.
   *
   * <p>You can override the
   * operationName later via {@link Span#setOperationName(String)}.
   *
   * <p>A contrived example:
   * <pre><code>
   *   Tracer tracer = ...
   *
   *   // Note: if there is a `tracer.activeSpan()`, it will be used as the target of an implicit
   *   //       CHILD_OF Reference for "workSpan" when `startActive()` is invoked.
   *   try (ActiveSpan workSpan = tracer.buildSpan("DoWork").startActive()) {
   *       workSpan.setTag("...", "...");
   *       // etc, etc
   *   }
   *
   *   // It's also possible to create Spans manually, bypassing the ActiveSpanSource activation.
   *   Span http = tracer.buildSpan("HandleHTTPRequest")
   *                     .asChildOf(rpcSpanContext)  // an explicit parent
   *                     .withTag("user_agent", req.UserAgent)
   *                     .withTag("lucky_number", 42)
   *                     .startManual();
   * </code></pre>
   *
   * @param operationName name of operation representing this span
   */
  @Override
  public SpanBuilder buildSpan(String operationName) {
    return new SpanBuilder(operationName);
  }

  /**
   * Inject a SpanContext into a `carrier` of a given type, presumably for propagation across
   * process boundaries.
   *
   * <p>Example:
   * <pre><code>
   * Tracer tracer = ...
   * Span clientSpan = ...
   * TextMap httpHeadersCarrier = new AnHttpHeaderCarrier(httpRequest);
   * tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, httpHeadersCarrier);
   * </code></pre>
   *
   * @param spanContext the SpanContext instance to inject into the carrier
   * @param format the Format of the carrier
   * @param carrier the carrier for the SpanContext state. All Tracer.inject() implementations must
   *     support io.opentracing.propagation.TextMap and java.nio.ByteBuffer.
   * @see Format
   * @see Format.Builtin
   */
  @Override
  public <C> void inject(io.opentracing.SpanContext spanContext, Format<C> format, C carrier) {
    this.propagator.inject((SpanContext) spanContext, format, carrier);
  }

  /**
   * Extract a SpanContext from a `carrier` of a given type, presumably after propagation across a
   * process boundary.
   *
   * <p>Example:
   * <pre><code>
   * Tracer tracer = ...
   * TextMap httpHeadersCarrier = new AnHttpHeaderCarrier(httpRequest);
   * SpanContext spanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, httpHeadersCarrier);
   * ... = tracer.buildSpan('...').asChildOf(spanCtx).startActive();
   * </code></pre>
   *
   * <p>If the span serialized state is invalid (corrupt, wrong version, etc) inside the carrier
   * this will result in an IllegalArgumentException.
   *
   * @param format the Format of the carrier
   * @param carrier the carrier for the SpanContext state. All Tracer.extract() implementations must
   *     support io.opentracing.propagation.TextMap and java.nio.ByteBuffer.
   * @return the SpanContext instance holding context to create a Span.
   * @see Format
   * @see Format.Builtin
   */
  @Override
  public <C> SpanContext extract(Format<C> format, C carrier) {
    return this.propagator.extract(format, carrier);
  }

  void report(Reportable reportable, boolean flush) {
    this.reporter.report(reportable);
    if (flush) {
      this.reporter.flush();
    }
  }

  boolean singleSpanOutput() {
    return this.singleSpanOutput;
  }

  String serviceName() {
    return this.serviceName;
  }

  Propagator propagator() {
    return this.propagator;
  }

  /**
   * Return the {@link ActiveSpan active span}. This does not affect the internal reference count
   * for the {@link ActiveSpan}.
   *
   * <p>If there is an {@link ActiveSpan active span}, it becomes
   * an implicit parent of any newly-created {@link BaseSpan span} at {@link
   * SpanBuilder#startActive()} time (rather than at {@link Tracer#buildSpan(String)} time).
   *
   * @return the {@link ActiveSpan active span}, or null if none could be found.
   */
  @Override
  public ActiveSpan activeSpan() {
    return this.spanSource.activeSpan();
  }

  /**
   * Wrap and "make active" a {@link Span} by encapsulating it – and any active state (e.g., MDC
   * state) in the current thread – in a new {@link ActiveSpan}.
   *
   * @param span the Span to wrap in an {@link ActiveSpan}
   * @return an {@link ActiveSpan} that encapsulates the given {@link Span} and any other
   *     {@link ActiveSpanSource}-specific context (e.g., the MDC context map)
   */
  @Override
  public ActiveSpan makeActive(io.opentracing.Span span) {
    return this.spanSource.makeActive(span);
  }

  private SpanContext activeSpanContext() {
    ActiveSpan handle = this.activeSpan();
    if (handle == null) {
      return null;
    }

    return (SpanContext) handle.context();
  }

  static class TracerBuilder {

    private Propagator propagator;
    private Reporter reporter;
    private OutputStream stream = new FileOutputStream(FileDescriptor.out);
    private String serviceName = System.getenv(SERVICE_NAME_VAR);
    private boolean singleSpanOutput;
    private String[] traceIdExtractHeaders = TRACE_ID_EXTRACT_HEADERS;
    private String[] spanIdExtractHeaders = SPAN_ID_EXTRACT_HEADERS;
    private String[] traceIdInjectHeaders = null;
    private String[] spanIdInjectHeaders = null;

    public TracerBuilder withStream(OutputStream stream) {
      this.stream = stream;
      return this;
    }

    public TracerBuilder withReporter(Reporter reporter) {
      this.reporter = reporter;
      return this;
    }

    public TracerBuilder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public TracerBuilder withSingleSpanOutput(boolean multiEvent) {
      this.singleSpanOutput = multiEvent;
      return this;
    }

    public TracerBuilder withPropagator(Propagator propagator) {
      this.propagator = propagator;
      return this;
    }

    public TracerBuilder withTraceIdExtractHeaders(String... traceIdExtractHeaders) {
      this.traceIdExtractHeaders = traceIdExtractHeaders;
      return this;
    }

    public TracerBuilder withSpanIdExtractHeaders(String... spanIdExtractHeaders) {
      this.spanIdExtractHeaders = spanIdExtractHeaders;
      return this;
    }

    public TracerBuilder withTraceIdInjectHeaders(String... traceIdInjectHeaders) {
      this.traceIdInjectHeaders = traceIdInjectHeaders;
      return this;
    }

    public TracerBuilder withSpanIdInjectHeaders(String... spanIdInjectHeaders) {
      this.spanIdInjectHeaders = spanIdInjectHeaders;
      return this;
    }

    public Tracer build() {
      return new Tracer(this);
    }
  }

  class SpanBuilder implements io.opentracing.Tracer.SpanBuilder {

    private final String operationName;
    private long startMicros;
    private SpanContext firstParent;
    private boolean ignoringActiveSpan = false;
    private Map<String, Object> tags;

    SpanBuilder(String operationName) {
      this.operationName = operationName;
    }

    /**
     * A shorthand for addReference(References.CHILD_OF, parent).
     *
     * <p>If parent==null, this is a noop.
     *
     * @param parent context of parent span
     */
    @Override
    @Synchronized
    public SpanBuilder asChildOf(io.opentracing.SpanContext parent) {
      this.firstParent = (SpanContext) parent;
      return this;
    }

    /**
     * A shorthand for addReference(References.CHILD_OF, parent.context()).
     *
     * <p>If parent==null, this is a noop.
     *
     * @param parent parent span
     */
    @Override
    public SpanBuilder asChildOf(BaseSpan<?> parent) {
      return this.asChildOf(parent.context());
    }

    /**
     * Add a reference from the Span being built to a distinct (usually parent) Span. May be called
     * multiple times to represent multiple such References.
     *
     * <p>If
     * <ul>
     *   <li>the {@link Tracer}'s {@link ActiveSpanSource#activeSpan()} is not null, and</li>
     *   <li>no <b>explicit</b> references are added via {@link SpanBuilder#addReference}, and</li>
     *   <li>{@link SpanBuilder#ignoreActiveSpan()} is not invoked,</li>
     * </ul> ... then an inferred {@link References#CHILD_OF} reference is created to the
     * {@link ActiveSpanSource#activeSpan()} {@link SpanContext} when either
     * {@link SpanBuilder#startActive()} or {@link SpanBuilder#startManual} is invoked.
     *
     * @param referenceType the reference type, typically one of the constants defined in
     *     References
     * @param referencedContext the SpanContext being referenced; e.g., for a References.CHILD_OF
     *     referenceType, the referencedContext is the parent. If referencedContext==null,
     *     the call to {@link #addReference} is a noop.
     * @see References
     */
    @Override
    public SpanBuilder addReference(String referenceType,
        io.opentracing.SpanContext referencedContext) {
      if (Objects.equals(referenceType, References.CHILD_OF)) {
        return this.asChildOf(referencedContext);
      }
      return this;
    }

    /**
     * Do not create an implicit {@link References#CHILD_OF} reference to the {@link
     * ActiveSpanSource#activeSpan}).
     */
    @Override
    public SpanBuilder ignoreActiveSpan() {
      this.ignoringActiveSpan = true;
      return this;
    }

    /**
     * Same as {@link Span#setTag(String, String)}, but for the span being built.
     *
     * @param key tag key
     * @param value tag value
     */
    @Override
    public SpanBuilder withTag(String key, String value) {
      return this.withTagInternal(key, value);
    }

    /**
     * Same as {@link Span#setTag(String, boolean)}, but for the span being built.
     *
     * @param key tag key
     * @param value tag value
     */
    @Override
    public SpanBuilder withTag(String key, boolean value) {
      return this.withTagInternal(key, value);
    }

    /**
     * Same as {@link Span#setTag(String, Number)}, but for the span being built.
     *
     * @param key tag key
     * @param value tag value
     */
    @Override
    public SpanBuilder withTag(String key, Number value) {
      return this.withTagInternal(key, value);
    }

    private <T> SpanBuilder withTagInternal(String key, T value) {
      if (this.tags == null) {
        this.tags = new HashMap<>();
      }
      this.tags.put(key, value);
      return this;
    }

    /**
     * Specify a timestamp of when the Span was started, represented in microseconds since epoch.
     *
     * @param microseconds start timestamp microseconds
     */
    @Override
    public SpanBuilder withStartTimestamp(long microseconds) {
      this.startMicros = microseconds;
      return this;
    }

    /**
     * Returns a newly started and activated {@link ActiveSpan}.
     *
     * <p>The returned {@link ActiveSpan} supports try-with-resources. For example:
     * <pre><code>
     *     try (ActiveSpan span = tracer.buildSpan("...").startActive()) {
     *         // (Do work)
     *         span.setTag( ... );  // etc, etc
     *     }  // Span finishes automatically unless deferred via {@link ActiveSpan#capture}
     * </code></pre>
     *
     * <p>If
     * <ul>
     *   <li>the {@link Tracer}'s {@link ActiveSpanSource#activeSpan()} is not null, and</li>
     *   <li>no <b>explicit</b> references are added via {@link SpanBuilder#addReference}, and</li>
     *   <li>{@link SpanBuilder#ignoreActiveSpan()} is not invoked,</li>
     * </ul> ... then an inferred {@link References#CHILD_OF} reference is created to the
     * {@link ActiveSpanSource#activeSpan()}'s
     * {@link SpanContext} when either {@link SpanBuilder#startManual()} or
     * {@link SpanBuilder#startActive} is invoked.
     *
     * <p>Note: {@link SpanBuilder#startActive()} is a shorthand for
     * {@code tracer.makeActive(spanBuilder.startManual())}.
     *
     * @return an {@link ActiveSpan}, already registered via the {@link ActiveSpanSource}
     * @see ActiveSpanSource
     * @see ActiveSpan
     */
    @Override
    public ActiveSpan startActive() {
      return Tracer.this.makeActive(this.createSpan());
    }

    /**
     * Like {@link #startActive()}, but the returned {@link Span} has not been registered via the
     * {@link ActiveSpanSource}.
     *
     * @return the newly-started Span instance, which has *not* been automatically registered via
     *     the {@link ActiveSpanSource}
     * @see SpanBuilder#startActive()
     */
    @Override
    public Span startManual() {
      return this.createSpan();
    }

    private Span createSpan() {
      if (this.startMicros == 0) {
        this.startMicros = Tools.nowMicros();
      }
      if (firstParent == null && !ignoringActiveSpan) {
        firstParent = activeSpanContext();
      }
      return new Span(Tracer.this,
          serviceName,
          operationName,
          startMicros,
          tags,
          firstParent);
    }

    /**
     * @deprecated use {@link #startManual} or {@link #startActive} instead.
     */
    @Override
    public Span start() {
      return this.startManual();
    }
  }
}
