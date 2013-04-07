package com.google.zxing.pdf417.decoder;

import java.util.ArrayList;
import java.util.List;

// not sure if I really need a separate class for this or if I can use a plain collection
public class AdjustmentResults {
  private final List<AdjustmentResult> results = new ArrayList<AdjustmentResult>();

  public void add(AdjustmentResult result) {
    results.add(result);
  }

  public AdjustmentResult get(int index) {
    return results.get(index);
  }

  public int size() {
    return results.size();
  }

  public boolean isEmpty() {
    return results.isEmpty();
  }
}