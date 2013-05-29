package com.carrotsearch.labs.langid;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

/**
 * Run classification quality benchmark.
 */
public class BenchmarkFullLangSubset {
  public static void main(String[] args) throws IOException {
    Random rnd = new Random(0xdeadbeef);

    // Test data.
    List<ObjectObjectCursor<String,String>> euroParl21 = EuroParlData.europarl21();
    Collections.shuffle(euroParl21, rnd);

    // Classifiers to test.
    @SuppressWarnings("unchecked")
    IClassifier<String,String> [] classifiers = new IClassifier [] {
        new LangIdV3ClassifierAdapter(new LangIdV3()),
        new LangIdV2ClassifierAdapter(new LangIdV2()),
        // new LangIdV1ClassifierAdapter(new LangIdV1()),
        new LanguageDetectionAdapter(),
    };

    for (IClassifier<String,String> classifier : classifiers) {
      System.out.println("--> " + classifier.getName());
      EvaluateQuality.run(euroParl21, classifier);
    }
  }
}
