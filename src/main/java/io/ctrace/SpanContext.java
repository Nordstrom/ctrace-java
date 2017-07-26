package io.ctrace;

import java.util.HashMap;
import java.util.Map;

public class SpanContext implements io.opentracing.SpanContext {
    private String traceId;
    private String spanId;
    private Map<String, String> baggage;

    SpanContext() {
        this.traceId = Tools.newId();
        this.spanId = Tools.newId();
    }

    SpanContext(String traceId, String spanId, Map<String, String> baggage) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.baggage = baggage;
    }

    SpanContext(SpanContext parentContext) {
        this.traceId = parentContext.traceId;
        this.spanId = Tools.newId();

        if (parentContext.baggage != null) {
            this.baggage = new HashMap<>();
            for (Map.Entry<String, String> entry : parentContext.baggage.entrySet()) {
                this.baggage.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public String traceId() {
        return this.traceId;
    }

    public String spanId() {
        return this.spanId;
    }

    void setBaggageItem(String key, String value) {
        if (this.baggage == null) {
            this.baggage = new HashMap<>();
        }
        this.baggage.put(key, value);
    }

    String getBaggageItem(String key) {
        if (this.baggage == null) {
            return null;
        }
        return this.baggage.get(key);
    }

    /**
     * @return all zero or more baggage items propagating along with the associated Span
     * @see Span#setBaggageItem(String, String)
     * @see Span#getBaggageItem(String)
     */
    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        if (this.baggage == null) {
            return null;
        }
        return this.baggage.entrySet();
    }
}
