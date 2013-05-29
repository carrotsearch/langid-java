package com.carrotsearch.labs.langid;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Language detection (classifier) public interface.
 * 
 * <p>
 * Use case scenarios are: streaming ({@link #reset()},
 * {@link #append(CharSequence)}, {@link #classify(boolean)}) and single-call
 * classification ({@link #classify(CharSequence, boolean)}).
 * 
 * <p>
 * Pay attention to contracts on individual methods (the objects they return are
 * mutable and their contents will change on subsequent calls).
 * 
 * <p>
 * <strong>Thread safety:</strong> in general instances of this interface will
 * <b>not</b> be thread safe unless marked otherwise. See particular
 * implementation for details.
 * 
 * @see LangIdV3
 */
public interface ILangIdClassifier {
  
  /**
   * Classify the language of an input character sequence. Whether all of the
   * sequence or just subsambles will be used for classification is up to the
   * implementation (at the moment the whole input is consumed so you may want
   * to trim it if it exceeds 2k characters which is typically enough to extract
   * a high-quality language profile).
   * 
   * <p>
   * This method is an all-in-one call to {@link #reset()},
   * {@link #append(CharSequence)} and {@link #classify(boolean)}.
   * 
   * @param str
   *          The input character sequence to identify.
   * @param normalizeConfidence
   *          Normalize prediction confidence to 0-1 range.
   * @return Returns the most likely language in which <code>str</code> is
   *         written. May return <code>null</code> if no data (or not enough
   *         data) is available.
   */
  public abstract DetectedLanguage classify(CharSequence str,
      boolean normalizeConfidence);
  
  /**
   * Reset internal buffers and state to start classifying a new example.
   */
  public abstract void reset();
  
  /**
   * Update internal buffers and feature vectors with more text.
   */
  public abstract void append(CharSequence str);
  
  /**
   * Update internal buffers and feature vectors with more UTF8-encoded text.
   * Care should be given to proper text segmentation into unicode points
   * (otherwise broken features may be identified).
   */
  public abstract void append(ByteBuffer buffer);
  
  /**
   * Update internal buffers and feature vectors with more UTF8-encoded text.
   * Care should be given to proper text segmentation into unicode points
   * (otherwise broken features may be identified).
   */
  public abstract void append(byte[] array, int start, int length);
  
  /**
   * Apply classification to the current buffer state. This may be called while
   * appending (to abort early if the desired confidence has been reached).
   */
  public abstract DetectedLanguage classify(boolean normalizeConfidence);
  
  /**
   * Return a list of ranked languages for the current buffer. The list is not
   * sorted, cannot be manipulated and will be reused on any subsequent calls to
   * this object, including {@link DetectedLanguage} objects inside. If the
   * result is to be stored somewhere, it needs to be deeply cloned.
   */
  public abstract List<DetectedLanguage> rank(boolean normalizeConfidence);
  
}