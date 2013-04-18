/*
 * Copyright 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.pdf417.detector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.pdf417.decoder.BitMatrixParser;

/**
 * <p>Encapsulates logic that detects valid codewords from a deskewed lines matrix.
 * To sample the grid several properties of PDF417 are used:</p>
 * 
 * <ul>
 *   <li>each codeword/symbol has 17 modules</li>
 *   <li>each codeword/symbol begins with a black bar and ends with a white bar</li>
 *   <li>each codeword consists of 4 black bars + 4 white bars</li>
 *   <li>all valid codewords are known (obviously)</li>
 * </ul>

 * @author creatale GmbH (christoph.schulz@creatale.de)
 */
public final class LinesSampler {

  private static final int MODULES_IN_SYMBOL = 17;
  private static final int BARS_IN_SYMBOL = 8;
  private static final int BARCODE_START_OFFSET = 2;
  private static final float[] RATIOS_TABLE;
  static {
    // Pre-computes and outputs the symbol ratio table.
    float[][] table = new float[BitMatrixParser.SYMBOL_TABLE.length][BARS_IN_SYMBOL];
    RATIOS_TABLE = new float[BitMatrixParser.SYMBOL_TABLE.length * BARS_IN_SYMBOL];
    int x = 0;
    for (int i = 0; i < BitMatrixParser.SYMBOL_TABLE.length; i++) {
      int currentSymbol = BitMatrixParser.SYMBOL_TABLE[i];
      int currentBit = currentSymbol & 0x1;
      for (int j = 0; j < BARS_IN_SYMBOL; j++) {
        float size = 0.0f;
        while ((currentSymbol & 0x1) == currentBit) {
          size += 1.0f;
          currentSymbol >>= 1;
        }
        currentBit = currentSymbol & 0x1;
        table[i][BARS_IN_SYMBOL - j - 1] = size / MODULES_IN_SYMBOL;
      }
      for (int j = 0; j < BARS_IN_SYMBOL; j++) {
        RATIOS_TABLE[x] = table[i][j];
        x++;
      }
    }
  }

  private final BitMatrix linesMatrix;
  private final int symbolsPerLine;
  private final int dimension;

  public LinesSampler(BitMatrix linesMatrix, int dimension) {
    this.linesMatrix = linesMatrix;
    this.symbolsPerLine = dimension / MODULES_IN_SYMBOL;
    this.dimension = dimension;
  }

  /**
   * Samples a grid from a lines matrix.
   *
   * @return the potentially decodable bit matrix.
   */
  public BitMatrix sample() throws NotFoundException {   
    List<Float> symbolWidths = findSymbolWidths();

    int[][] codewords = new int[linesMatrix.getHeight()][];
    int[][] clusterNumbers = new int[linesMatrix.getHeight()][];
    linesMatrixToCodewords(codewords, clusterNumbers, symbolWidths);

    List<List<Map<Integer, Integer>>> votes = distributeVotes(codewords, clusterNumbers);

    List<List<Integer>> detectedCodeWords = new ArrayList<List<Integer>>();
    resize3(detectedCodeWords, votes.size());
    for (int i = 0; i < votes.size(); i++) {
      resize4(detectedCodeWords.get(i), votes.get(i).size());
      for (int j = 0; j < votes.get(i).size(); j++) {
        if (!votes.get(i).get(j).isEmpty()) {
          detectedCodeWords.get(i).set(j, getValueWithMaxVotes(votes.get(i).get(j)).getVote());
        }
      }
    }

    List<Integer> insertLinesAt = findMissingLines(detectedCodeWords);

    int rowCount = decodeRowCount(detectedCodeWords, insertLinesAt);
    resize3(detectedCodeWords, rowCount);

    return codewordsToBitMatrix(detectedCodeWords, dimension, detectedCodeWords.size());
  }

