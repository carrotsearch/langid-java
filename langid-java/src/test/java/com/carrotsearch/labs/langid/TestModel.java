package com.carrotsearch.labs.langid;

import java.util.Set;

import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.Seeds;
import com.google.common.collect.Sets;

public class TestModel extends RandomizedTest {
  @Test
  public void testModelCopy() {
    Model d = Model.defaultModel();
    Model n = Model.detectOnly(d.getDetectedLanguages());

    assertArrayEquals(n.langClasses, d.langClasses);
    assertArrayEquals(n.nb_pc, d.nb_pc, 0.0f);
    assertArrayEquals(n.nb_ptc, d.nb_ptc, 0.0f);
    assertEquals(n.numClasses, d.numClasses);
    assertEquals(n.numFeatures, d.numFeatures);
    assertArrayEquals(n.dsa, d.dsa);

    assertEquals(n.dsaOutput.length, d.dsaOutput.length);
    for (int i = 0; i < n.dsaOutput.length; i++) {
      assertArrayEquals(n.dsaOutput[i], d.dsaOutput[i]);
    }
  }
  
  @Test
  @Seeds(value = {
      @Seed,
      @Seed("deadbeef")
  })
  public void testSameResultWithTrimmedLanguages() {
    final Set<String> allowed = Sets.newHashSet("en", "de", "es", "fr", "it", "pl");
    LangIdV3 v1 = new LangIdV3();
    LangIdV3 v2 = new LangIdV3(Model.detectOnly(allowed));

    for (int i = 0; i < 10000; i++) {
      String in = randomRealisticUnicodeOfCodepointLengthBetween(1, 300);
      DetectedLanguage c1 = v1.classify(in, true);
      DetectedLanguage c2 = v2.classify(in, true);
      
      if (allowed.contains(c1.langCode)) {
        assertEquals(c1.langCode, c2.langCode);
      }
    }
  }  
}
