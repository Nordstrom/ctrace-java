package io.ctrace;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

/**
 * Propagator handles injecting and extracting SpanContext to/from carriers such as HTTP Headers,
 * TextMaps, etc...
 */
@Accessors(fluent = true)
public class Propagator {
  private static final String BAGGAGE_PREFIX = "X-B3-Bag-";
  private static final ImmutableSet<String> TRACE_ID_HEADERS = ImmutableSet.of("X-B3-Traceid");
  private static final ImmutableSet<String> SPAN_ID_HEADERS = ImmutableSet.of("X-B3-Spanid");

  @Getter private final Set<String> traceIdInjectHeaders;
  @Getter private final Set<String> spanIdInjectHeaders;
  @Getter private final String baggageInjectPrefix;
  @Getter private final Set<String> traceIdExtractHeaders;
  @Getter private final Set<String> spanIdExtractHeaders;
  @Getter private final String baggageExtractPrefix;

  /** No arg Propagator constructor. */
  public Propagator() {
    this(null, null, null, null, null, null);
  }

  /**
   * All args Propagator constructor.
   *
   * @param traceIdInjectHeaders traceId inject header set
   * @param spanIdInjectHeaders spanId inject header set
   * @param baggageInjectPrefix baggage inject prefix
   * @param traceIdExtractHeaders traceId extract header set
   * @param spanIdExtractHeaders spanId extract header set
   * @param baggageExtractPrefix baggage extract prefix
   */
  public Propagator(
      Set<String> traceIdInjectHeaders,
      Set<String> spanIdInjectHeaders,
      String baggageInjectPrefix,
      Set<String> traceIdExtractHeaders,
      Set<String> spanIdExtractHeaders,
      String baggageExtractPrefix) {
    if (traceIdInjectHeaders == null || traceIdInjectHeaders.isEmpty()) {
      traceIdInjectHeaders = TRACE_ID_HEADERS;
    }
    if (spanIdInjectHeaders == null || spanIdInjectHeaders.isEmpty()) {
      spanIdInjectHeaders = SPAN_ID_HEADERS;
    }
    if (traceIdExtractHeaders == null || traceIdExtractHeaders.isEmpty()) {
      traceIdExtractHeaders = TRACE_ID_HEADERS;
    }
    if (spanIdExtractHeaders == null || spanIdExtractHeaders.isEmpty()) {
      spanIdExtractHeaders = SPAN_ID_HEADERS;
    }
    if (Strings.isNullOrEmpty(baggageExtractPrefix)) {
      baggageExtractPrefix = BAGGAGE_PREFIX;
    }
    if (Strings.isNullOrEmpty(baggageInjectPrefix)) {
      baggageInjectPrefix = BAGGAGE_PREFIX;
    }

    this.traceIdInjectHeaders = traceIdInjectHeaders;
    this.spanIdInjectHeaders = spanIdInjectHeaders;
    this.baggageInjectPrefix = baggageInjectPrefix;
    this.traceIdExtractHeaders = toLowerCase(traceIdExtractHeaders);
    this.spanIdExtractHeaders = toLowerCase(spanIdExtractHeaders);
    this.baggageExtractPrefix = baggageExtractPrefix.toLowerCase();
  }

  private static ImmutableSet<String> toLowerCase(Set<String> set) {
    return set.stream().map(String::toLowerCase).collect(ImmutableSet.toImmutableSet());
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
  public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
    if (carrier instanceof TextMap) {
      TextMap textMap = (TextMap) carrier;
      Iterable<Map.Entry<String, String>> baggage = spanContext.baggageItems();
      if (baggage != null) {
        for (Map.Entry<String, String> entry : spanContext.baggageItems()) {
          textMap.put(baggageInjectPrefix + entry.getKey(), entry.getValue());
        }
      }
      for (String header : traceIdInjectHeaders) {
        textMap.put(header, spanContext.traceId());
      }
      for (String header : spanIdInjectHeaders) {
        textMap.put(header, spanContext.spanId());
      }
    } else {
      throw new IllegalArgumentException("Unknown carrier");
    }
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
  public <C> SpanContext extract(Format<C> format, C carrier) {
    String traceId = null;
    String spanId = null;
    Map<String, String> baggage = new HashMap<>();

    if (!(carrier instanceof TextMap)) {
      throw new IllegalArgumentException("Unknown carrier");
    }
    TextMap textMap = (TextMap) carrier;
    for (val entry : textMap) {
      val key = entry.getKey();
      val lowerKey = key.toLowerCase();

      if (traceId == null && this.traceIdExtractHeaders.contains(lowerKey)) {
        traceId = entry.getValue();
      } else if (spanId == null && this.spanIdExtractHeaders.contains(lowerKey)) {
        spanId = entry.getValue();
      }
      if (lowerKey.startsWith(baggageExtractPrefix)) {
        val bagKey = key.substring((baggageExtractPrefix.length()));
        baggage.put(bagKey, entry.getValue());
      }
    }

    if (traceId != null) {
      // We at least need a traceId
      return new SpanContext(traceId, spanId, baggage);
    }

    return null;
  }
}
