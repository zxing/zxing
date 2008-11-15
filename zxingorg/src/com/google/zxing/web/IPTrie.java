/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.web;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * A trie data structure for storing a set of IP addresses efficiently.
 * 
 * @author Sean Owen
 */
final class IPTrie {

  private final IPTrieNode root;

  IPTrie() {
    root = new IPTrieNode(false);
  }

  int incrementAndGet(InetAddress ipAddress) {
    byte[] octets = ipAddress.getAddress();
    synchronized (root) {
      IPTrieNode current = root;
      int max = octets.length - 1;
      for (int offset = 0; offset < max; offset++) {
        int index = 0xFF & octets[offset];
        IPTrieNode child = current.children[index];
        if (child == null) {
          child = new IPTrieNode(offset == max - 1);
          current.children[index] = child;
        }
        current = child;
      }
      int index = 0xFF & octets[max];
      current.values[index]++;
      return current.values[index];
    }
  }

  void clear() {
    synchronized (root) {
      Arrays.fill(root.children, null);
    }
  }

  private static final class IPTrieNode {
    final IPTrieNode[] children;
    final int[] values;

    private IPTrieNode(boolean terminal) {
      if (terminal) {
        children = null;
        values = new int[256];
      } else {
        children = new IPTrieNode[256];
        values = null;
      }
    }
  }

}