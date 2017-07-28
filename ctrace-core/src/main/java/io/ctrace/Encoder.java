package io.ctrace;

/**
 * Created by a50r on 7/14/17.
 */
public interface Encoder {
    /**
     * Encode a span into an array of bytes.
     * @param span - span to encode
     * @return array of encoded bytes.
     */
    byte[] encode(Span span);
}
