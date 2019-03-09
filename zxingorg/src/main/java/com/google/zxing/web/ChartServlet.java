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

package com.google.zxing.web;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * A reimplementation of the
 * <a href="https://google-developers.appspot.com/chart/infographics/docs/qr_codes">
 * Google Chart Server's QR code encoder</a>, which is now deprecated. See
 * <a href="https://github.com/zxing/zxing/wiki/Chart-Server-Parameters">the chart server
 * parameters wiki page</a> for docs.
 *
 * @author Sean Owen
 */
@WebServlet({"/w/chart", "/w/chart.png", "/w/chart.gif", "/w/chart.jpg", "/w/chart.jpeg"})
public final class ChartServlet extends HttpServlet {

  private static final int MAX_DIMENSION = 4096;
  private static final Collection<Charset> SUPPORTED_OUTPUT_ENCODINGS = ImmutableSet.<Charset>builder()
      .add(StandardCharsets.UTF_8).add(StandardCharsets.ISO_8859_1).add(Charset.forName("Shift_JIS")).build();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doEncode(request, response, false);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doEncode(request, response, true);
  }

  private static void doEncode(HttpServletRequest request, HttpServletResponse response, boolean isPost)
      throws IOException {

    ChartServletRequestParameters parameters;
    try {
      parameters = doParseParameters(request, isPost);
    } catch (IllegalArgumentException | NullPointerException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
      return;
    }

    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, parameters.getMargin());
    if (!StandardCharsets.ISO_8859_1.equals(parameters.getOutputEncoding())) {
      // Only set if not QR code default
      hints.put(EncodeHintType.CHARACTER_SET, parameters.getOutputEncoding().name());
    }
    hints.put(EncodeHintType.ERROR_CORRECTION, parameters.getEcLevel());

    BitMatrix matrix;
    try {
      matrix = new QRCodeWriter().encode(parameters.getText(),
                                         BarcodeFormat.QR_CODE,
                                         parameters.getWidth(),
                                         parameters.getHeight(),
                                         hints);
    } catch (WriterException we) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, we.toString());
      return;
    }

    String requestURI = request.getRequestURI();
    if (requestURI == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    int lastDot = requestURI.lastIndexOf('.');
    String imageFormat;
    if (lastDot > 0) {
      imageFormat = requestURI.substring(lastDot + 1).toUpperCase(Locale.ROOT);
      // Special-case jpg -> JPEG
      if ("JPG".equals(imageFormat)) {
        imageFormat = "JPEG";
      }
    } else {
      imageFormat = "PNG";
    }
    
    String contentType;
    switch (imageFormat) {
      case "PNG":
        contentType = "image/png";
        break;
      case "JPEG":
        contentType = "image/jpeg";
        break;
      case "GIF":
        contentType = "image/gif";
        break;
      default:
        throw new IllegalArgumentException("Unknown format " + imageFormat);
    }
    
    ByteArrayOutputStream imageOut = new ByteArrayOutputStream(1024);
    MatrixToImageWriter.writeToStream(matrix, imageFormat, imageOut);
    byte[] imageData = imageOut.toByteArray();

    response.setContentType(contentType);
    response.setContentLength(imageData.length);
    response.setHeader("Cache-Control", "public");
    response.getOutputStream().write(imageData);
  }

  private static ChartServletRequestParameters doParseParameters(ServletRequest request, boolean readBody)
      throws IOException {

    Preconditions.checkArgument("qr".equals(request.getParameter("cht")), "Bad type");

    String widthXHeight = request.getParameter("chs");
    Preconditions.checkNotNull(widthXHeight, "No size");
    int xIndex = widthXHeight.indexOf('x');
    Preconditions.checkArgument(xIndex >= 0, "Bad size");

    int width = Integer.parseInt(widthXHeight.substring(0, xIndex));
    int height = Integer.parseInt(widthXHeight.substring(xIndex + 1));
    Preconditions.checkArgument(width > 0 && height > 0, "Bad size");
    Preconditions.checkArgument(width <= MAX_DIMENSION && height <= MAX_DIMENSION, "Bad size");

    String outputEncodingName = request.getParameter("choe");
    Charset outputEncoding = StandardCharsets.UTF_8;
    if (outputEncodingName != null) {
      outputEncoding = Charset.forName(outputEncodingName);
      Preconditions.checkArgument(SUPPORTED_OUTPUT_ENCODINGS.contains(outputEncoding), "Bad output encoding");
    }

    ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.L;
    int margin = 4;

    String ldString = request.getParameter("chld");
    if (ldString != null) {
      int pipeIndex = ldString.indexOf('|');
      if (pipeIndex < 0) {
        // Only an EC level
        ecLevel = ErrorCorrectionLevel.valueOf(ldString);
      } else {
        ecLevel = ErrorCorrectionLevel.valueOf(ldString.substring(0, pipeIndex));
        margin = Integer.parseInt(ldString.substring(pipeIndex + 1));
        Preconditions.checkArgument(margin > 0, "Bad margin");
      }
    }

    String text;
    if (readBody) {
      text = CharStreams.toString(request.getReader());
    } else {
      text = request.getParameter("chl");
    }
    Preconditions.checkArgument(text != null && !text.isEmpty(), "No input");

    return new ChartServletRequestParameters(width, height, outputEncoding, ecLevel, margin, text);
  }

}
