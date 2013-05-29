package com.carrotsearch.labs.langid;

import java.util.Locale;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.google.common.base.Objects;

public class EvaluateQuality {
  public static <T extends CharSequence> void run(
      Iterable<ObjectObjectCursor<String,T>> testData,
      IClassifier<String,T> classifier) {
    
    long start = System.currentTimeMillis();
    
    int correct = 0;
    int total = 0;
    for (ObjectObjectCursor<String,T> c : testData) {
      total++;
      String expected = c.key;
      String actual = classifier.classify(c.value);
      if (Objects.equal(expected, actual)) {
        correct++;
      }
    }
    
    long end = System.currentTimeMillis();

    System.out.println(String.format(Locale.ENGLISH,
        "%10d/%10d (%3.4f%%) in %.2f sec. (%.0f docs/sec.)", correct, total,
        (correct * 100.0d / total), (end - start) / 1000.0d, total
            / ((end - start) / 1000.0d)));
  }  
}
