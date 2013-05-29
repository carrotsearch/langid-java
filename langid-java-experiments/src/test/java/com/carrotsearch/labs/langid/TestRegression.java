package com.carrotsearch.labs.langid;

import org.junit.Ignore;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.Seeds;

public class TestRegression extends RandomizedTest {
  @Test
  @Seeds(value = {
      @Seed,
      @Seed("deadbeef")
  })
  @Ignore
  public void testV2AgainstV1() {
    LangIdV1 v1 = new LangIdV1();
    LangIdV2 v2 = new LangIdV2();
    for (int i = 0; i < 100; i++) {
      String in = randomRealisticUnicodeOfCodepointLengthBetween(1, 300);
      DetectedLanguage c1 = v1.classify(in);
      DetectedLanguage c2 = v2.classify(in, true);
      assertEquals(c1.langCode, c2.langCode);
      assertTrue(c1.confidence + " " + c2.confidence, c1.confidence == c2.confidence);
    }
  }

  @Test
  @Seeds(value = {
      @Seed,
      @Seed("deadbeef")
  })
  public void testV3AgainstV2() {
    LangIdV2 v2 = new LangIdV2();
    LangIdV3 v3 = new LangIdV3();
    for (int i = 0; i < 100; i++) {
      String in = randomRealisticUnicodeOfCodepointLengthBetween(1, 300);
      DetectedLanguage c1 = v2.classify(in, true);
      DetectedLanguage c2 = v3.classify(in, true);
      assertEquals(c1.langCode, c2.langCode);
      assertEquals(c1.confidence + " " + c2.confidence, c1.confidence, c2.confidence, 0.01d);

      c1 = v2.classify(in, false);
      c2 = v3.classify(in, false);
      assertEquals(c1.langCode, c2.langCode);
      assertEquals(c1.confidence + " " + c2.confidence, c1.confidence, c2.confidence, 0.01d);      
    }
  }  
}
