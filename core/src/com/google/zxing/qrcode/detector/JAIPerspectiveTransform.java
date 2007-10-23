package com.google.zxing.qrcode.detector;

/**
 * TODO need to reimplement this from scratch. This is derived from jai-core from Sun
 *  and it is not clear we can redistribute this modification.
 */
final class JAIPerspectiveTransform {

  private float m00, m01, m02, m10, m11, m12, m20, m21, m22;

  JAIPerspectiveTransform() {
    m00 = m11 = m22 = 1.0f;
    m01 = m02 = m10 = m12 = m20 = m21 = 0.0f;
  }

  private void makeAdjoint() {
    float m00p = m11 * m22 - m12 * m21;
    float m01p = m12 * m20 - m10 * m22; // flipped sign
    float m02p = m10 * m21 - m11 * m20;
    float m10p = m02 * m21 - m01 * m22; // flipped sign
    float m11p = m00 * m22 - m02 * m20;
    float m12p = m01 * m20 - m00 * m21; // flipped sign
    float m20p = m01 * m12 - m02 * m11;
    float m21p = m02 * m10 - m00 * m12; // flipped sign
    float m22p = m00 * m11 - m01 * m10;
    // Transpose and copy sub-determinants
    m00 = m00p;
    m01 = m10p;
    m02 = m20p;
    m10 = m01p;
    m11 = m11p;
    m12 = m21p;
    m20 = m02p;
    m21 = m12p;
    m22 = m22p;
  }

  private static void getSquareToQuad(float x0, float y0,
                                      float x1, float y1,
                                      float x2, float y2,
                                      float x3, float y3,
                                      JAIPerspectiveTransform tx) {
    float dx3 = x0 - x1 + x2 - x3;
    float dy3 = y0 - y1 + y2 - y3;
    tx.m22 = 1.0f;
    if ((dx3 == 0.0f) && (dy3 == 0.0f)) { // to do: use tolerance
      tx.m00 = x1 - x0;
      tx.m01 = x2 - x1;
      tx.m02 = x0;
      tx.m10 = y1 - y0;
      tx.m11 = y2 - y1;
      tx.m12 = y0;
      tx.m20 = 0.0f;
      tx.m21 = 0.0f;
    } else {
      float dx1 = x1 - x2;
      float dy1 = y1 - y2;
      float dx2 = x3 - x2;
      float dy2 = y3 - y2;
      float invdet = 1.0f / (dx1 * dy2 - dx2 * dy1);
      tx.m20 = (dx3 * dy2 - dx2 * dy3) * invdet;
      tx.m21 = (dx1 * dy3 - dx3 * dy1) * invdet;
      tx.m00 = x1 - x0 + tx.m20 * x1;
      tx.m01 = x3 - x0 + tx.m21 * x3;
      tx.m02 = x0;
      tx.m10 = y1 - y0 + tx.m20 * y1;
      tx.m11 = y3 - y0 + tx.m21 * y3;
      tx.m12 = y0;
    }
  }

  private static JAIPerspectiveTransform getSquareToQuad(float x0, float y0,
                                                         float x1, float y1,
                                                         float x2, float y2,
                                                         float x3, float y3) {
    JAIPerspectiveTransform tx = new JAIPerspectiveTransform();
    getSquareToQuad(x0, y0, x1, y1, x2, y2, x3, y3, tx);
    return tx;
  }

  private static JAIPerspectiveTransform getQuadToSquare(float x0, float y0,
                                                         float x1, float y1,
                                                         float x2, float y2,
                                                         float x3, float y3) {
    JAIPerspectiveTransform tx = new JAIPerspectiveTransform();
    getSquareToQuad(x0, y0, x1, y1, x2, y2, x3, y3, tx);
    tx.makeAdjoint();
    return tx;
  }

  static JAIPerspectiveTransform getQuadToQuad(float x0, float y0,
                                               float x1, float y1,
                                               float x2, float y2,
                                               float x3, float y3,
                                               float x0p, float y0p,
                                               float x1p, float y1p,
                                               float x2p, float y2p,
                                               float x3p, float y3p) {
    JAIPerspectiveTransform tx1 = getQuadToSquare(x0, y0, x1, y1, x2, y2, x3, y3);
    JAIPerspectiveTransform tx2 = getSquareToQuad(x0p, y0p, x1p, y1p, x2p, y2p, x3p, y3p);
    tx1.concatenate(tx2);
    return tx1;
  }

  private void concatenate(JAIPerspectiveTransform Tx) {
    float m00p = m00 * Tx.m00 + m10 * Tx.m01 + m20 * Tx.m02;
    float m10p = m00 * Tx.m10 + m10 * Tx.m11 + m20 * Tx.m12;
    float m20p = m00 * Tx.m20 + m10 * Tx.m21 + m20 * Tx.m22;
    float m01p = m01 * Tx.m00 + m11 * Tx.m01 + m21 * Tx.m02;
    float m11p = m01 * Tx.m10 + m11 * Tx.m11 + m21 * Tx.m12;
    float m21p = m01 * Tx.m20 + m11 * Tx.m21 + m21 * Tx.m22;
    float m02p = m02 * Tx.m00 + m12 * Tx.m01 + m22 * Tx.m02;
    float m12p = m02 * Tx.m10 + m12 * Tx.m11 + m22 * Tx.m12;
    float m22p = m02 * Tx.m20 + m12 * Tx.m21 + m22 * Tx.m22;
    m00 = m00p;
    m10 = m10p;
    m20 = m20p;
    m01 = m01p;
    m11 = m11p;
    m21 = m21p;
    m02 = m02p;
    m12 = m12p;
    m22 = m22p;
  }

  void transform(float[] points) {
    int max = points.length;
    for (int offset = 0; offset < max; offset += 2) {
      float x = points[offset];
      float y = points[offset + 1];
      float w = m20 * x + m21 * y + m22;
      if (w == 0.0f) {
        points[offset] = x;
        points[offset + 1] = y;
      } else {
        float oneOverW = 1.0f / w;
        points[offset] = (m00 * x + m01 * y + m02) * oneOverW;
        points[offset + 1] = (m10 * x + m11 * y + m12) * oneOverW;
      }
    }
  }
}