package com.google.zxing;


import java.util.Arrays;

public class CoverageTool2000 {

  private static boolean[][] coverageMatrix = new boolean[10][];
  public static boolean setUpIsDone = false;
  public static double[] allCoverage = new double[10];

  /**
   * Initializes the coverageMatrix with false values for a specific m aethod.
   *
   * @param methodID
   * @param length
   */
  public static void initCoverageMatrix(int methodID, int length) {
    coverageMatrix[methodID] = new boolean[length];
    Arrays.fill(coverageMatrix[methodID], false);


  }

  /**
   * Checks the total coverage for a given method
   *
   * @param methodID
   * @return double - ratio between visited branches and total number of branches.
   */
  public static double checkCoverage(int methodID) {
    int length = coverageMatrix[methodID].length;
    int countTrue = 0;
    for (int i = 0; i < length; i++) {
      if (coverageMatrix[methodID][i]) {
        countTrue++;
      }
    }
    return Math.round(((double) countTrue / length) * 100d) / 100d;
  }

  /**
   * Sets the coverageMatrix to true for a specific method and branch.
   *
   * @param methodID
   * @param branchID
   */
  public static void setCoverageMatrix(int methodID, int branchID) {
    coverageMatrix[methodID][branchID] = true;
  }


  public static void checkAllCoverage(int nrTest) {
    for (int i = 0; i < nrTest;i++) {
      System.out.println("Method " + i + " - Coverage: " + checkCoverage(i));
    }

  }
}
