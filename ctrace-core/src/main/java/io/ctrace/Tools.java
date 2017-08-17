package io.ctrace;

import java.security.SecureRandom;
import java.util.Map;
import lombok.val;

public final class Tools {
  private static final char[] hexArray = "0123456789abcdef".toCharArray();

  private static class Holder {
    static final SecureRandom numberGenerator = new SecureRandom();
  }

  /**
   * Generate new ctrace id.
   *
   * @return new id.
   */
  public static String newId() {
    byte[] randomBytes = new byte[8];
    val ng = Holder.numberGenerator;
    ng.nextBytes(randomBytes);
    return bytesToHex(randomBytes);
  }

  /**
   * Convert bytes to hex.
   *
   * @param bytes bytes to convert
   * @return hex string
   */
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static String longToHex(long l) {
    return Long.toHexString(l);
  }

  static long nowMicros() {
    return System.currentTimeMillis() * 1000;
  }

  static long nowMillis() {
    return System.currentTimeMillis();
  }
}