  /**
   * Use the following property of PDF417 barcodes to detect symbols:
   * Every symbol starts with a black module and every symbol is 17 modules 
   * wide, therefore there have to be columns in the line matrix that are 
   * completely composed of black pixels.
   *
   * @return array containing with symbol widths.
   */
  private List<Float> findSymbolWidths() {
    float expectedSymbolWidth;
    if (symbolsPerLine > 0) {
      expectedSymbolWidth = linesMatrix.getWidth() / (float) symbolsPerLine;
    } else {
      expectedSymbolWidth = linesMatrix.getWidth();
    }

    List<Float> symbolWidths = new ArrayList<Float>();
    int symbolStart = 0;
    boolean lastWasSymbolStart = true;
    int[] blackCount = new int[linesMatrix.getWidth()];
    for (int x = BARCODE_START_OFFSET; x < linesMatrix.getWidth(); x++) {
      for (int y = 0; y < linesMatrix.getHeight(); y++) {
        if (linesMatrix.get(x, y)) {
          blackCount[x]++;
        }
      }
      if (blackCount[x] == linesMatrix.getHeight()) {
        if (!lastWasSymbolStart) {
          float currentWidth = x - symbolStart;
          // Make sure we really found a symbol by asserting a minimal size of
          // 75% of the expected symbol width. This might break highly distorted
          // barcodes, but fixes an issue with barcodes where there is a full black 
          // column from top to bottom within a symbol.
          if (currentWidth > 0.75 * expectedSymbolWidth) {
            // The actual symbol width might be slightly bigger than the expected
            // symbol width, but if we are more than half an expected symbol width 
            // bigger, we assume that  we missed one or more symbols and assume that 
            // they were the expected symbol width.
            while (currentWidth > 1.5 * expectedSymbolWidth) {
              symbolWidths.add(expectedSymbolWidth);
              currentWidth -= expectedSymbolWidth;
            }
            symbolWidths.add(currentWidth);
            lastWasSymbolStart = true;
            symbolStart = x;
          }
        }
      } else {
        if (lastWasSymbolStart) {
          lastWasSymbolStart = false;
        }
      }
    }

    // The last symbol ends at the right edge of the matrix, where there usually is no black bar.
    float currentWidth = linesMatrix.getWidth() - symbolStart;
    while (currentWidth > 1.5 * expectedSymbolWidth) {
      symbolWidths.add(expectedSymbolWidth);
      currentWidth -= expectedSymbolWidth;
    }
    symbolWidths.add(currentWidth);

    return symbolWidths;
  }

  private void linesMatrixToCodewords(int[][] codewords, int[][] clusterNumbers, List<Float> symbolWidths) 
      throws NotFoundException {

    // Not sure if this is the right way to handle this but avoids an error:
    if (symbolsPerLine > symbolWidths.size()) {
      throw NotFoundException.getNotFoundInstance();
    }

    for (int y = 0; y < linesMatrix.getHeight(); y++) {
      codewords[y] = new int[symbolsPerLine];
      clusterNumbers[y] = new int [symbolsPerLine];
      Arrays.fill(clusterNumbers[y], 0, clusterNumbers[y].length, -1);
      List<Integer> barWidths = new ArrayList<Integer>();
      // Run-length encode the bars in the scanned linesMatrix.
      // We assume that the first bar is black, as determined by the PDF417 standard.
      // Filter small white bars at the beginning of the barcode.
      // Small white bars may occur due to small deviations in scan line sampling.
      barWidths.add(BARCODE_START_OFFSET);
      boolean isSetBar = true;
      for (int x = BARCODE_START_OFFSET; x < linesMatrix.getWidth(); x++) {
        if (linesMatrix.get(x, y)) {
          if (!isSetBar) {
            isSetBar = true;
            barWidths.add(0);
          }
        } else {
          if (isSetBar) {
            isSetBar = false;
            barWidths.add(0);
          }

        }
        int lastIndex = barWidths.size() - 1;
        barWidths.set(lastIndex, barWidths.get(lastIndex) + 1);
      }

      // Find the symbols in the line by counting bar lengths until we reach symbolWidth.
      // We make sure, that the last bar of a symbol is always white, as determined by the PDF417 standard.
      // This helps to reduce the amount of errors done during the symbol recognition.
      // The symbolWidth usually is not constant over the width of the barcode.
      int[] cwStarts = new int[symbolsPerLine];
      cwStarts[0] = 0;
      int cwCount = 1;
      int cwWidth = 0;
      for (int i = 0; i < barWidths.size() && cwCount < symbolsPerLine; i++) {
        cwWidth += barWidths.get(i);
        if ((float)cwWidth > symbolWidths.get(cwCount - 1)) {
          if ((i % 2) == 1) { // check if bar is white
            i++;
          }
          if (i < barWidths.size()) {
          cwWidth = barWidths.get(i);
          }
          cwStarts[cwCount] = i;
          cwCount++;
        }
      }

      float[][] cwRatios = new float[symbolsPerLine][BARS_IN_SYMBOL];
      // Distribute bar widths to modules of a codeword.
      for (int i = 0; i < symbolsPerLine; i++) {
        int cwStart = cwStarts[i];
        int cwEnd = (i == symbolsPerLine - 1) ? barWidths.size() : cwStarts[i + 1];
        int cwLength = cwEnd - cwStart;

        if (cwLength < 7 || cwLength > 9) {
          // We try to recover symbols with 7 or 9 bars and spaces with heuristics, but everything else is beyond repair.
          continue;
        }

        // For symbols with 9 bar length simply ignore the last bar.
        float cwWidthF = 0.0f;
        for (int j = 0; j < Math.min(BARS_IN_SYMBOL, cwLength); ++j) {
          cwWidthF += (float)barWidths.get(cwStart + j);
        }

        // If there were only 7 bars and spaces detected use the following heuristic:
        // Assume the length of the symbol is symbolWidth and the last (unrecognized) bar uses all remaining space.
        if (cwLength == 7) {
          for (int j = 0; j < cwLength; ++j) {
            cwRatios[i][j] = (float)barWidths.get(cwStart + j) / symbolWidths.get(i);
          }
          cwRatios[i][7] = (symbolWidths.get(i) - cwWidthF) / symbolWidths.get(i);
        } else {
          for (int j = 0; j < cwRatios[i].length; ++j) {
            cwRatios[i][j] = barWidths.get(cwStart + j) / cwWidthF;
          }
        }

        float bestMatchError = Float.MAX_VALUE;
        int bestMatch = 0;

        // Search for the most possible codeword by comparing the ratios of bar size to symbol width.
        // The sum of the squared differences is used as similarity metric.
        // (Picture it as the square euclidian distance in the space of eight tuples where a tuple represents the bar ratios.)
        for (int j = 0; j < BitMatrixParser.SYMBOL_TABLE.length; j++) {
          float error = 0.0f;
          for (int k = 0; k < BARS_IN_SYMBOL; k++) {
            float diff = RATIOS_TABLE[j * BARS_IN_SYMBOL + k] - cwRatios[i][k];
            error += diff * diff;
          }
          if (error < bestMatchError) {
            bestMatchError = error;
            bestMatch = BitMatrixParser.SYMBOL_TABLE[j];
          }
        }
        codewords[y][i] = bestMatch;
        clusterNumbers[y][i] = calculateClusterNumber(bestMatch);
      }
    }
  }

