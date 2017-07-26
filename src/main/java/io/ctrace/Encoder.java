package io.ctrace;

/**
 * Created by a50r on 7/14/17.
 */
public interface Encoder {
    byte[] Encode(Span span);
}
