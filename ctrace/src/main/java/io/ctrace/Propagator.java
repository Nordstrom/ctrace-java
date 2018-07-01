package io.ctrace;

import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Propagator handles injecting and extracting SpanContext to/from carriers such as HTTP Headers,
 * TextMaps, etc...
 */
@AllArgsConstructor
@Accessors(fluent = true)
public class Propagator {

  //  static final String SPAN_ID_MAP_KEY = "ct-span-id";
  //  static final String TRACE_ID_MAP_KEY = "ct-trace-id";
  //  static final String BAGGAGE_MAP_PREFIX = "ct-bag-";
  //  static final String SPAN_ID_HEADER_KEY = "Ct-Span-Id";
  //  static final String TRACE_ID_HEADER_KEY = "Ct-Trace-Id";
  static final String BAGGAGE_PREFIX = "Bag-";
  @Getter private final Set<String> traceIdInjectHeaders;
  @Getter private final Set<String> spanIdInjectHeaders;
  @Getter private final Set<String> traceIdExtractHeaders;
  @Getter private final Set<String> spanIdExtractHeaders;

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
          textMap.put(BAGGAGE_PREFIX + entry.getKey(), entry.getValue());
        }
      }
      for (String header : this.traceIdInjectHeaders) {
        textMap.put(header, spanContext.traceId());
      }
      for (String header : this.spanIdInjectHeaders) {
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

    if (carrier instanceof TextMap) {
      TextMap textMap = (TextMap) carrier;
      for (Map.Entry<String, String> entry : textMap) {
        String key = entry.getKey();
        String lowerKey = key.toLowerCase();

        if (lowerKey.startsWith(BAGGAGE_PREFIX)) {
          String bagKey = key.substring((BAGGAGE_PREFIX.length()));
          baggage.put(bagKey, entry.getValue());
        }
      }

      for (Map.Entry<String, String> entry : textMap) {
        String key = entry.getKey().toLowerCase();

        if (traceId == null && this.traceIdExtractHeaders.contains(key)) {
          traceId = entry.getValue();
        } else if (spanId == null && this.spanIdExtractHeaders.contains(key)) {
          spanId = entry.getValue();
        }
      }
    } else {
      throw new IllegalArgumentException("Unknown carrier");
    }

    if (traceId != null) {
      // We at least need a traceId
      return new SpanContext(traceId, spanId, baggage);
    }

    return null;
  }
}
