/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A utility which auto-translates English strings in Android string resources using
 * Google Translate.</p>
 *
 * <p>Pass the Android client res/ directory as first argument, and optionally message keys
 * who should be forced to retranslate.
 * Usage: {@code StringsResourceTranslator android/res/ [key_1 ...]}</p>
 *
 * @author Sean Owen
 */
public final class StringsResourceTranslator {

  private static final String API_KEY = "INSERT-YOUR-KEY";
  
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final Pattern ENTRY_PATTERN = Pattern.compile("<string name=\"([^\"]+)\".*>([^<]+)</string>");
  private static final Pattern STRINGS_FILE_NAME_PATTERN = Pattern.compile("values-(.+)");
  private static final Pattern TRANSLATE_RESPONSE_PATTERN = Pattern.compile("translatedText\":\\s*\"([^\"]+)\"");
  private static final Pattern VALUES_DIR_PATTERN = Pattern.compile("values-[a-z]{2}(-[a-zA-Z]{2,3})?");

  private static final String APACHE_2_LICENSE =
      "<!--\n" +
      " Copyright (C) 2011 ZXing authors\n" +
      '\n' +
      " Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
      " you may not use this file except in compliance with the License.\n" +
      " You may obtain a copy of the License at\n" +
      '\n' +
      "      http://www.apache.org/licenses/LICENSE-2.0\n" +
      '\n' +
      " Unless required by applicable law or agreed to in writing, software\n" +
      " distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
      " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
      " See the License for the specific language governing permissions and\n" +
      " limitations under the License.\n" +
      " -->\n";

  private static final Map<String,String> LANGUAGE_CODE_MASSAGINGS = new HashMap<String,String>(3);
  static {
    LANGUAGE_CODE_MASSAGINGS.put("zh-rCN", "zh-cn");
    LANGUAGE_CODE_MASSAGINGS.put("zh-rTW", "zh-tw");
  }

  private StringsResourceTranslator() {}

  public static void main(String[] args) throws IOException {

    File resDir = new File(args[0]);
    File valueDir = new File(resDir, "values");
    File stringsFile = new File(valueDir, "strings.xml");
    Collection<String> forceRetranslation = Arrays.asList(args).subList(1, args.length);

    File[] translatedValuesDirs = resDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.isDirectory() && VALUES_DIR_PATTERN.matcher(file.getName()).matches();
      }
    });
    for (File translatedValuesDir : translatedValuesDirs) {
      File translatedStringsFile = new File(translatedValuesDir, "strings.xml");
      translate(stringsFile, translatedStringsFile, forceRetranslation);
    }

  }

  private static void translate(File englishFile,
                                File translatedFile,
                                Collection<String> forceRetranslation) throws IOException {

    SortedMap<String,String> english = readLines(englishFile);
    SortedMap<String,String> translated = readLines(translatedFile);
    String parentName = translatedFile.getParentFile().getName();

    Matcher stringsFileNameMatcher = STRINGS_FILE_NAME_PATTERN.matcher(parentName);
    stringsFileNameMatcher.find();
    String language = stringsFileNameMatcher.group(1);
    String massagedLanguage = LANGUAGE_CODE_MASSAGINGS.get(language);
    if (massagedLanguage != null) {
      language = massagedLanguage;
    }

    System.out.println("Translating " + language);

    File resultTempFile = File.createTempFile(parentName, ".xml");

    boolean anyChange = false;
    Writer out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(resultTempFile), UTF8);
      out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      out.write(APACHE_2_LICENSE);
      out.write("<resources>\n");

      for (Map.Entry<String,String> englishEntry : english.entrySet()) {
        String key = englishEntry.getKey();
        String value = englishEntry.getValue();
        out.write("  <string name=\"");
        out.write(key);
        out.write('"');
        if (value.contains("%s") || value.contains("%f")) {
          // Need to specify that there's a value placeholder
          out.write(" formatted=\"false\"");
        }
        out.write('>');

        String translatedString = translated.get(key);
        if (translatedString == null || forceRetranslation.contains(key)) {
          anyChange = true;
          translatedString = translateString(value, language);
        }
        out.write(translatedString);

        out.write("</string>\n");
      }

      out.write("</resources>\n");
      out.flush();

    } finally {
      quietClose(out);
    }

    if (anyChange) {
      System.out.println("  Writing translations");
      translatedFile.delete();
      resultTempFile.renameTo(translatedFile);
    }
  }

  static String translateString(String english, String language) throws IOException {
    if ("en".equals(language)) {
      return english;
    }
    String massagedLanguage = LANGUAGE_CODE_MASSAGINGS.get(language);
    if (massagedLanguage != null) {
      language = massagedLanguage;
    }
    System.out.println("  Need translation for " + english);

    URL translateURL = new URL(
        "https://www.googleapis.com/language/translate/v2?key=" + API_KEY + "&q=" +
        URLEncoder.encode(english, "UTF-8") +
        "&source=en&target=" + language);
    CharSequence translateResult = fetch(translateURL);
    Matcher m = TRANSLATE_RESPONSE_PATTERN.matcher(translateResult);
    if (!m.find()) {
      System.err.println("No translate result");
      System.err.println(translateResult);
      return english;
    }
    String translation = m.group(1);
    System.out.println("  Got translation " + translation);

    // This is a little crude; unescape some common escapes in the raw response
    translation = translation.replaceAll("\\\\u0026quot;", "\"");
    translation = translation.replaceAll("\\\\u0026#39;", "'");
    translation = translation.replaceAll("\\\\u200b", "");
    translation = translation.replaceAll("&amp;quot;", "\"");
    translation = translation.replaceAll("&amp;#39;", "'");
    return translation;
  }

  private static CharSequence fetch(URL translateURL) throws IOException {
    URLConnection connection = translateURL.openConnection();
    connection.connect();
    StringBuilder translateResult = new StringBuilder(200);
    Reader in = null;
    try {
      in = new InputStreamReader(connection.getInputStream(), UTF8);
      char[] buffer = new char[1024];
      int charsRead;
      while ((charsRead = in.read(buffer)) > 0) {
        translateResult.append(buffer, 0, charsRead);
      }
    } finally {
      quietClose(in);
    }
    return translateResult;
  }

  private static SortedMap<String,String> readLines(File file) throws IOException {
    SortedMap<String,String> entries = new TreeMap<String,String>();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF8));
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher m = ENTRY_PATTERN.matcher(line);
        if (m.find()) {
          String key = m.group(1);
          String value = m.group(2);
          entries.put(key, value);
        }
      }
      return entries;
    } finally {
      quietClose(reader);
    }
  }

  private static void quietClose(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ioe) {
        // continue
      }
    }
  }

}
