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
      if (coverageMatrix[methodID][i] == true) {
        countTrue++;
      }
    }
    allCoverage[methodID] = Math.round(((double) countTrue / length)*100d)/100d;
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


  public static void checkAllCoverage() {
    System.out.println("1: " + allCoverage[0] + " 2: " + allCoverage[1] + " 3: " + allCoverage[2] + " 4: " + allCoverage[3] + " 5: " + allCoverage[4]);
    System.out.println("6: " + allCoverage[5] + " 7: " + allCoverage[6] + " 8: " + allCoverage[7] + " 9: " + allCoverage[8] + " 10: " + allCoverage[9]);
  }
}
