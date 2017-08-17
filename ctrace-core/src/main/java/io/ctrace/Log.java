package io.ctrace;

import java.util.Map;

public final class Log {

  private final long timestampMillis;
  private final Field[] fields;

  /**
   * Constructor.
   *
   * @param timestampMillis timestamp in epoch milliseconds
   * @param event log event
   */
  public Log(long timestampMillis, String event) {
    this(timestampMillis, new Field[]{new Field<>("event", event)});
  }


  /**
   * Constructor.
   *
   * @param event log event
   */
  public Log(String event) {
    this(Tools.nowMillis(), new Field[]{new Field<>("event", event)});
  }

  /**
   * Constructor.
   * @param event log event
   * @param payload log payload
   */
  public Log(String event, Object payload) {
    this(Tools.nowMillis(), new Field[]{
        new Field<>("event", event),
        new Field<>("payload", payload)
    });
  }

  /**
   * Constructor.
   * @param timestampMillis timestamp in epoch milliseconds
   * @param event log event
   * @param payload log payload
   */
  public Log(long timestampMillis, String event, Object payload) {
    this(timestampMillis, new Field[]{
        new Field<>("event", event),
        new Field<>("payload", payload)
    });
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
    this.fields = new Field[fields.size()];
    int i = 0;
    for (Map.Entry<String, ?> entry : fields.entrySet()) {
      this.fields[i++] = new Field<>(entry.getKey(), entry.getValue());
    }
  }

  Log(long timestampMillis, Field[] fields) {
    this.timestampMillis = timestampMillis;
    this.fields = fields;
  }

  public long timestampMillis() {
    return this.timestampMillis;
  }

  public Field[] fields() {
    return fields;
  }

  public static class Field<T> {

    private String key;
    private T value;

    public Field(String key, T value) {
      this.key = key;
      this.value = value;
    }

    public String key() {
      return this.key;
    }

    public T value() {
      return this.value;
    }
  }
}

