package com.example.photo2text;

// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashSet;

import java.util.Set;

/* <p>Associated {@link Graphic} items should use the following methods to convert to view
 * coordinates for the graphics that are drawn:
 *
 * <ol>
 *   <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 *       supplied value from the preview scale to the view scale.
 *   <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
 *       coordinate from the preview's coordinate system to the view coordinate system.
 * </ol>
 */
public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {
    private final Object lock = new Object();
    private int previewWidth;
    private float widthScaleFactor = 1.0f;
    private int previewHeight;
    private float heightScaleFactor = 1.0f;
    private int facing = CameraCharacteristics.LENS_FACING_BACK;
    private Set<T> graphics = new HashSet<>();

    /**In this app we are declaring a seprate graphics(elementGraphics) set so that it does not interferes with the main "graphics" set
     * as in OnTap method we want to make sure on tapping the screen, On can only select the textBlocks not the lineText, as we only need linesText for representation*/
    private Set<T> elementGraphics = new HashSet<>();

    public int totalWords = 0;
    public boolean isElementUsed = false;


    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay. Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the graphics element. Add
     * instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public abstract static class Graphic {
        private GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        /**
         * Draw the graphic on the supplied canvas. Drawing should use the following methods to convert
         * to view coordinates for the graphics that are drawn:
         *
         * <ol>
         * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
         * supplied value from the preview scale to the view scale.
         * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /**
         * Returns true if the supplied coordinates are within this graphic.
         */
        public abstract boolean contains(float x, float y);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * overlay.widthScaleFactor;
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        public float scaleY(float vertical) {
            return vertical * overlay.heightScaleFactor;
        }

        /**
         * Returns the application context of the app.
         */
        public Context getApplicationContext() {
            return overlay.getContext().getApplicationContext();
        }


        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate system.
         */
        public float translateX(float x) {
            if (overlay.facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return overlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        /**
         * Returns a RectF in which the left and right parameters of the provided Rect are adjusted
         * by translateX, and the top and bottom are adjusted by translateY.
         */
        public RectF translateRect(RectF inputRect) {
            RectF returnRect = new RectF();

            returnRect.left = translateX(inputRect.left);
            returnRect.top = translateY(inputRect.top);
            returnRect.right = translateX(inputRect.right);
            returnRect.bottom = translateY(inputRect.bottom);

            return returnRect;
        }


        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void clearElements() {
        synchronized (lock) {
            elementGraphics.clear();
        }
        postInvalidate();
    }

    /**
     * Adds a graphic to the overlay.
     */
    public void add(T graphic) {
        synchronized (lock) {
            graphics.add(graphic); // what if i can create a seperate method for adding elements where it will use a different set,That set will store graphics data of elements.
        }
        postInvalidate();
    }

    public void addElement(T graphicElement) {
        synchronized (lock) {
            elementGraphics.add(graphicElement);
        }
        postInvalidate();
    }


    /**
     * Removes a graphic from the overlay.
     */
    public void remove(T graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }
    public void removeElement(T graphicElement) {
        synchronized (lock) {
            elementGraphics.remove(graphicElement);
        }
        postInvalidate();
    }


    /**
     * Returns the first graphic, if any, that exists at the provided absolute screen coordinates.
     * These coordinates will be offset by the relative screen position of this view.
     *
     * @return First graphic containing the point, or null if no text is detected.
     */
    public T getGraphicAtLocation(float rawX, float rawY) {
        synchronized (lock) {
            int[] location = new int[2];
            this.getLocationOnScreen(location);
            for (T graphic : graphics) {
                if (graphic.contains(rawX, rawY)) {
                    totalWords = graphics.size(); // I Am defining the total words in this method because if i define it in the Add method it will automatically change as the loop continues which will result in a unstable loop.

                    Log.d("TTS", "Total Words:" + totalWords);
                    //    Log.d("PositionTag", "Rawx:" + rawX + " RawY:" + "locationX:" + location[0] + " DifferenceX:" + (rawX - location[0]));

                    return graphic;
                }
            }
            return null;
        }
    }


    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform image
     * coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (lock) {
            this.previewWidth = previewWidth;
            this.previewHeight = previewHeight;
            this.facing = facing;
        }
        postInvalidate();
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            if ((previewWidth != 0) && (previewHeight != 0)) {
                widthScaleFactor = (float) getWidth() / (float) previewWidth;
                heightScaleFactor = (float) getHeight() / (float) previewHeight;
            }

            for (T graphic : graphics) {
                graphic.draw(canvas);
            }

            for (T graphic : elementGraphics){
                graphic.draw(canvas);
            }

        }
    }
}
