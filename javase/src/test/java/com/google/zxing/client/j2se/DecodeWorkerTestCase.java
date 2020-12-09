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

  static final String IMAGE_NOBARCODE_DATA_URI =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAMAAABEpIrGAAACE1BMVEUAAAAYsPIXsPL//+" +
      "2J6/CI6vB74/Alt/IXr/LL/++M7PD//+UZsPIXr/ISrfIRrPIXsPIYsPIYsPIKqfIQrPITrvIZsfImt/IouPImt/IZsf" +
      "IXr/IXsPIbsfIWr/IcsvInuPImuPIvvPJt3PB54vBt3PAasfIXsPIXr/IUrvIYsPIwvfJx3vB54vCE6PCS7/B64/Bt3P" +
      "AktvIXr/IYsPIYsPIasfIlt/IwvPKJ6/CM7PCW8fCQ7vCB5vAmt/IXr/IYsPIYsPIXsPKI6vCI6vCI6vAYsfMYsPIYsP" +
      "KI6vCI6vCI6vAYrO0YsPGI6vCI6vCI6vCJ6/CH6vBw3vB64/CE6PAwvPJr2/FLy/ESrfIYsPIjtvIasfIYsPIYsPIYsP" +
      "IYsPIYsPIYsPIYsPIYsPIYre4vvPIktvJ64/Bx3vCI6vAitfJj1/F74/CH6vAbsfI7wvF/5fCJ6vAXsPJw3fAWmNEYre" +
      "0mt/J85PCJ6/Bw3vAXqukYr/EYsPIZsPIwvPJ95PAjtvIVksgWmtQYre4VlMsXpuUVkccWl9AXquouu/Jy3/AWl88Wm9" +
      "QXpuQYrO0ZsfIVkcgVlMwWmNAXqekVisIVkMcVkscWm9UTXaQVgr0VisMVlcwTT5oTVZ4TXKMVhL4TTpoTTZoTW6MVg7" +
      "4Vj8YVicIVkMYVi8MUfbkTVZ8UfLkUY6gTVJ4Vg70TT5sTVp/hyCW0AAAAZnRSTlMAAAAAAAAAAAAAAAACGx8fHxsCAh" +
      "k2yeTi4uLJMgEZNsnm++fi5s80GTbJ5v3NNhzJ4uCvFwHJ5v3mNgIbHx8cBDHN/ckZOcz+5jYC5f3k4Bzk/f3NMv7NNh" +
      "0f/eTg4jYd/eQZ5v3GzkXBAAAByUlEQVQ4y73TvW4TQRSG4e/dtbHx2hv/FBEIEBWIEgmLhoaGittJRYeEfAUUFBS+Ai" +
      "qIoIBQgGQRFKWwoAxgKYog3vXf2uvx7lAkBDvyio7TnkffnDOaQf8o/h/4C+06gOQB1oHgnEASosZZ9VYFQvLqp837vO" +
      "NnsAqQdwXg3oeTI+DzOXDn8DLcZo8OdwF4vwqaZW4BXdiRrAPxuRkewE3gC7wxa+/hITfGlXkXdrQebJHkoLvICBBbJO" +
      "PG+BsvlQEeAbAfZydccCI//jrMnOExxMDrWtYWT4iI/LiTtQYtGIsybwd7GLMOKB84UqUDrxbzNe+hJeNNY+F2AbYJlp" +
      "r2BKQX6UvR1U/AbpP5aXebQLKipRIQxlJUknr8GYPdZu5FINGSpNSPRsVZcVhYyu+5H5vMxVMIJZHW+5VJ2UxiqTArep" +
      "NZcXaUGIlnYAaS0sR1lG5O+X4tjiuWUT2yhXBW5DlwWA3PktPNo8RN8sZN3MT1RwFtrMO0FNaCoXzVDyTper8WVF17IO" +
      "OWaUsLTDWsTfIyc+eXr6EuWS+oM/shX9CWhKmGeVPRtHxcMtqIbIqpOmNKAxnaS1/E+migDR3nJGnR0GDR+A23fMFDA8" +
      "6WRQAAAABJRU5ErkJggg==";

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
