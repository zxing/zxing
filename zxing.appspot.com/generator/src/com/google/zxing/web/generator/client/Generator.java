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
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>. Setup the page, and loads
 * all different generators.
 * 
 * @author Yohann Coppel
 */
public class Generator implements EntryPoint {
//  public static final StringConstants S = GWT.create(StringConstants.class);
  private final List<GeneratorSource> generators = new ArrayList<GeneratorSource>();
  ListBox genList = new ListBox();
  ListBox sizeList = new ListBox();
  Image result = new Image("");//http://chart.apis.google.com/chart?cht=qr&chs=300x300&chl=http://google.com");
  Grid topPanel = new Grid(5, 1);
  GeneratorSource selectedGenerator = null;
  private Button generateButton;
  VerticalPanel rightPanel = new VerticalPanel();
  TextBox urlResult = new TextBox();
  HTML downloadText = new HTML("<a href=\"\" id=\"downloadlink\" >Download</a> or embed with this URL:");
  //Element errorElement = null;
  
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
    downloadText.getElement().setId("downloadText");
    rightPanel.add(div);
    rightPanel.add(downloadText);
    rightPanel.add(urlResult);
    mainPanel.add(rightPanel);
    mainPanel.getElement().setId("mainpanel");
    RootPanel.get("ui").add(mainPanel);
    //RootPanel.get().add(output);
    //output.setHeight("200px");
    //output.setWidth("500px");
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
    generators.add(new CalendarEventGenerator(changeListener, keyPressHandler));
    generators.add(new ContactInfoGenerator(changeListener, keyPressHandler));
    generators.add(new EmailGenerator(changeListener, keyPressHandler));
    generators.add(new GeoLocationGenerator(changeListener, keyPressHandler));
    generators.add(new PhoneNumberGenerator(changeListener, keyPressHandler));
    generators.add(new SmsAddressGenerator(changeListener, keyPressHandler));
    generators.add(new TextGenerator(changeListener));
    generators.add(new UrlGenerator(changeListener, keyPressHandler));
    generators.add(new WifiGenerator(changeListener, keyPressHandler));
  }
  
  public void setupLeftPanel() {
    topPanel.setHTML(2, 0,
        "<span id=\"errorMessageID\" class=\""+StylesDefs.ERROR_MESSAGE+"\"></span>");
    
    // fills up the list of generators
    for(GeneratorSource generator: generators) {
      genList.addItem(generator.getName());
      setGridStyle(generator.getWidget());
    }

    sizeList.addItem("S", "120");
    sizeList.addItem("M", "230");
    sizeList.addItem("L", "350");
    sizeList.setSelectedIndex(2);
    
    // updates the second row of the table with the content of the selected
    // generator
    genList.addChangeListener(new ChangeListener() {
      public void onChange(Widget sender) {
        int i = genList.getSelectedIndex();
        setWidget(i);
      }
    });

    // grid for the generator picker
    Grid selectionTable = new Grid(1, 2);
    selectionTable.setText(0, 0, "Contents");
    selectionTable.setWidget(0, 1, genList);
    setGridStyle(selectionTable);
    
    topPanel.setWidget(0, 0, selectionTable);
    
    // grid for the generate button
    Grid generateGrid = new Grid(1, 2);
    setGridStyle(generateGrid);
    
    generateButton = new Button("Generate &rarr;");
    generateButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        generate();
      }
    });
    generateGrid.setWidget(0,1,generateButton);
    topPanel.setWidget(4, 0, generateGrid);
    
    Grid sizeTable = new Grid(1, 2);
    sizeTable.setText(0, 0, "Barcode size");
    sizeTable.setWidget(0, 1, sizeList);
    setGridStyle(sizeTable);
    topPanel.setWidget(3, 0, sizeTable);
  }

  protected void setGridStyle(Grid grid) {
    grid.getColumnFormatter().addStyleName(0, "firstColumn");
    grid.getColumnFormatter().addStyleName(1, "secondColumn");
    CellFormatter cellFormatter = grid.getCellFormatter();
    for(int i = 0; i < grid.getRowCount(); ++i) {
      cellFormatter.addStyleName(i, 0, "firstColumn");
      cellFormatter.addStyleName(i, 1, "secondColumn");
    }
  }

  protected String getUrl(int sizeX, int sizeY, String content) {
    StringBuilder result = new StringBuilder();
    result.append("http://chart.apis.google.com/chart?cht=qr&chs=");
    result.append(sizeX).append('x').append(sizeY);
    result.append("&chl=");
    result.append(URL.encodeComponent(content));
    return result.toString();
  }
  
  private void generate() {
    try {
      String text = selectedGenerator.getText();
      eraseErrorMessage();
      int size = Integer.parseInt(sizeList
          .getValue(sizeList.getSelectedIndex()));
      String url = getUrl(size, size, text);
      result.setUrl(url);
      result.setVisible(true);
      urlResult.setText(url);
      urlResult.setVisible(true);
      Element linkElement = DOM.getElementById("downloadlink");
      linkElement.setAttribute("href", url);
      downloadText.setVisible(true);
    } catch (GeneratorException ex) {
      invalidateBarcode();
      String error = ex.getMessage();
      showErrorMessage(error);
    }
  }
  
  public void invalidateBarcode() {
    result.setVisible(false);
    urlResult.setText("");
    urlResult.setVisible(false);
    Element linkElement = DOM.getElementById("downloadlink");
    linkElement.setAttribute("href", "");
    downloadText.setVisible(false);
  }
  
  public void showErrorMessage(String error) {
    Element errorElement = DOM.getElementById("errorMessageID");
    errorElement.setInnerHTML(error);
  }
  
  public void eraseErrorMessage() {
    Element errorElement = DOM.getElementById("errorMessageID");
    errorElement.setInnerHTML("&nbsp;");
  }
  
  public ChangeListener changeListener = new ChangeListener() {
    public void onChange(Widget sender) {
      try {
        selectedGenerator.validate(sender);
        eraseErrorMessage();
      } catch (GeneratorException ex) {
        String error = ex.getMessage();
        showErrorMessage(error);
        invalidateBarcode();
      }
    }
  };
  
  public KeyPressHandler keyPressHandler = new KeyPressHandler() {
    @Override
    public void onKeyPress(KeyPressEvent event) {
      if (event.getCharCode() == '\n' || event.getCharCode() == '\r') {
        generate();
      }
    }
  };
}