  private List<List<Map<Integer, Integer>>> distributeVotes(int[][] codewords, int[][] clusterNumbers) {
    // Matrix of votes for codewords which are possible at this position.
    List<List<Map<Integer, Integer>>> votes = new ArrayList<List<Map<Integer, Integer>>>();
    votes.add(new ArrayList<Map<Integer,Integer>>());
    resize2(votes.get(0), symbolsPerLine);

    int currentRow = 0;
    Map<Integer, Integer> clusterNumberVotes = new HashMap<Integer, Integer>();
    int lastLineClusterNumber = -1;

    for (int y = 0; y < codewords.length; y++) {
      // Vote for the most probable cluster number for this row.
      clusterNumberVotes.clear();
      for (int i = 0; i < codewords[y].length; i++) {
        if (clusterNumbers[y][i] != -1) {
          clusterNumberVotes.put(clusterNumbers[y][i], defaultValue(clusterNumberVotes.get(clusterNumbers[y][i]), 0) + 1);
        }
      }

      // Ignore lines where no codeword could be read.
      if (!clusterNumberVotes.isEmpty()) {
        VoteResult voteResult = getValueWithMaxVotes(clusterNumberVotes);
        boolean lineClusterNumberIsIndecisive = voteResult.isIndecisive();
        int lineClusterNumber = voteResult.getVote();

        // If there are to few votes on the lines cluster number, we keep the old one.
        // This avoids switching lines because of damaged inter line readings, but
        // may cause problems for barcodes with four or less rows.
        if (lineClusterNumberIsIndecisive) {
          lineClusterNumber = lastLineClusterNumber;
        }

        if ((lineClusterNumber != ((lastLineClusterNumber + 3) % 9)) && (lastLineClusterNumber != -1)) {
          lineClusterNumber = lastLineClusterNumber;
        }

        // Ignore broken lines at the beginning of the barcode.
        if ((lineClusterNumber == 0 && lastLineClusterNumber == -1) || (lastLineClusterNumber != -1)) {
          if ((lineClusterNumber == ((lastLineClusterNumber + 3) % 9)) && (lastLineClusterNumber != -1)) {
            currentRow++;
            if (votes.size() < currentRow + 1) {
              resize1(votes, currentRow + 1);
              resize2(votes.get(currentRow), symbolsPerLine);
            }
          }

          if ((lineClusterNumber == ((lastLineClusterNumber + 6) % 9)) && (lastLineClusterNumber != -1)) {
            currentRow += 2;
            if (votes.size() < currentRow + 1) {
              resize1(votes, currentRow + 1);
              resize2(votes.get(currentRow), symbolsPerLine);
            }
          }

          for (int i = 0; i < codewords[y].length; i++) {
            if (clusterNumbers[y][i] != -1) {
              if (clusterNumbers[y][i] == lineClusterNumber) {
                Map<Integer, Integer> votesMap = votes.get(currentRow).get(i);
                votesMap.put(codewords[y][i], defaultValue(votesMap.get(codewords[y][i]), 0) + 1);
              } else if (clusterNumbers[y][i] == ((lineClusterNumber + 3) % 9)) {
                if (votes.size() < currentRow + 2) {
                  resize1(votes, currentRow + 2);
                  resize2(votes.get(currentRow + 1), symbolsPerLine);
                }
                Map<Integer, Integer> votesMap = votes.get(currentRow + 1).get(i);
                votesMap.put(codewords[y][i], defaultValue(votesMap.get(codewords[y][i]), 0) + 1);
              } else if ((clusterNumbers[y][i] == ((lineClusterNumber + 6) % 9)) && (currentRow > 0)) {
                Map<Integer, Integer> votesMap = votes.get(currentRow - 1).get(i);
                votesMap.put(codewords[y][i], defaultValue(votesMap.get(codewords[y][i]), 0) + 1);
              }
            }
          }
          lastLineClusterNumber = lineClusterNumber;
        }
      }
    }

    return votes;
  }

