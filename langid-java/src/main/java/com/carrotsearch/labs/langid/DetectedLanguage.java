package com.carrotsearch.labs.langid;

/**
 * Mutable {@link DetectedLanguage} so that we can reuse instances
 * if needed.
 */
public final class DetectedLanguage implements Cloneable {
  public String langCode;
  public float confidence;

  public DetectedLanguage(String lang, float confidence) {
    this.langCode = lang;
    this.confidence = confidence;
  }

  public String getLangCode() {
    return langCode;
  }

  public double getConfidence() {
    return confidence;
  }
  
  @Override
  protected DetectedLanguage clone() {
    return new DetectedLanguage(langCode, confidence);
  }
  
  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public boolean equals(Object other) {
    if (other != null && other instanceof DetectedLanguage) {
      DetectedLanguage d = (DetectedLanguage) other;
      return Float.compare(this.confidence, d.confidence) == 0 &&
             equals(langCode, d.langCode);
    }
    return false;
  }

  private static boolean equals(String a, String b) {
    return a == b || (a != null && a.equals(b));
  }

  @Override
  public String toString() {
    return "[" + langCode + ", conf.: " + confidence + "]"; 
  }
}
