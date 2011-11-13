/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.web.generator.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public final class Generator implements EntryPoint {

  private final List<GeneratorSource> generators = new ArrayList<GeneratorSource>();
  private final ListBox genList = new ListBox();
  private final ListBox sizeList = new ListBox();
  private final ListBox ecLevelList = new ListBox();
  private final ListBox encodingList = new ListBox();
  private final Image result = new Image("");
  private final HTMLTable topPanel = new Grid(5, 1);
  private GeneratorSource selectedGenerator = null;
  private final VerticalPanel rightPanel = new VerticalPanel();
  private final TextBox urlResult = new TextBox();
  private final Widget downloadText =
      new HTML("<a href=\"\" id=\"downloadlink\" >Download</a> or embed with this URL:");
  private final TextArea rawTextResult = new TextArea();

  @Override
  public void onModuleLoad() {
    loadGenerators();

    HorizontalPanel mainPanel = new HorizontalPanel();
    setupLeftPanel();
    topPanel.getElement().setId("leftpanel");
    Widget leftPanel = topPanel;

    mainPanel.add(leftPanel);

    SimplePanel div = new SimplePanel();
    SimplePanel div2 = new SimplePanel();
    div2.add(result);
    div2.getElement().setId("innerresult");
    div.add(div2);
    div.getElement().setId("imageresult");

    urlResult.getElement().setId("urlresult");
    rawTextResult.getElement().setId("rawtextresult");
    rawTextResult.setCharacterWidth(50);
    rawTextResult.setVisibleLines(8);
    downloadText.getElement().setId("downloadText");
    rightPanel.add(div);
    rightPanel.add(downloadText);
    rightPanel.add(urlResult);
    rightPanel.add(rawTextResult);
    mainPanel.add(rightPanel);
    mainPanel.getElement().setId("mainpanel");
    RootPanel.get("ui").add(mainPanel);
    setWidget(1);
    invalidateBarcode();
  }

  private void setWidget(int id) {
    if (id >= 0 && id < generators.size()) {
      topPanel.setWidget(1, 0, generators.get(id).getWidget());
      genList.setSelectedIndex(id);
      selectedGenerator = generators.get(id);
      eraseErrorMessage();
      invalidateBarcode();
      genList.setFocus(false);
      selectedGenerator.setFocus();
    }
  }

  private void loadGenerators() {
    generators.add(new CalendarEventGenerator(changeHandler, keyPressHandler));
    generators.add(new ContactInfoGenerator(changeHandler, keyPressHandler));
    generators.add(new EmailGenerator(changeHandler, keyPressHandler));
    generators.add(new GeoLocationGenerator(changeHandler, keyPressHandler));
    generators.add(new PhoneNumberGenerator(changeHandler, keyPressHandler));
    generators.add(new SmsAddressGenerator(changeHandler, keyPressHandler));
    generators.add(new TextGenerator(changeHandler));
    generators.add(new UrlGenerator(changeHandler, keyPressHandler));
    generators.add(new WifiGenerator(changeHandler, keyPressHandler));
  }

  void setupLeftPanel() {
    topPanel.setHTML(2, 0,
        "<span id=\"errorMessageID\" class=\""+StylesDefs.ERROR_MESSAGE+"\"></span>");

    // fills up the list of generators
    for(GeneratorSource generator: generators) {
      genList.addItem(generator.getName());
      setGridStyle(generator.getWidget());
    }

    sizeList.addItem("Small", "120");
    sizeList.addItem("Medium", "230");
    sizeList.addItem("Large", "350");
    sizeList.setSelectedIndex(2);

    ecLevelList.addItem("L");
    ecLevelList.addItem("M");
    ecLevelList.addItem("Q");
    ecLevelList.addItem("H");
    ecLevelList.setSelectedIndex(0);
    
    encodingList.addItem("UTF-8");
    encodingList.addItem("ISO-8859-1");
    encodingList.addItem("Shift_JIS");
    encodingList.setSelectedIndex(0);

    // updates the second row of the table with the content of the selected generator
    genList.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent Event) {
        int i = genList.getSelectedIndex();
        setWidget(i);
      }
    });

    // grid for the generator picker
    HTMLTable selectionTable = new Grid(1, 2);
    selectionTable.setText(0, 0, "Contents");
    selectionTable.setWidget(0, 1, genList);
    setGridStyle(selectionTable);

    topPanel.setWidget(0, 0, selectionTable);

    // grid for the generate button
    HTMLTable generateGrid = new Grid(1, 2);
    setGridStyle(generateGrid);

    Button generateButton = new Button("Generate &rarr;");
    generateButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        generate();
      }
    });
    generateGrid.setWidget(0, 1, generateButton);
    topPanel.setWidget(4, 0, generateGrid);

    HTMLTable configTable = new Grid(3, 2);
    configTable.setText(0, 0, "Barcode size");
    configTable.setWidget(0, 1, sizeList);
    configTable.setText(1, 0, "Error correction");
    configTable.setWidget(1, 1, ecLevelList);
    configTable.setText(2, 0, "Character encoding");
    configTable.setWidget(2, 1, encodingList);
    setGridStyle(configTable);
    topPanel.setWidget(3, 0, configTable);
  }

  private static void setGridStyle(HTMLTable grid) {
    grid.getColumnFormatter().addStyleName(0, "firstColumn");
    grid.getColumnFormatter().addStyleName(1, "secondColumn");
    HTMLTable.CellFormatter cellFormatter = grid.getCellFormatter();
    for (int i = 0; i < grid.getRowCount(); ++i) {
      cellFormatter.addStyleName(i, 0, "firstColumn");
      cellFormatter.addStyleName(i, 1, "secondColumn");
    }
  }

  private static String getUrl(int sizeX, int sizeY, String ecLevel, String encoding, String content) {
    StringBuilder result = new StringBuilder(100);
    result.append("http://chart.apis.google.com/chart?cht=qr");
    result.append("&chs=").append(sizeX).append('x').append(sizeY);
    result.append("&chld=").append(ecLevel);
    result.append("&choe=").append(encoding);
    result.append("&chl=").append(URL.encodeQueryString(content));
    return result.toString();
  }

  private void generate() {
    try {
      String text = selectedGenerator.getText();
      eraseErrorMessage();
      int size = Integer.parseInt(sizeList.getValue(sizeList.getSelectedIndex()));
      String ecLevel = ecLevelList.getValue(ecLevelList.getSelectedIndex());
      String encoding = encodingList.getValue(encodingList.getSelectedIndex());
      String url = getUrl(size, size, ecLevel, encoding, text);
      result.setUrl(url);
      result.setVisible(true);
      urlResult.setText(url);
      urlResult.setVisible(true);
      rawTextResult.setText(text);
      rawTextResult.setVisible(true);
      Element linkElement = DOM.getElementById("downloadlink");
      linkElement.setAttribute("href", url);
      downloadText.setVisible(true);
    } catch (GeneratorException ex) {
      invalidateBarcode();
      String error = ex.getMessage();
      showErrorMessage(error);
    }
  }

  void invalidateBarcode() {
    result.setVisible(false);
    urlResult.setText("");
    urlResult.setVisible(false);
    rawTextResult.setText("");
    rawTextResult.setVisible(false);
    Element linkElement = DOM.getElementById("downloadlink");
    linkElement.setAttribute("href", "");
    downloadText.setVisible(false);
  }

  private static void showErrorMessage(String error) {
    Element errorElement = DOM.getElementById("errorMessageID");
    errorElement.setInnerHTML(error);
  }

  private static void eraseErrorMessage() {
    Element errorElement = DOM.getElementById("errorMessageID");
    errorElement.setInnerHTML("&nbsp;");
  }

  private final ChangeHandler changeHandler = new ChangeHandler() {
    @Override
    public void onChange(ChangeEvent event) {
      try {
        selectedGenerator.validate((Widget) event.getSource());
        eraseErrorMessage();
      } catch (GeneratorException ex) {
        String error = ex.getMessage();
        showErrorMessage(error);
        invalidateBarcode();
      }
    }
  };

  private final KeyPressHandler keyPressHandler = new KeyPressHandler() {
    @Override
    public void onKeyPress(KeyPressEvent event) {
      if (event.getCharCode() == '\n' || event.getCharCode() == '\r') {
        generate();
      }
    }
  };
}
