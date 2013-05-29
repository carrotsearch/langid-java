package com.carrotsearch.labs.langid;


/**
 * An adapter for {@link LangIdV2}.
 */
public final class LangIdV2ClassifierAdapter implements IClassifier<String,String> {
  private final LangIdV2 delegate;

  public LangIdV2ClassifierAdapter(LangIdV2 delegate) {
    this.delegate = delegate;
  }

  @Override
  public String classify(String data) {
    return delegate.classify(data, true).langCode;
  }
  
  @Override
  public String getName() {
    return "langid-v2";
  }
}
