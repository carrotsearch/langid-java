package com.carrotsearch.labs.langid;


/**
 * An adapter for {@link LangIdV1}.
 */
public final class LangIdV1ClassifierAdapter implements IClassifier<String,String> {
  private final LangIdV1 delegate;

  public LangIdV1ClassifierAdapter(LangIdV1 delegate) {
    this.delegate = delegate;
  }

  @Override
  public String classify(String data) {
    return delegate.classify(data).langCode;
  }
  
  @Override
  public String getName() {
    return "langid-v1";
  }
}
