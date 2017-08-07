package io.ctrace;

import java.util.ArrayList;
import java.util.Map;

public abstract class AbstractReportable implements Reportable {

  protected String serviceName;
  protected String operationName;
  protected String parentId;
  protected long startMillis;
  protected long finishMillis;
  protected long duration;
  protected Map<String, Object> tags;
  protected ArrayList<LogEntry> logs;
  protected LogEntry log;
  private String prefix;

  protected AbstractReportable() {
  }

  protected AbstractReportable(String serviceName, String operationName, String parentId,
      long startMillis, long finishMillis, long duration,
      Map<String, Object> tags, ArrayList<LogEntry> logs, LogEntry log) {
    this.serviceName = serviceName;
    this.operationName = operationName;
    this.parentId = parentId;
    this.startMillis = startMillis;
    this.finishMillis = finishMillis;
    this.duration = duration;
    this.tags = tags;
    this.logs = logs;
    this.log = log;
  }

  @Override
  public String parentId() {
    return this.parentId;
  }

  @Override
  public String service() {
    return this.serviceName;
  }

  @Override
  public String operation() {
    return this.operationName;
  }

  @Override
  public long startMillis() {
    return this.startMillis;
  }

  @Override
  public long finishMillis() {
    return this.finishMillis;
  }

  @Override
  public long duration() {
    return this.duration;
  }

  @Override
  public Iterable<Map.Entry<String, Object>> tags() {
    if (AbstractReportable.this.tags == null) {
      return null;
    }
    return AbstractReportable.this.tags.entrySet();
  }

  @Override
  public Iterable<LogEntry> logs() {
    if (AbstractReportable.this.logs == null) {
      return null;
    }
    return AbstractReportable.this.logs;
  }

  @Override
  public String prefix() {
    return this.prefix;
  }

  @Override
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public LogEntry log() {
    return this.log;
  }
}
