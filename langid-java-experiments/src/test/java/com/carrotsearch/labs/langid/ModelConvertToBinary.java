package com.carrotsearch.labs.langid;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.Files;

/**
 * Read a model from txt file and persist it to binary format.
 */
public final class ModelConvertToBinary {
  private static Model loadModel(InputStream modelData) throws IOException {
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(modelData, Charset.forName("UTF-8")));

    float [] nb_ptc = null;
    float [] nb_pc = null;
    String [] nb_classes = null;
    short [] tk_nextmove = null;
    int[][] tk_output = null;

    String line;
    while ((line = reader.readLine()) != null) {
      int eqIndex = line.indexOf("=");
      if (eqIndex < 0) continue;
      String key = line.substring(0, eqIndex);
      String val = line.substring(eqIndex + 1);
      if (key.equals("nb_ptc")) {
        nb_ptc = arrayOfFloats(val);
      }
      if (key.equals("nb_pc")) {
        nb_pc = arrayOfFloats(val);
      }
      if (key.equals("nb_classes")) {
        nb_classes = val.split("[\\,\\s]+");
      }
      if (key.equals("tk_nextmove")) {
        int[] tmp = arrayOfInts(val);
        short[] tmp2 = new short[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
          if (tmp[i] > Short.MAX_VALUE) throw new RuntimeException();
          tmp2[i] = (short) tmp[i];
        }
        tk_nextmove = tmp2;
      }
      if (key.equals("tk_output")) {
        String [] kvPairs = val.split("[\\;]");
        Map<Integer,int[]> tmp = new HashMap<Integer,int[]>();
        for (int i = 0; i < kvPairs.length; i++) {
          int colonIndex = kvPairs[i].indexOf(":");
          int index = Integer.parseInt(kvPairs[i].substring(0, colonIndex).trim());
          int [] vals = arrayOfInts(kvPairs[i].substring(colonIndex + 1).replaceAll("[\\(\\)]", ""));
          tmp.put(index, vals);
        }
        
        int maxKey = -1;
        for (int i : tmp.keySet()) {
          maxKey = Math.max(maxKey, i);
        }
        tk_output = new int[maxKey + 1][];
        for (Map.Entry<Integer,int[]> e : tmp.entrySet()) {
          int[] value = e.getValue();
          if (value.length != 0) {
            tk_output[e.getKey()] = value;
          }
        }
      }
    }

    // transpose model.nb_ptc so dot() has linear access pattern in memory.
    nb_ptc = MatrixOps.transpose(
        nb_ptc, 
        nb_pc.length,
        nb_ptc.length / nb_pc.length);

    return new Model(
        nb_classes,
        nb_ptc,
        nb_pc,
        tk_nextmove,
        tk_output);
  }

  private static float[] arrayOfFloats(String val) {
    String [] vals = val.split("[\\,\\s]+");
    float [] res = new float [vals.length];
    for (int i = 0; i < vals.length; i++) {
      res[i] = Float.parseFloat(vals[i]);
    }
    return res;
  }

  private static int[] arrayOfInts(String val) {
    String [] vals = val.trim().split("[\\,\\s]+");
    if (vals.length > 0 && vals[vals.length -1].equals("")) {
      vals = Arrays.copyOf(vals, vals.length - 1);
    }
    int [] res = new int [vals.length];
    for (int i = 0; i < vals.length; i++) {
      try {
        res[i] = Integer.parseInt(vals[i]);
      } catch (NumberFormatException e) {
        throw e;
      }
    }
    return res;
  }

  public static void main(String[] args) throws Exception {
    Model model = ModelConvertToBinary.loadModel(ModelConvertToBinary.class.getResourceAsStream("/langid.model.txt"));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    model.writeExternal(oos);

    Files.write(baos.toByteArray(), new File("langid.model"));
  }
}