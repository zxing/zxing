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

package com.google.zxing.client.bug;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

/**
 * @author John Connolly
 */
public final class ImageCanvas extends Canvas {

	private Image image;

	public ImageCanvas(Image image) {
		this.image = image;
	}

	@Override
  public void paint(Graphics g) {
	  g.drawImage(image, 0, 0, this);
	}
	
	@Override
  public void update(Graphics g) {
		paint(g);
	}

 	@Override
  public Dimension getMinimumSize()	{
 		return getPreferredSize();
 	}
 	
 	@Override
  public Dimension getPreferredSize() {
    int width;
    do {
      width = image.getWidth(this);
    } while (width < 0);

    int height;
    do {
      height = image.getHeight(this);
    } while (height < 0);

 		return new Dimension(width, height);
 	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

}
