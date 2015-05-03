/*
 * Copyright 2007 ZXing authors
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This simple command line utility decodes files, directories of files, or URIs which are passed
 * as arguments. By default it uses the normal decoding algorithms, but you can pass --try_harder
 * to request that hint. The raw text of each barcode is printed, and when running against
 * directories, summary statistics are also displayed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CommandLineRunner {

  private CommandLineRunner() {
  }

  public static void main(String[] args) throws Exception {
    DecoderConfig config = new DecoderConfig();
    JCommander jCommander = new JCommander(config, args);
    jCommander.setProgramName(CommandLineRunner.class.getSimpleName());
    if (config.help) {
      jCommander.usage();
      return;
    }

    List<URI> inputs = config.inputPaths;
    do {
      inputs = retainValid(expand(inputs), config.recursive);
    } while (config.recursive && isExpandable(inputs));

    int numInputs = inputs.size();
    if (numInputs == 0) {
      jCommander.usage();
      return;
    }

    Queue<URI> syncInputs = new ConcurrentLinkedQueue<>(inputs);
    int numThreads = Math.min(numInputs, Runtime.getRuntime().availableProcessors());
    int successful = 0;    
    if (numThreads > 1) {
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      Collection<Future<Integer>> futures = new ArrayList<>(numThreads);
      for (int x = 0; x < numThreads; x++) {
        futures.add(executor.submit(new DecodeWorker(config, syncInputs)));
      }
      executor.shutdown();
      for (Future<Integer> future : futures) {
        successful += future.get();
      }
    } else {
      successful += new DecodeWorker(config, syncInputs).call();
    }

    if (!config.brief && numInputs > 1) {
      System.out.println("\nDecoded " + successful + " files out of " + numInputs +
          " successfully (" + (successful * 100 / numInputs) + "%)\n");
    }
  }

  private static List<URI> expand(List<URI> inputs) throws IOException, URISyntaxException {
    List<URI> expanded = new ArrayList<>();
    for (URI input : inputs) {
      if (isFileOrDir(input)) {
        Path inputPath = Paths.get(input);
        if (Files.isDirectory(inputPath)) {
          try (DirectoryStream<Path> childPaths = Files.newDirectoryStream(inputPath)) {
            for (Path childPath : childPaths) {
              expanded.add(childPath.toUri());
            }
          }
        } else {
          expanded.add(input);
        }
      } else {
        expanded.add(input);
      }
    }
    for (int i = 0; i < expanded.size(); i++) {
      URI input = expanded.get(i);
      if (input.getScheme() == null) {
        expanded.set(i, new URI("file", input.getSchemeSpecificPart(), input.getFragment()));
      }
    }
    return expanded;
  }

  private static List<URI> retainValid(List<URI> inputs, boolean recursive) {
    List<URI> retained = new ArrayList<>();
    for (URI input : inputs) {
      boolean retain;
      if (isFileOrDir(input)) {
        Path inputPath = Paths.get(input);
        retain =
            !inputPath.getFileName().toString().startsWith(".") &&
            (recursive || !Files.isDirectory(inputPath));
      } else {
        retain = true;
      }
      if (retain) {
        retained.add(input);
      }
    }
    return retained;
  }

  private static boolean isExpandable(List<URI> inputs) {
    for (URI input : inputs) {
      if (isFileOrDir(input) && Files.isDirectory(Paths.get(input))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isFileOrDir(URI uri) {
    return "file".equals(uri.getScheme());
  }

}
