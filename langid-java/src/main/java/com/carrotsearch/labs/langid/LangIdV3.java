package com.carrotsearch.labs.langid;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: sub-sampling for stable detection and quicker termination?
// TODO: add a classify method operating directly on a byte[] or a byte buffer.
// TODO: add classify returning all predictions.

/**
 * Performs text language identification.
 * 
 * <p>
 * An adaptation of the algorithm (including vast chunks of the implementation)
 * described in <a
 * href="http://www.aclweb.org/anthology-new/P/P12/P12-3005.pdf">
 * http://www.aclweb.org/anthology-new/P/P12/P12-3005.pdf</a>.
 * 
 * <p>
 * Data structures and most of the code has been changed to reflect Java's specific 
 * performance characteristics.
 * 
 * <p>
 * See performance notes in {@link #classify(CharSequence, boolean)}.
 * 
 * <p><strong>Thread safety:</strong> an instance of this class is <b>not</b> safe
 * for use by multiple threads at the same time. There are data buffers that are reused
 * internally (allocated statically for performance reasons). Model data can be safely 
 * shared though so it's trivial to create a thread-local factory of language identifiers.
 * 
 * @see "https://github.com/saffsd/langid.py"
 */
public final class LangIdV3 implements ILangIdClassifier {
  /** Data model for the classifier. */
  final Model model;

  // Reusable feature vector.
  final DoubleLinkedCountingSet fv;
  
  // Scratch data.
  private final float[] scratchPdc;

  // UTF16 to UTF8 encoder.
  private final CharsetEncoder encoder;

  // Scratch data.
  private final ByteBuffer scratchUtf8 = ByteBuffer.allocate(1024 * 4 /* 4 kB */);

  // Reusable rank list.
  private final ArrayList<DetectedLanguage> rankList;
  private final List<DetectedLanguage> rankListView;

  /**
   * Create a language identifier with the default model (full set of languages).
   * @see Model#detectOnly(java.util.Set)
   * @see Model#defaultModel()
   */
  public LangIdV3() {
    this(Model.defaultModel());
  }

  /**
   * Create a language identifier with a restricted model (set of languages).
   */
  public LangIdV3(Model model) {
    this.model = model;

    this.fv = new DoubleLinkedCountingSet(model.numFeatures, model.numFeatures); 
    this.scratchPdc = new float [model.numClasses];

    this.rankList = new ArrayList<DetectedLanguage>();
    for (String langCode : model.langClasses) {
      rankList.add(new DetectedLanguage(langCode, 0));
    }
    this.rankListView = Collections.unmodifiableList(rankList);

    this.encoder = Charset.forName("UTF-8")
        .newEncoder()
        .onMalformedInput(CodingErrorAction.IGNORE)
        .onUnmappableCharacter(CodingErrorAction.IGNORE);
  }

  /* 
   *
   */
  @Override
  public DetectedLanguage classify(CharSequence str, boolean normalizeConfidence) {
    // Compute the features and apply NB
    reset();
    append(str);
    return classify(normalizeConfidence);
  }

  /* 
   *
   */
  @Override
  public void reset() {
    fv.clear();
  }

  /* 
   *
   */
  @Override
  public void append(CharSequence str) {
    encoder.reset();
    CharBuffer chbuf = CharBuffer.wrap(str);
    CoderResult result;
    do {
      scratchUtf8.clear();
      result = encoder.encode(chbuf, scratchUtf8, true);
      scratchUtf8.flip();

      append(scratchUtf8);
    } while (result.isOverflow());
  }

  /* 
   *
   */
  @Override
  public void append(ByteBuffer buffer) {
    // Update predictions (without an intermediate statecount as in the original)
    short state = 0;
    int[][] tk_output = model.dsaOutput;
    short[] tk_nextmove = model.dsa;

    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      state = tk_nextmove[(state << 8) + (b & 0xff)];

      int[] is = tk_output[state];
      if (is != null) {
        for (int feature : is) {
          fv.increment(feature);
        }
      }
    }
  }

  /* 
   *
   */
  @Override
  public void append(byte [] array, int start, int length) {
    // Update predictions (without an intermediate statecount as in the original)
    short state = 0;
    int[][] tk_output = model.dsaOutput;
    short[] tk_nextmove = model.dsa;

    for (int i = start, max = start + length; i < max; i++) {
      byte b = array[i];
      state = tk_nextmove[(state << 8) + (b & 0xff)];

      int[] is = tk_output[state];
      if (is != null) {
        for (int feature : is) {
          fv.increment(feature);
        }
      }
    }
  }

  /* 
   *
   */
  @Override
  public DetectedLanguage classify(boolean normalizeConfidence) {
    final float [] probs = naiveBayesClassConfidence(fv);

    // Search for argmax(language certainty)
    int c = 0;
    float max = probs[c];
    for (int i = 1; i < probs.length; i++) {
      if (probs[i] > max) {
        c = i;
        max = probs[i];
      }
    }
    
    if (normalizeConfidence) {
      max = normalizeConfidenceAsProbability(probs, c);
    }
    
    return new DetectedLanguage(model.langClasses[c], max);    
  }

  /* 
   *
   */
  @Override
  public List<DetectedLanguage> rank(boolean normalizeConfidence) {
    final float [] probs = naiveBayesClassConfidence(fv);

    for (int c = model.numClasses; --c >= 0;) {
      float confidence = 
              normalizeConfidence 
            ? normalizeConfidenceAsProbability(probs, c)
            : probs[c];

      rankList.get(c).confidence = confidence;
    }

    return rankListView;
  }

  /**
   * Normalize confidence to 0..1 interval.
   */
  private float normalizeConfidenceAsProbability(float [] probs, int clazzIndex) {
    // Renormalize log-probs into a proper distribution
    float s = 0;
    float v = probs[clazzIndex];
    for (int j = 0; j < probs.length; j++) {
      s += Math.exp(probs[j] - v);
    }
    return  1 / s;
  }

  /**
   * Compute naive bayes class confidence values.
   */
  private float[] naiveBayesClassConfidence(DoubleLinkedCountingSet fv) {
    // Reuse scratch and initialize with nb_pc
    final float [] pdc = this.scratchPdc;
    System.arraycopy(model.nb_pc, 0, pdc, 0, pdc.length);

    // Compute the partial log-probability of the document given each class.
    final int numClasses = model.numClasses;
    final int numFeatures = model.numFeatures;
    final int [] dense = this.fv.dense;
    final int [] counts = this.fv.counts;
    final int nz = this.fv.elementsCount;
    final float [] nb_ptc = model.nb_ptc;
    for (int i = 0, fi = 0; i < numClasses; i++, fi += numFeatures) {
      float v = 0;
      for (int j = 0; j < nz; j++) {
        int index = dense[j];
        v += counts[j] * nb_ptc[fi + index];
      }
      pdc[i] += v;
    }

    return pdc;
  }
}