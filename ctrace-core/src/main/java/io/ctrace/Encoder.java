package io.ctrace;

import com.sun.xml.internal.bind.v2.runtime.output.Encoded;

public interface Encoder {

  /**
   * Encode a span into a string.
   *
   * @param span - span data to encode
   * @param log - log entry to encode
   * @return encoded string.
   */
  String encodeToString(Span span, Log log);

  /**
   * Encode a span into an array of bytes.
   *
   * @param span - span data to encode
   * @param log - log entry to encode
   * @return encoded array of bytes.
   */
  byte[] encodeToBytes(Span span, Log log);

  PreEncodedSpan preEncode(Span span);

  String encodeToString(PreEncodedSpan span, Log log);
  byte[] encodeToBytes(PreEncodedSpan span, Log log);
}
