package com.carrotsearch.labs.langid;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;



/**
 * Data model for {@link LangIdV3}.
 * 
 * @see #defaultModel()
 */
public final class Model {
  /** The default model, initialized lazily (once). */
  private static Model defaultModel;

  /**
   * Language classes.
   */
  String[] langClasses;

  /**
   * Flattened matrix of per-language feature probabilities.
   * <pre>
   * [featureIndex][langIndex]
   * where 
   * index = {@link #numClasses} * langIndex + featureIndex
   * </pre>
   */
  float[] nb_ptc;

  /**
   * Conditional init per-language probabilities (?).
   */
  float[] nb_pc;

  /**
   * State machine for walking byte n-grams. 
   */
  short[] dsa;

  /**
   * An output (may be null) associated with each state.
   */
  int[][] dsaOutput;

  /** Number of classes (languages). */
  int numClasses;

  /** Number of features (total). */
  int numFeatures;

  /**
   * Create a new model.
   */
  Model(String [] langClasses, float [] ptc, float [] pc, short [] dsa, int[][] dsaOutput) {
    this.langClasses = langClasses;
    this.nb_ptc = ptc;
    this.nb_pc = pc;
    this.dsa = dsa;
    this.dsaOutput = dsaOutput;

    assert nb_pc.length == langClasses.length;
    this.numClasses  = langClasses.length;
    this.numFeatures = nb_ptc.length / numClasses; 
  }

  /**
   * Read a model from an external data stream.
   */
  public static Model readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    String [] langClasses = (String[]) in.readObject();
    float[] nb_ptc = (float[]) in.readObject();
    float[] nb_pc = (float[]) in.readObject();
    short[] dsa = (short[]) in.readObject();
    int[][] dsaOutput = (int[][]) in.readObject();
    return new Model(langClasses, nb_ptc, nb_pc, dsa, dsaOutput);
  }

  void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(langClasses);
    out.writeObject(nb_ptc);
    out.writeObject(nb_pc);
    out.writeObject(dsa);
    out.writeObject(dsaOutput);
  }
  
  /**
   * Return a copy of this model trimmed to detect only a subset of languages.  
   */
  public static Model detectOnly(Set<String> langCodes) {
    final Model source = defaultModel();

    Set<String> newClasses = new LinkedHashSet<String>(Arrays.asList(source.langClasses));
    newClasses.retainAll(langCodes);
    if (newClasses.size() < 2) { 
      throw new IllegalArgumentException("A model must contain at least two languages.");
    }

    // Limit the set of supported languages (fewer languages = tighter loops and faster execution).
    String [] trimmed_nb_classes = newClasses.toArray(new String[newClasses.size()]);
    float[] trimmed_nb_pc = new float [newClasses.size()];
    float[] trimmed_nb_ptc = new float [newClasses.size() * source.numFeatures];
    for (int i = 0, j = 0; i < source.numClasses; i++) {
      if (newClasses.contains(source.langClasses[i])) {
        trimmed_nb_pc[j] = source.nb_pc[i];
        for (int f = 0; f < source.numFeatures; f++) {
          int iFrom = source.numFeatures * i + f;
          int iTo   = source.numFeatures * j + f;
          trimmed_nb_ptc[iTo] = source.nb_ptc[iFrom];
        }
        j++;
      }
    }

    return new Model(
        trimmed_nb_classes,
        trimmed_nb_ptc,
        trimmed_nb_pc,
        source.dsa,
        source.dsaOutput);
  }
  
  /**
   * Return a set of detected languages.
   */
  public Set<String> getDetectedLanguages() {
    return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(langClasses)));  
  }  

  /**
   * Return the default model with a full set of detected languages.
   */
  public static synchronized Model defaultModel() {
    if (defaultModel != null) {
      return defaultModel;
    }

    DataInputStream is = null;
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      is = new DataInputStream(
              new BufferedInputStream(
                  Model.class.getResourceAsStream("langid.lzma")));

      byte[] streamProperties = new byte[5];
      is.readFully(streamProperties);

      LzmaDecoder decoder = new LzmaDecoder();
      if (!decoder.SetDecoderProperties(streamProperties))
        throw new IOException("Incorrect stream properties.");

      byte [] streamSize = new byte [8];
      is.readFully(streamSize);

      long streamSizeLong = 0;
      for (int i = 8; --i >= 0;) {
        streamSizeLong <<= 8;
        streamSizeLong |= streamSize[i] & 0xFF;
      }

      if (!decoder.Code(is, os, streamSizeLong)) {
        throw new IOException("Error in data stream");
      }

      os.flush();

      return Model.readExternal(
          new ObjectInputStream(
              new ByteArrayInputStream(
                  os.toByteArray())));
    } catch (Exception e) {
      throw new RuntimeException("Default model not available.", e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Ignore, nothing to do.
        }
      }
    }
  }
}