  private List<Integer> findMissingLines(List<List<Integer>> detectedCodeWords) {
    List<Integer> insertLinesAt = new ArrayList<Integer>();
    if (detectedCodeWords.size() > 1) {
      for (int i = 0; i < detectedCodeWords.size() - 1; i++) {
        int clusterNumberRow = -1;
        for (int j = 0; j < detectedCodeWords.get(i).size() && clusterNumberRow == -1; j++) {
          int clusterNumber = calculateClusterNumber(detectedCodeWords.get(i).get(j));
          if (clusterNumber != -1) {
            clusterNumberRow = clusterNumber;
          }
        }
        if (i == 0) {
          // The first line must have the cluster number 0. Insert empty lines to match this.
          if (clusterNumberRow > 0) {
            insertLinesAt.add(0);
            if (clusterNumberRow > 3) {
              insertLinesAt.add(0);
            }
          }
        }
        int clusterNumberNextRow = -1;
        for (int j = 0; j < detectedCodeWords.get(i + 1).size() && clusterNumberNextRow == -1; j++) {
          int clusterNumber = calculateClusterNumber(detectedCodeWords.get(i + 1).get(j));
          if (clusterNumber != -1) {
            clusterNumberNextRow = clusterNumber;
          }
        }
        if ((clusterNumberRow + 3) % 9 != clusterNumberNextRow
            && clusterNumberRow != -1
            && clusterNumberNextRow != -1) {
          // The cluster numbers are not consecutive. Insert an empty line between them.
          insertLinesAt.add(i + 1);
          if (clusterNumberRow == clusterNumberNextRow) {
            // There may be two lines missing. This is detected when two consecutive lines have the same cluster number.
            insertLinesAt.add(i + 1);
          }
        }
      }
    }

    for (int i = 0; i < insertLinesAt.size(); i++) {
      List<Integer> v = new ArrayList<Integer>();
      for (int j = 0; j < symbolsPerLine; ++j) {
        v.add(0);
      }
      detectedCodeWords.add(insertLinesAt.get(i) + i, v);
    }

    return insertLinesAt;
  }

