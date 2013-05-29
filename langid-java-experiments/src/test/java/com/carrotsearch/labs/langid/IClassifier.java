package com.carrotsearch.labs.langid;

public interface IClassifier<LABEL, DATA> {
  public LABEL classify(DATA data);
  public String getName();
}
