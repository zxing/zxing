/*
 * Copyright (C) 2014 ZXing authors
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

package com.google.zxing.client.android.encode;

import android.telephony.PhoneNumberUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sean Owen
 */
final class VCardTelDisplayFormatter implements Formatter {

  private final List<Map<String,Set<String>>> metadataForIndex;

  VCardTelDisplayFormatter() {
    this(null);
  }

  VCardTelDisplayFormatter(List<Map<String,Set<String>>> metadataForIndex) {
    this.metadataForIndex = metadataForIndex;
  }

  @Override
  public CharSequence format(CharSequence value, int index) {
    value = PhoneNumberUtils.formatNumber(value.toString());
    Map<String,Set<String>> metadata =
        metadataForIndex == null || metadataForIndex.size() <= index ? null : metadataForIndex.get(index);
    value = formatMetadata(value, metadata);
    return value;
  }

  private static CharSequence formatMetadata(CharSequence value, Map<String,Set<String>> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return value;
    }
    StringBuilder withMetadata = new StringBuilder();
    for (Map.Entry<String,Set<String>> metadatum : metadata.entrySet()) {
      Set<String> values = metadatum.getValue();
      if (values == null || values.isEmpty()) {
        continue;
      }
      Iterator<String> valuesIt = values.iterator();
      withMetadata.append(valuesIt.next());
      while (valuesIt.hasNext()) {
        withMetadata.append(',').append(valuesIt.next());
      }
    }
    if (withMetadata.length() > 0) {
      withMetadata.append(' ');
    }
    withMetadata.append(value);
    return withMetadata;
  }

}
