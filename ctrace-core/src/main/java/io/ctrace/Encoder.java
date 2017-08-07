package io.ctrace;

public interface Encoder {

  /**
   * Encode a span into an array of bytes.
   *
   * @param e - span data to encode
   * @return array of encoded bytes.
   */
  String encodeToString(Encodable e);

  byte[] encodeToBytes(Encodable e);

  String encodeTags(Encodable e);

  String encodeBaggage(Encodable e);
}
