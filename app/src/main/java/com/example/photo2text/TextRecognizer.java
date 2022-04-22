package com.example.photo2text;

import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.nio.ByteBuffer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TextRecognizer {

    public GraphicOverlay mGraphicOverlay; // it is initialised in Camera2BasicFragment
    private static final String TAG = "txtProcessor";


    public int mWidth;
    public int mHeight;
    public String textOutput;

    private GraphicOverlay.Graphic textGraphic;

    // Whether we should ignore process(). This is usually caused by feeding input data faster than
    // the model can handle.
    private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

    public void setupResolution(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void imageFromByteBuffer(ByteBuffer buffer, int rotation) {

        if (shouldThrottle.get()) {  // This is to drop frames when one frame is processing.if its value is true it will exit the function but if false it will execute.
            return;
        }

        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(mWidth)
                .setHeight(mHeight)
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
                .setRotation(rotation)
                .build();
        mGraphicOverlay.setCameraInfo(mHeight, mWidth, CameraCharacteristics.LENS_FACING_BACK);

        FirebaseVisionImage image = FirebaseVisionImage.fromByteBuffer(buffer, metadata);

        runTextRecognition(image);
    }

    public void imageFromBitmap(Bitmap bitmap) {
        if (shouldThrottle.get()) {  // This is to drop frames when one frame is processing.if its value is true it will exit the function but if false it will execute.
            return;
        }

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        runTextRecognition(image);
    }

    private void runTextRecognition(FirebaseVisionImage image) {

        if (image == null) {
            Log.d(TAG, "runTextRecognition: Image is null");
        }

        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer(); //We are running the TextRecognizer on device

        Task<FirebaseVisionText> result = recognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText textRecognized) {
                        shouldThrottle.set(false);
                        processTextRecognised(textRecognized); // here we are passing the text for processing
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        shouldThrottle.set(false);
                        Log.d(TAG, "onFailure: ");
                    }
                });
        shouldThrottle.set(true);

    }

    private void processTextRecognised(FirebaseVisionText textRecognized) {
        mGraphicOverlay.clear();
        mGraphicOverlay.clearElements();
        List<FirebaseVisionText.TextBlock> blocks = textRecognized.getTextBlocks(); // get the text from the image think textblocks as a paragraph

        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
             for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
               /* for (int k = 0; k < elements.size(); k++) {
                    textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                    mGraphicOverlay.add(textGraphic);
                    textOutput = elements.get(k).getText();
                    Log.d(TAG, "Recognized Text: " + textOutput);
                }*/
                textGraphic = new TextGraphic(mGraphicOverlay, lines.get(j));
                mGraphicOverlay.addElement(textGraphic); /**Here we are using addElement instead of using add to add graphics so that TextBlock doesnot inteferes with lineText*/
            ///    textOutput = lines.get(j).getText();
           //     Log.d(TAG, "Recognized Text: " + textOutput);
            }
            textGraphic = new TextGraphic(mGraphicOverlay, blocks.get(i));
            mGraphicOverlay.add(textGraphic);
            textOutput = blocks.get(i).getText();
            Log.d(TAG, "Recognized Text: " + textOutput);

        }

    }
}