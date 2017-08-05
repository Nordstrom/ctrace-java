package io.ctrace;

import java.io.IOException;
import java.io.OutputStream;

/**
 * StreamReporter encodes and reports the span to an output stream.
 */
public class StreamReporter implements Reporter {
  private OutputStream stream;
  private Encoder encoder;

  public StreamReporter(OutputStream stream, Encoder encoder) {
    this.stream = stream;
    this.encoder = encoder;
  }

  /**
   * Report the reportable item.  This can be a start, log, or finish by default.  In
   * if SingleSpanOutput = true, this will be the full span with all logs sent at finish.
   * @param reportable reportable span data.
   */
  public void report(Reportable reportable) {
    byte[] encoded = this.encoder.encodeToBytes(reportable);
    try {
      this.stream.write(encoded);
    } catch (IOException e) {
      // Do nothing at this point...
    }
  }

  /**
   * Flush the reporter.  In the case of output buffering this flushes the buffer.
   */
  public void flush() {
    try {
      this.stream.flush();
    } catch (IOException e) {
      // do nothing
    }
  }
}
