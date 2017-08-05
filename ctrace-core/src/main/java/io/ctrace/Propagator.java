package io.ctrace;

import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Propagator handles injecting and extracting SpanContext to/from carriers such as HTTP Headers,
 * TextMaps, etc...
 */
public class Propagator {

  static final String SPAN_ID_MAP_KEY = "ct-span-id";
  static final String TRACE_ID_MAP_KEY = "ct-trace-id";
  static final String BAGGAGE_MAP_PREFIX = "ct-bag-";
  static final String SPAN_ID_HEADER_KEY = "Ct-Span-Id";
  static final String TRACE_ID_HEADER_KEY = "Ct-Trace-Id";
  static final String BAGGAGE_HEADER_PREFIX = "Ct-Bag-";
  protected final String[] traceIdInjectHeaders;
  protected final String[] spanIdInjectHeaders;
  protected Set<String> traceIdExtractHeaders;
  protected Set<String> spanIdExtractHeaders;

  protected Propagator(
      String[] traceIdExtractHeaders,
      String[] spanIdExtractHeaders,
      String[] traceIdInjectHeaders,
      String[] spanIdInjectHeaders) {
    if (traceIdExtractHeaders != null) {
      this.traceIdExtractHeaders = new HashSet<>(Stream.of(traceIdExtractHeaders)
          .map(s -> s.toLowerCase())
          .collect(Collectors.toList()));
    }
    if (spanIdExtractHeaders != null) {
      this.spanIdExtractHeaders = new HashSet<>(Stream.of(spanIdExtractHeaders)
          .map(s -> s.toLowerCase())
          .collect(Collectors.toList()));
    }
    this.traceIdInjectHeaders = traceIdInjectHeaders;
    this.spanIdInjectHeaders = spanIdInjectHeaders;
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
  public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
    if (carrier instanceof TextMap) {
      TextMap textMap = (TextMap) carrier;
      Iterable<Map.Entry<String, String>> baggage = spanContext.baggageItems();
      if (format == Format.Builtin.HTTP_HEADERS) {
        if (baggage != null) {
          for (Map.Entry<String, String> entry : spanContext.baggageItems()) {
            textMap.put(BAGGAGE_HEADER_PREFIX + entry.getKey(), entry.getValue());
          }
        }
        textMap.put(SPAN_ID_HEADER_KEY, spanContext.spanId());
        textMap.put(TRACE_ID_HEADER_KEY, spanContext.traceId());
        if (this.traceIdInjectHeaders != null) {
          for (String header : this.traceIdInjectHeaders) {
            textMap.put(header, spanContext.traceId());
          }
        }
        if (this.spanIdInjectHeaders != null) {
          for (String header : this.spanIdInjectHeaders) {
            textMap.put(header, spanContext.spanId());
          }
        }
      } else if (format == Format.Builtin.TEXT_MAP) {
        if (baggage != null) {
          for (Map.Entry<String, String> entry : spanContext.baggageItems()) {
            textMap.put(BAGGAGE_MAP_PREFIX + entry.getKey(), entry.getValue());
          }
        }
        textMap.put(SPAN_ID_MAP_KEY, String.valueOf(spanContext.spanId()));
        textMap.put(TRACE_ID_MAP_KEY, String.valueOf(spanContext.traceId()));
      } else {
        throw new IllegalArgumentException("Unknown format");
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

        if (TRACE_ID_MAP_KEY.equals(lowerKey)) {
          traceId = entry.getValue();
        } else if (SPAN_ID_MAP_KEY.equals(lowerKey)) {
          spanId = entry.getValue();
        } else if (lowerKey.startsWith(BAGGAGE_MAP_PREFIX)) {
          String bagKey = key.substring((BAGGAGE_MAP_PREFIX.length()));
          baggage.put(bagKey, entry.getValue());
        }
      }

      if (format == Format.Builtin.HTTP_HEADERS) {
        for (Map.Entry<String, String> entry : textMap) {
          String key = entry.getKey()
              .toLowerCase();

          if (traceId == null
              && this.traceIdExtractHeaders != null
              && this.traceIdExtractHeaders.contains(key)) {
            traceId = entry.getValue();
          } else if (spanId == null
              && this.spanIdExtractHeaders != null
              && this.spanIdExtractHeaders.contains(key)) {
            spanId = entry.getValue();
          }
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
