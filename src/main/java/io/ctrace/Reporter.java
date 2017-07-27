package io.ctrace;

/**
 * Reporter encodes and reports the span to an output or transport.
 */
public interface Reporter {
    void report(Span span);
    void flush();
}
