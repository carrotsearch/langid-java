package com.carrotsearch.labs.langid;

import org.junit.Test;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;

public class TestDoubleLinkedCountingSet extends RandomizedTest {
  @Test
  public void testSimple() {
    DoubleLinkedCountingSet s = new DoubleLinkedCountingSet(10, 5);
    assertEquals(0, s.elementsCount);
    s.increment(3);
    assertEquals(1, s.elementsCount);
    assertEquals(1, s.counts[0]);
    assertEquals(3, s.dense[0]);
    s.increment(10);
    assertEquals(2, s.elementsCount);
    assertEquals(1, s.counts[1]);
    assertEquals(10, s.dense[1]);
    s.increment(3);
    assertEquals(2, s.elementsCount);
    assertEquals(2, s.counts[0]);
  }
  
  @Test
  @Repeat(iterations = 20)
  public void testRandomized() {
    int maxValue = randomIntBetween(0, 1000);
    int maxValues = randomIntBetween(0, maxValue);

    int [] values = new int [maxValues];
    for (int i = 0; i < values.length; i++) {
      values[i] = randomIntBetween(0, maxValue);
    }

    DoubleLinkedCountingSet s = new DoubleLinkedCountingSet(maxValue, maxValues);
    IntIntOpenHashMap ref = IntIntOpenHashMap.newInstance();
    for (int i = 0; i < maxValues * 10; i++) {
      int r = values[randomIntBetween(0, values.length - 1)];
      ref.addTo(r, 1);
      s.increment(r);
    }
    
    IntIntOpenHashMap result = IntIntOpenHashMap.newInstance();
    for (int i = 0; i < s.elementsCount; i++) {
      int k = s.dense[i];
      int v = s.counts[i];
      assertTrue(!result.containsKey(k));
      result.put(k, v);
    }
    
    assertEquals(ref, result);
  }
}
