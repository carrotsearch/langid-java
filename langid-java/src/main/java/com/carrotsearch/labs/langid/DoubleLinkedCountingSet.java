package com.carrotsearch.labs.langid;

/**
 * A double linked set with counting; trimmed and specific to this use case.
 */
final class DoubleLinkedCountingSet {
  public final int[] sparse;
  public final int[] dense;
  public final int[] counts;
  
  public int elementsCount;
  
  public DoubleLinkedCountingSet(int maxValue, int maxValues) {
    if (maxValues > maxValue) {
      throw new IllegalArgumentException("?" + maxValues + " > " + maxValue);
    }
    this.sparse = new int[maxValue + 1];
    this.dense = new int[maxValues];
    this.counts = new int[maxValues];
  }
  
  public void increment(int key) {
    int index = sparse[key];
    if (index < elementsCount && dense[index] == key) {
      counts[index]++;
    } else {
      index = elementsCount++;
      sparse[key] = index;
      dense[index] = key;
      counts[index] = 1;
    }
  }

  public void clear() {
    this.elementsCount = 0;
  }
}
