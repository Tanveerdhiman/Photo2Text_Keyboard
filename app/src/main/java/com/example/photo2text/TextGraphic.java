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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;


import com.google.firebase.ml.vision.text.FirebaseVisionText;




/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class TextGraphic extends GraphicOverlay.Graphic {

    private static final int TEXT_COLOR = Color.WHITE; //Todo:What if i can use a tts engine which will change the colour of the text the text
    private static final float TEXT_SIZE = 22.0f; // TODO i may have to make a method which will automatically adjust text size acoording to real size
    private static final float STROKE_WIDTH = 0.25f;

    private final Paint rectPaint;
    private final Paint textPaint;
    private FirebaseVisionText.Line lineText;
    private FirebaseVisionText.Element elementText;
    private FirebaseVisionText.TextBlock textBlock;
    private final boolean isElementUsed;

    /** A value of  '1' represents FirebaseVisionText.Line.
     * A value of  '2' represents FirebaseVisionText.Element.
     * A value of  '3' represents FirebaseVisionText.TextBlock.
     * This is implemented to decided which constructor to be used for each FirebaseVisionText type*/
    private int visionTextType = 0;

    TextGraphic(GraphicOverlay overlay, FirebaseVisionText.Line text) {
        super(overlay);
        Log.d("txtGraphic", "lineText called: ");

        isElementUsed = false;
        visionTextType = 1;
        this.lineText = text; //Todo: to make a map which will store each word and use it


        rectPaint = new Paint();
        rectPaint.setColor(Color.WHITE);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        postInvalidate();

    }

    TextGraphic(GraphicOverlay overlay, FirebaseVisionText.Element elementText) {
        super(overlay);
        Log.d("txtGraphic", "elementText called: ");
        isElementUsed = true;
        visionTextType = 2;
        overlay.isElementUsed = true;
        this.elementText = elementText;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        postInvalidate();
    }

    public TextGraphic(GraphicOverlay overlay, FirebaseVisionText.TextBlock textBlock) {
        super(overlay);
        Log.d("txtGraphic", "textBlock called: ");

        isElementUsed = false;
        visionTextType = 3;

        //overlay.isElementUsed = true;
        this.textBlock = textBlock;

        rectPaint = new Paint();
        rectPaint.setColor(Color.RED);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3.5f);

        textPaint = new Paint();
        textPaint.setColor(Color.TRANSPARENT);
        textPaint.setTextSize(TEXT_SIZE);
        postInvalidate();
    }


    public FirebaseVisionText.Line getLineText() {
        return lineText;
    }

    public FirebaseVisionText.Element getElementText() {
        return elementText;
    }
    public FirebaseVisionText.TextBlock getTextBlock() {
        return textBlock;
    }


    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (lineText == null  && visionTextType == 1 /*!isElementUsed*/) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }else if (elementText == null && visionTextType == 2 /*isElementUsed*/){
            throw new IllegalStateException("Attempting to draw a null text.");
        }
        else if (textBlock == null && visionTextType == 3){
            throw new IllegalStateException("Attempting to draw a null text.");
        }

        /**Why do we have to translate the coordinates of the bounding box?
         *   Because the bounding coordinates are relative to the frame that was detected on, not the one that we're viewing.
         * If you zoom in using pinch-to-zoom, for instance, they won't line up.
         */

        // Draws the bounding box around the TextBlock.


        /**Here we are using value of "visionTextType" to determine which constructor had been used*/
        //RectF rect;
      /*  switch (visionTextType){

            case 1:
                Log.d("bBox", "draw: Case 1");
                rect = new RectF(lineText.getBoundingBox());
                rect = translateRect(rect);
                canvas.drawRect(rect, rectPaint);
                canvas.drawText(lineText.getText(), rect.left, rect.bottom, textPaint); // TODO:: rect.left and rect.bottom are the coordinates of the text they can be used for mapping purposes
            case 2:
                Log.d("bBox", "draw: Case 2");
                rect = new RectF(elementText.getBoundingBox());
                rect = translateRect(rect);
                canvas.drawRect(rect, rectPaint);
                canvas.drawText(elementText.getText(), rect.left, rect.bottom, textPaint);
            case 3:
                Log.d("bBox", "draw: Case 3");
                rect = new RectF(textBlock.getBoundingBox());
                rect = translateRect(rect);
                canvas.drawRect(rect, rectPaint);
                canvas.drawText(textBlock.getText(), rect.left, rect.bottom, textPaint);

        }*/
        if (visionTextType ==1) {
            RectF rect = new RectF(lineText.getBoundingBox());
            rect = translateRect(rect);
            canvas.drawRect(rect, rectPaint);
            canvas.drawText(lineText.getText(), rect.left, rect.bottom, textPaint); // rect.left and rect.bottom are the coordinates of the text they can be used for mapping puposes

        } else if (visionTextType ==2) {
            RectF  rect = new RectF(elementText.getBoundingBox());
            rect = translateRect(rect);
            canvas.drawRect(rect, rectPaint);
            canvas.drawText(elementText.getText(), rect.left, rect.bottom, textPaint); // rect.left and rect.bottom are the coordinates of the text they can be used for mapping puposes
        }
        else if (visionTextType ==3) {
            RectF  rect = new RectF(textBlock.getBoundingBox());
            rect = translateRect(rect);
            canvas.drawRect(rect, rectPaint);
            canvas.drawText(textBlock.getText(), rect.left, rect.bottom, textPaint); // rect.left and rect.bottom are the coordinates of the text they can be used for mapping puposes
        }

    }


    @Override
    public boolean contains(float x, float y) {

        if (lineText == null  && visionTextType == 1 /*!isElementUsed*/) {
            return false;
        }else if (elementText == null && visionTextType == 2 /*isElementUsed*/){
            return false;
        }
        else if (textBlock == null && visionTextType == 3){
            return false;
        }

        /**Here we are again using the boolean to determine which constructor had been used*/

        RectF rect = null;

        if (visionTextType == 1){
            rect = new RectF(lineText.getBoundingBox());
        }else if (visionTextType == 2)
        {
            rect = new RectF(elementText.getBoundingBox());
        }
        else if (visionTextType == 3) {
            rect = new RectF(textBlock.getBoundingBox());
        } 

        rect = translateRect(rect);
        return rect.contains(x, y);

        //TODO: What i need to do is to create a array which will store text.As we can access get the text by touching at a specfic point on the screen.We will use its index as a base value
        //For a loop which will automatically increase its value and TTS engine will speak based on the index.
        //We can then create a seprate map which will store locations.
    }
}