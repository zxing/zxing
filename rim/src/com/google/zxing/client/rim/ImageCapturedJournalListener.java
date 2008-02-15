/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing.client.rim;

import net.rim.device.api.io.file.FileSystemJournal;
import net.rim.device.api.io.file.FileSystemJournalEntry;
import net.rim.device.api.io.file.FileSystemJournalListener;

/**
 * @author Sean Owen (srowen@google.com)
 */
final class ImageCapturedJournalListener implements FileSystemJournalListener {

  private final ZXingMainScreen screen;
  private long lastUSN;

  ImageCapturedJournalListener(ZXingMainScreen screen) {
    this.screen = screen;
  }

  public void fileJournalChanged() {
    long nextUSN = FileSystemJournal.getNextUSN();
    for (long lookUSN = nextUSN - 1; lookUSN >= lastUSN; --lookUSN) {
      FileSystemJournalEntry entry = FileSystemJournal.getEntry(lookUSN);
      if (entry == null) {
        break;
      }
      if (entry.getEvent() == FileSystemJournalEntry.FILE_ADDED) {
        screen.handleFile(entry.getPath());
      }
    }
    lastUSN = nextUSN;
  }


}
