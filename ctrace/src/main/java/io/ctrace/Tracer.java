package io.ctrace;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.propagation.Format;
import io.opentracing.util.ThreadLocalScopeManager;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Synchronized;
import lombok.experimental.Accessors;
import lombok.val;

/**
 * Tracer is a simple, thin interface for Span creation and propagation across arbitrary transports.
 */
@Accessors(fluent = true)
public final class Tracer implements io.opentracing.Tracer {

  private static final String SERVICE_NAME_VAR = "ctrace_service_name";
  private static final ImmutableSet<String> TRACE_ID_EXTRACT_HEADERS =
      ImmutableSet.of("x-b3-traceid");
  private static final ImmutableSet<String> SPAN_ID_EXTRACT_HEADERS =
      ImmutableSet.of("x-b3-spanid");
  private static final ImmutableSet<String> TRACE_ID_INJECT_HEADERS =
      ImmutableSet.of("X-B3-Traceid");
  private static final ImmutableSet<String> SPAN_ID_INJECT_HEADERS = ImmutableSet.of("X-B3-Spanid");

  private final Propagator propagator;

  private final Logger logger;
  private final String serviceName;
  @Getter private final ScopeManager scopeManager;

  public Tracer() {
    this(null);
  }

  public Tracer(String serviceName) {
    this(null, null, null, serviceName, null, null, null, null, null);
  }

  @Builder
  private Tracer(
      Logger logger,
      OutputStream stream,
      Propagator propagator,
      String serviceName,
      ScopeManager scopeManager,
      @Singular ImmutableSet<String> traceIdExtractHeaders,
      @Singular ImmutableSet<String> spanIdExtractHeaders,
      @Singular ImmutableSet<String> traceIdInjectHeaders,
      @Singular ImmutableSet<String> spanIdInjectHeaders) {
    if (serviceName == null) {
      serviceName = System.getenv(SERVICE_NAME_VAR);
    }
    if (logger == null) {
      if (stream != null) {
        logger = new StreamLogger(stream, new JsonEncoder());
      } else {
        logger = new Slf4jLogger();
      }
    }

    if (propagator == null) {
      if (traceIdInjectHeaders == null) {
        traceIdInjectHeaders = TRACE_ID_INJECT_HEADERS;
      }
      if (spanIdInjectHeaders == null) {
        spanIdInjectHeaders = SPAN_ID_INJECT_HEADERS;
      }
      if (traceIdExtractHeaders == null) {
        traceIdExtractHeaders = TRACE_ID_EXTRACT_HEADERS;
      }
      if (spanIdExtractHeaders == null) {
        spanIdExtractHeaders = SPAN_ID_EXTRACT_HEADERS;
      }
      propagator =
          new Propagator(
              traceIdInjectHeaders,
              spanIdInjectHeaders,
              traceIdExtractHeaders
                  .stream()
                  .map(String::toLowerCase)
                  .collect(ImmutableSet.toImmutableSet()),
              spanIdExtractHeaders
                  .stream()
                  .map(String::toLowerCase)
                  .collect(ImmutableSet.toImmutableSet()));
    }

    if (scopeManager == null) {
      scopeManager = new ThreadLocalScopeManager();
    }

    Preconditions.checkArgument(serviceName != null, "Missing serviceName");
    this.logger = logger;
    this.propagator = propagator;
    this.scopeManager = scopeManager;
    this.serviceName = serviceName;
  }

  /**
   * Return a new SpanBuilder for a Span with the given `operationName`.
   *
   * <p>You can override the operationName later via {@link Span#setOperationName(String)}.
   *
   * <p>A contrived example:
   *
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
   *
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
   *
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

  String serviceName() {
    return this.serviceName;
  }

  Propagator propagator() {
    return this.propagator;
  }

  Logger logger() {
    return this.logger;
  }

  /**
   * Return the {@link Span active span}. This does not affect the internal reference count for the
   * {@link Span}.
   *
   * <p>If there is an {@link Span active span}, it becomes an implicit parent of any newly-created
   * {@link Span span} at {@link SpanBuilder#startActive(boolean)} time (rather than at {@link
   * Tracer#buildSpan(String)} time).
   *
   * @return the {@link Span active span}, or null if none could be found.
   */
  @Override
  public io.opentracing.Span activeSpan() {
    val scope = this.scopeManager.active();
    if (scope == null) {
      return null;
    }
    return scope.span();
  }

