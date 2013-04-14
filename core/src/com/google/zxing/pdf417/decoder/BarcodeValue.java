package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.decoder.SimpleLog.LEVEL;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class BarcodeValue {
  Map<Integer,Integer> values = new HashMap<Integer,Integer>();

  public void setValue(int value) {
    Integer confidence = values.get(value);
    if (confidence == null) {
      confidence = 0;
    }
    confidence = confidence + 1;
    values.put(value, confidence);
    //    for (Entry<Integer,Integer> entry : values.entrySet()) {
    //      SimpleLog.log(LEVEL.ALL, "value: " + entry.getKey() + ", confidence: " + entry.getValue());
    //    }
  }

  public Integer getValue() {
    int maxConfidence = -1;
    Integer result = null;
    boolean ambigous = false;
    for (Entry<Integer,Integer> entry : values.entrySet()) {
      if (entry.getValue() > maxConfidence) {
        maxConfidence = entry.getValue();
        result = entry.getKey();
        ambigous = false;
      } else if (entry.getValue() > maxConfidence) {
        ambigous = true;
      }
    }
    if (ambigous) {
      SimpleLog.log(LEVEL.DEVEL, "Ambigous: " + ambigous + " or underspecified value: " + maxConfidence +
          ", returning null instead");
      return null;
    }
    return result;
  }

  public Integer getConfidence(int value) {
    return values.get(value);
  }
}
