package io.ctrace;

/**
 * Key constants for use in Span mappings such as MDC, etc...
 */
public final class Keys {
  public static final String TRACE_ID = "ctrace-trace-id";
  public static final String SPAN_ID = "ctrace-span-id";
  public static final String PARENT_ID = "ctrace-parent-id";
  public static final String SERVICE = "ctrace-service";
  public static final String OPERATION = "ctrace-operation";
  public static final String START = "ctrace-start";
  public static final String FINISH = "ctrace-finish";
  public static final String DURATION = "ctrace-duration";
  public static final String TAGS = "ctrace-tags";
  public static final String BAGGAGE = "ctrace-bag";

  private Keys() {}
}
