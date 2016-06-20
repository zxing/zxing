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

package com.google.zxing.common;

public class BitRegion {
    private final int left;
    private final int top;
    private final int width;
    private final int height;

    /**
     * @param left The horizontal position to begin at (inclusive)
     * @param top The vertical position to begin at (inclusive)
     * @param width The width of the BitRegion
     * @param height The height of the BitRegion
     */
    public BitRegion(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public void checkValidityBitRegion() {
        if (this.getTop() < 0 || this.getLeft() < 0) {
            throw new IllegalArgumentException("Left and top must be nonnegative");
        }
        if (this.getHeight() < 1 || this.getWidth() < 1) {
            throw new IllegalArgumentException("Height and width must be at least 1");
        }
    }

    public int calculateRight(){
        return this.left + this.width;
    }

    public int calculateBottom(){
        return this.top + this.height;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
