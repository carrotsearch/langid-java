package com.carrotsearch.labs.langid;


/**
 * An adapter for {@link LangIdV3}.
 */
public final class LangIdV3ClassifierAdapter implements IClassifier<String,String> {
  private final LangIdV3 delegate;

  public LangIdV3ClassifierAdapter(LangIdV3 delegate) {
    this.delegate = delegate;
  }

  @Override
  public String classify(String data) {
    return delegate.classify(data, true).langCode;
  }
  
  @Override
  public String getName() {
    return "langid-v3";
  }
}
