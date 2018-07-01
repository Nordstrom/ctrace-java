package io.ctrace;

import com.google.common.collect.ImmutableMap;
import io.opentracing.log.Fields;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class Log {

  @Getter private final long timestampMillis;
  @Getter private final Map<String, ?> fields;
  @Getter private final String message;
  @Getter private String messageKey;

  /**
   * Constructor.
   *
   * @param timestampMillis timestamp in epoch milliseconds
   * @param event log event
   */
  public Log(long timestampMillis, String event) {
    this(timestampMillis, ImmutableMap.of("event", event));
  }

  /**
   * Constructor.
   *
   * @param event log event
   */
  public Log(String event) {
    this(Tools.nowMillis(), ImmutableMap.of("event", event));
  }

  /**
   * Constructor.
   *
   * @param fields - map of log fields
   */
  public Log(Map<String, ?> fields) {
    this(Tools.nowMillis(), fields);
  }

  /**
   * Constructor.
   *
   * @param timestampMillis timestamp in epoch milliseconds
   * @param fields log fields
   */
  public Log(long timestampMillis, Map<String, ?> fields) {
    this.timestampMillis = timestampMillis;
    this.fields = fields;
    this.messageKey = Fields.MESSAGE;
    Object message = fields.get(this.messageKey);
    if (message == null) {
      this.messageKey = Fields.EVENT;
      message = fields.get(this.messageKey);
    }
    if (message == null) {
      message = "log";
    }
    this.message = message.toString();
  }
}
