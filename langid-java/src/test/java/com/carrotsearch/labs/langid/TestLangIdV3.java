package com.carrotsearch.labs.langid;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.google.common.base.Charsets;

public class TestLangIdV3 extends RandomizedTest {
  @Test
  public void testSanity() {
    LangIdV3 langid = new LangIdV3();

    
    for (String [] langString : new String [][] {
        {"en", "Mike McCandless rocks the boat."},
        {"pl", "W Szczebrzeszynie chrząszcz brzmi w trzcinie"},
        {"it", "Piano italiano per la crescita: negoziato in Europa sugli investimenti «virtuosi»"}
    }) {
      DetectedLanguage result = langid.classify(langString[1], true);
      assertEquals(langString[0], result.langCode);
    }
  }

  /**
   * Make sure all ways of getting the prediction yield the same result.
   */
  @Test
  public void testAppendMethods() {
    LangIdV3 v1 = new LangIdV3();
    
    for (int i = 0; i < 1000; i++) {
      String in = randomRealisticUnicodeOfCodepointLengthBetween(1, 300);
      boolean normalizeConfidence = randomBoolean();
      
      v1.reset();
      v1.append(in);
      DetectedLanguage c1 = v1.classify(normalizeConfidence).clone();

      v1.reset();
      assertEquals(c1, v1.classify(in, normalizeConfidence));

      v1.reset();
      v1.append(ByteBuffer.wrap(in.getBytes(Charsets.UTF_8)));
      assertEquals(c1, v1.classify(normalizeConfidence));

      byte [] bytes = in.getBytes(Charsets.UTF_8);
      int pad = randomIntBetween(0, 100);
      int shift = randomIntBetween(0, pad);
      byte [] bytesShifted = new byte [bytes.length + pad];
      System.arraycopy(bytes, 0, bytesShifted, shift, bytes.length);

      v1.reset();
      v1.append(bytesShifted, shift, bytes.length);
      assertEquals(c1, v1.classify(normalizeConfidence));
    }
  }
}
