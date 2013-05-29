package com.carrotsearch.labs.langid;

import com.carrotsearch.sizeof.RamUsageEstimator;

public class BenchmarkV3Size {
  public static void main(String[] args) {
    long s = System.currentTimeMillis();
    LangIdV3 classifier = new LangIdV3();
    long e = System.currentTimeMillis();
    System.out.println("Load time: " + (e - s) / 1000.0d);
    System.out.println("Total: " + RamUsageEstimator.humanSizeOf(classifier));
    System.out.println("m: " + RamUsageEstimator.humanSizeOf(classifier.model));
    System.out.println("m.nb_pc: " + RamUsageEstimator.humanSizeOf(classifier.model.nb_pc));
    System.out.println("m.nb_ptc: " + RamUsageEstimator.humanSizeOf(classifier.model.nb_ptc));
    System.out.println("m.tk_nextmove: " + RamUsageEstimator.humanSizeOf(classifier.model.dsa));
    System.out.println("m.tk_output: " + RamUsageEstimator.humanSizeOf(classifier.model.dsaOutput));
  }
}
