package com.google.zxing;


import java.util.Arrays;

public class CoverageTool2000 {

  private static boolean[][] coverageMatrix = new boolean[10][];

  /**
   * Initializes the coverageMatrix with false values for a specific method.
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
      if (coverageMatrix[methodID][i] == true) {
        countTrue++;
      }
    }
    return (double) countTrue / length;
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
  
}
