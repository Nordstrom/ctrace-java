package io.ctrace;

/**
 * Reporter encodes and reports the span to an output or transport.
 */
public interface Reporter {

  /**
   * Report the reportable item.  This can be a start, log, or finish by default.  In
   * if SingleSpanOutput = true, this will be the full span with all logs sent at finish.
   * @param reportable reportable span data.
   */
  void report(Reportable reportable);

  /**
   * Flush the reporter.  In the case of output buffering this flushes the buffer.
   */
  void flush();
}
