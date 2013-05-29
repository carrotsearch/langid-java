package com.carrotsearch.labs.langid;

/**
 * Some flattened matrix utilities.
 */
final class MatrixOps {
  private MatrixOps() {}
  
  /**
   * Transpose a matrix with the given number of cols and rows.
   */
  static float [] transpose(float [] m, int cols, int rows) {
    assert m.length == (cols * rows);

    // This could be made faster by going with linear strides and a modulo
    // but it's of zero impact here.
    float [] t = new float [m.length];
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        t[c * rows + r] = (float) m[r * cols + c];
      }
    }

    return t;
  }
}