  private SpanContext activeSpanContext() {
    io.opentracing.Span handle = this.activeSpan();
    if (handle == null) {
      return null;
    }

    return (SpanContext) handle.context();
  }

  public class SpanBuilder implements io.opentracing.Tracer.SpanBuilder {

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
    public SpanBuilder asChildOf(io.opentracing.Span parent) {
      return this.asChildOf(parent.context());
    }

    /**
     * Add a reference from the Span being built to a distinct (usually parent) Span. May be called
     * multiple times to represent multiple such References.
     *
     * @param referenceType the reference type, typically one of the constants defined in References
     * @param referencedContext the SpanContext being referenced; e.g., for a References.CHILD_OF
     *     referenceType, the referencedContext is the parent. If referencedContext==null, the call
     *     to {@link #addReference} is a noop.
     * @see References
     */
    @Override
    public SpanBuilder addReference(
        String referenceType, io.opentracing.SpanContext referencedContext) {
      if (Objects.equals(referenceType, References.CHILD_OF)) {
        return this.asChildOf(referencedContext);
      }
      return this;
    }

    /**
     * Do not create an implicit {@link References#CHILD_OF} reference to the {@link
     * Tracer#activeSpan}).
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
     * Returns a newly started and activated {@link Scope}.
     *
     * <p>The returned {@link Scope} supports try-with-resources. For example:
     *
     * <pre><code>
     *     try (Scope scope = tracer.buildSpan("...").startActive(true)) {
     *         // (Do work)
     *         scope.span().setTag( ... );  // etc, etc
     *     }
     *     // Span does finishes automatically only when 'finishSpanOnClose' is true
     * </code></pre>
     *
     * <p>If
     *
     * <ul>
     *   <li>the {@link Tracer}'s {@link ScopeManager#active()} is not null, and
     *   <li>no <b>explicit</b> references are added via {@link SpanBuilder#addReference}, and
     *   <li>{@link SpanBuilder#ignoreActiveSpan()} is not invoked,
     * </ul>
     *
     * <p>... then an inferred {@link References#CHILD_OF} reference is created to the {@link
     * ScopeManager#active()}'s {@link SpanContext} when either {@link SpanBuilder#start()} or
     * {@link SpanBuilder#startActive} is invoked.
     *
     * <p>Note: {@link SpanBuilder#startActive(boolean)} is a shorthand for {@code
     * tracer.scopeManager().activate(spanBuilder.start(), finishSpanOnClose)}.
     *
     * @param finishSpanOnClose whether span should automatically be finished when {@link
     *     Scope#close()} is called
     * @return a {@link Scope}, already registered via the {@link ScopeManager}
     * @see ScopeManager
     * @see Scope
     */
    @Override
    public Scope startActive(boolean finishSpanOnClose) {
      return Tracer.this.scopeManager.activate(createSpan(), finishSpanOnClose);
    }

    /**
     * Start a span manually.
     *
     * @deprecated use {@link #start} or {@link #startActive} instead.
     */
    @Override
    public io.opentracing.Span startManual() {
      return start();
    }

    private Span createSpan() {
      if (this.startMicros == 0) {
        this.startMicros = Tools.nowMicros();
      }
      if (firstParent == null && !ignoringActiveSpan) {
        firstParent = activeSpanContext();
      }
      return new Span(Tracer.this, serviceName, operationName, startMicros, tags, firstParent);
    }

    /**
     * Like {@link SpanBuilder#startActive(boolean)}, but the returned {@link Span} has not been
     * registered via the {@link ScopeManager}.
     *
     * @return the newly-started Span instance, which has *not* been automatically registered via
     *     the {@link ScopeManager}
     * @see SpanBuilder#startActive(boolean)
     */
    @Override
    public io.opentracing.Span start() {
      return createSpan();
    }
  }
}
