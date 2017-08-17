package io.ctrace;

public class PreEncodedSpan {
  public static final String START = "ctrace-encoded-start";
  public static final String FINISH = "ctrace-encoded-finish";
  public static final String TAGS = "ctrace-encoded-tags";
  public static final String BAGGAGE = "ctrace-encoded-baggage";

  private String start;
  private String finish;
  private String tags;
  private String baggage;

  /**
   * Constructor.
   *
   * @param start start section
   * @param finish finish section
   * @param tags tags section
   * @param baggage baggage section
   */
  public PreEncodedSpan(String start, String finish, String tags, String baggage) {
    this.start = start;
    this.finish = finish;
    this.tags = tags;
    this.baggage = baggage;
  }

  public String start() {
    return this.start;
  }

  public String finish() {
    return this.finish;
  }

  public String tags() {
    return this.tags;
  }

  public String baggage() {
    return this.baggage;
  }
}
