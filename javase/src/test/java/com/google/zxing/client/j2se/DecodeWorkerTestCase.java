/*
 * Copyright 2018 ZXing authors
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

package com.google.zxing.client.j2se;

import com.beust.jcommander.JCommander;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Tests {@link DecodeWorker}.
 */
public final class DecodeWorkerTestCase extends Assert {

  static final String IMAGE_DATA_URI =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACEAAAAhAQAAAAB/n//CAAAAkklEQVR42mP4DwQNDJjkB4" +
      "E77A0M369N/d7A8CV6rjiQjPMFkWG1QPL7RVGg%2BAfREKCa/5/vA9V/nFSQ3sDwb7/KdiDJqX4dSH4pXN/A8DfyDVD2" +
      "988HQPUfPVaqA0XKz%2BgD9bIk1AP1fgwvB7KlS9VBdqXbA82PT9AH2fiaH2SXGdDM71fDgeIfhIvKsbkTTAIAKYVr0N" +
      "z5IloAAAAASUVORK5CYII=";


  @Test
  public void testWorker() throws Exception {
    DecoderConfig config = new DecoderConfig();
    JCommander jCommander = new JCommander(config);
    jCommander.parse("--pure_barcode", IMAGE_DATA_URI);
    Queue<URI> inputs = new LinkedList<>(Collections.singletonList(new URI(IMAGE_DATA_URI)));
    DecodeWorker worker = new DecodeWorker(config, inputs);
    assertEquals(1, worker.call().intValue());
  }
  
}
