package com.carrotsearch.labs.langid;

import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class TestMatrixOps extends RandomizedTest {
  @Test
  public void testTransposeSimple() {
    float [] m = {
        0, 1, 2,
        3, 4, 5
    };

    float[] t = MatrixOps.transpose(m, 3, 2);

    assertArrayEquals(new float [] {
        0, 3,
        1, 4,
        2, 5
    }, t, 0f);
  }
  
  @Test
  public void testTransposeLarger() {
    float [] m = {
        0,  1,  2,  3,  4,  5,
        6,  7,  8,  9, 10, 11,
       12, 13, 14, 15, 16, 17
    };

    float[] t = MatrixOps.transpose(m, 6, 3);

    assertArrayEquals(new float [] {
        0,  6, 12,
        1,  7, 13,
        2,  8, 14,
        3,  9, 15,
        4, 10, 16,
        5, 11, 17
    }, t, 0f);
  }  
}