  private int decodeRowCount(List<List<Integer>> detectedCodeWords, List<Integer> insertLinesAt) {
    // Use the information in the first and last column to determin the number of rows and find more missing rows.
    // For missing rows insert blank space, so the error correction can try to fill them in.

    insertLinesAt.clear();    
    Map<Integer,Integer> rowCountVotes = new HashMap<Integer, Integer>();
    Map<Integer,Integer> ecLevelVotes = new HashMap<Integer, Integer>();
    Map<Integer,Integer> rowNumberVotes = new HashMap<Integer, Integer>();
    int lastRowNumber = -1;

    for (int i = 0; i + 2 < detectedCodeWords.size(); i += 3) {
      rowNumberVotes.clear();
      int firstCodewordDecodedLeft = -1;
      if (detectedCodeWords.get(i).get(0) != 0) {
        firstCodewordDecodedLeft = BitMatrixParser.getCodeword(detectedCodeWords.get(i).get(0));
      }
      int secondCodewordDecodedLeft = -1;
      if (detectedCodeWords.get(i + 1).get(0) != 0) {
        secondCodewordDecodedLeft = BitMatrixParser.getCodeword(detectedCodeWords.get(i + 1).get(0));
      }
      int thirdCodewordDecodedLeft = -1;
      if (detectedCodeWords.get(i + 2).get(0) != 0) {
        thirdCodewordDecodedLeft = BitMatrixParser.getCodeword(detectedCodeWords.get(i + 2).get(0));
      }

      int firstCodewordDecodedRight = -1;
      if (detectedCodeWords.get(i).get(detectedCodeWords.get(i).size() - 1) != 0) {
        firstCodewordDecodedRight = BitMatrixParser.getCodeword(detectedCodeWords.get(i).get(detectedCodeWords.get(i).size() - 1));
      }
      int secondCodewordDecodedRight = -1;
      if (detectedCodeWords.get(i + 1).get(detectedCodeWords.get(i + 1).size() - 1) != 0) {
        secondCodewordDecodedRight = BitMatrixParser.getCodeword(detectedCodeWords.get(i + 1).get(detectedCodeWords.get(i + 1).size() - 1));
      }
      int thirdCodewordDecodedRight = -1;
      if (detectedCodeWords.get(i + 2).get(detectedCodeWords.get(i + 2).size() - 1) != 0) {
        thirdCodewordDecodedRight = BitMatrixParser.getCodeword(detectedCodeWords.get(i + 2).get(detectedCodeWords.get(i + 2).size() - 1));
      }

      if (firstCodewordDecodedLeft != -1 && secondCodewordDecodedLeft != -1) {
        int leftRowCount = ((firstCodewordDecodedLeft % 30) * 3) + ((secondCodewordDecodedLeft % 30) % 3);
        int leftECLevel = (secondCodewordDecodedLeft % 30) / 3;

        rowCountVotes.put(leftRowCount, defaultValue(rowCountVotes.get(leftRowCount), 0) + 1);
        ecLevelVotes.put(leftECLevel, defaultValue(ecLevelVotes.get(leftECLevel), 0) + 1);
      }

      if (secondCodewordDecodedRight != -1 && thirdCodewordDecodedRight != -1) {
        int rightRowCount = ((secondCodewordDecodedRight % 30) * 3) + ((thirdCodewordDecodedRight % 30) % 3);
        int rightECLevel = (thirdCodewordDecodedRight % 30) / 3;

        rowCountVotes.put(rightRowCount, defaultValue(rowCountVotes.get(rightRowCount), 0) + 1);
        ecLevelVotes.put(rightECLevel, defaultValue(ecLevelVotes.get(rightECLevel), 0) + 1);
      }

      if (firstCodewordDecodedLeft != -1) {
        int rowNumber = firstCodewordDecodedLeft / 30;
        rowNumberVotes.put(rowNumber, defaultValue(rowNumberVotes.get(rowNumber), 0) + 1);
      }
      if (secondCodewordDecodedLeft != -1) {
        int rowNumber = secondCodewordDecodedLeft / 30;
        rowNumberVotes.put(rowNumber, defaultValue(rowNumberVotes.get(rowNumber), 0) + 1);
      }
      if (thirdCodewordDecodedLeft != -1) {
        int rowNumber = thirdCodewordDecodedLeft / 30;
        rowNumberVotes.put(rowNumber, defaultValue(rowNumberVotes.get(rowNumber), 0) + 1);
      }
      if (firstCodewordDecodedRight != -1) {
        int rowNumber = firstCodewordDecodedRight / 30;
        rowNumberVotes.put(rowNumber, defaultValue(rowNumberVotes.get(rowNumber), 0) + 1);
      }
      if (secondCodewordDecodedRight != -1) {
        int rowNumber = secondCodewordDecodedRight / 30;
        rowNumberVotes.put(rowNumber, defaultValue(rowNumberVotes.get(rowNumber), 0) + 1);
      }
      if (thirdCodewordDecodedRight != -1) {
        int rowNumber = thirdCodewordDecodedRight / 30;
        rowNumberVotes.put(rowNumber, defaultValue(rowNumberVotes.get(rowNumber), 0) + 1);
      }
      int rowNumber = getValueWithMaxVotes(rowNumberVotes).getVote();
      if (lastRowNumber + 1 < rowNumber) {
        for (int j = lastRowNumber + 1; j < rowNumber; j++) {
          insertLinesAt.add(i);
          insertLinesAt.add(i);
          insertLinesAt.add(i);
        }
      }
      lastRowNumber = rowNumber;
    }

    for (int i = 0; i < insertLinesAt.size(); i++) {
      List<Integer> v = new ArrayList<Integer>();
      for (int j = 0; j < symbolsPerLine; ++j) {
        v.add(0);
      }
      detectedCodeWords.add(insertLinesAt.get(i) + i, v);      
    }

    int rowCount = getValueWithMaxVotes(rowCountVotes).getVote();
    //int ecLevel = getValueWithMaxVotes(ecLevelVotes).getVote();

    rowCount += 1;
    return rowCount;
  }

  private static final class VoteResult {
    private boolean indecisive;
    private int vote;
    boolean isIndecisive() {
      return indecisive;
    }
    void setIndecisive(boolean indecisive) {
      this.indecisive = indecisive;
    }
    int getVote() {
      return vote;
    }
    void setVote(int vote) {
      this.vote = vote;
    }
  }

  private static VoteResult getValueWithMaxVotes(Map<Integer, Integer> votes) {
    VoteResult result = new VoteResult();
    int maxVotes = 0;
    for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
      if (entry.getValue() > maxVotes) {
        maxVotes = entry.getValue();
        result.setVote(entry.getKey());
        result.setIndecisive(false);
      } else if (entry.getValue() == maxVotes) {
        result.setIndecisive(true);
      }
    }
    return result;
  }

  private static BitMatrix codewordsToBitMatrix(List<List<Integer>> codewords, int dimension, int yDimension) {
    BitMatrix result = new BitMatrix(dimension, yDimension);
    for (int i = 0; i < codewords.size(); i++) {
      for (int j = 0; j < codewords.get(i).size(); j++) {
        int moduleOffset = j * MODULES_IN_SYMBOL;
        for (int k = 0; k < MODULES_IN_SYMBOL; k++) {
          if ((codewords.get(i).get(j) & (1 << (MODULES_IN_SYMBOL - k - 1))) > 0) {
            result.set(moduleOffset + k, i);
          }
        }
      }
    }
    return result;
  }

  private static int calculateClusterNumber(int codeword) {
    if (codeword == 0) {
      return -1;
    }
    int barNumber = 0;
    boolean blackBar = true;
    int clusterNumber = 0;
    for (int i = 0; i < MODULES_IN_SYMBOL; i++) {
      if ((codeword & (1 << i)) > 0) {
        if (!blackBar) {
          blackBar = true;
          barNumber++;
        }
        if (barNumber % 2 == 0) {
          clusterNumber++;
        } else {
          clusterNumber--;
        }
      } else {
        if (blackBar) {
          blackBar = false;
        }
      }
    }
    return (clusterNumber + 9) % 9;
  }

  private static void resize1(List<List<Map<Integer,Integer>>> list, int size) {
    // Delete some
    for (int i = size; i < list.size(); i++) {
      list.remove(i);
    }
    // Append some.
    for (int i = list.size(); i < size; i++) {
      list.add(new ArrayList<Map<Integer,Integer>>());
    }
  }

  private static void resize2(List<Map<Integer,Integer>> list, int size) {
    // Delete some
    for (int i = size; i < list.size(); i++) {
      list.remove(i);
    }
    // Append some.
    for (int i = list.size(); i < size; i++) {
      list.add(new HashMap<Integer, Integer>());
    }
  }

  private static void resize3(List<List<Integer>> list, int size) {
    // Delete some
    for (int i = size; i < list.size(); i++) {
      list.remove(i);
    }
    // Append some.
    for (int i = list.size(); i < size; i++) {
      list.add(new ArrayList<Integer>());
    }
  }

  private static void resize4(List<Integer> list, int size) {
    // Delete some
    for (int i = size; i < list.size(); i++) {
      list.remove(i);
    }
    // Append some.
    for (int i = list.size(); i < size; i++) {
      list.add(0);
    }
  }
  
  private static <T> T defaultValue(T value, T d) {
    return value == null ? d : value;
  }

}
