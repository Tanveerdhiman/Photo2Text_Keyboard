package com.example.photo2text;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;


import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.GestureDetector;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.FrameLayout;

import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {


    MainActivity mainActivity = new MainActivity();
    private GraphicOverlay mGraphicOverlay;
    private TextRecognizer mTextRecognizer = new TextRecognizer();

    private float touchX;
    private float touchY;

    private TextureView mTextureView;

    /*Tested resolutions : 1280x960 , 2048 x 1536, 800 x 600*/
    private int mPreviewWidth = 1280;
    private int mPreviewHeight = 960;

    private Size mPreviewSize;
    private ImageReader mImageReader;
    private Button RecogniseBtn;

    private Button clearBtn;
    private Button backspaceBtn;
    private Button spaceBtn;
    private Button returnBtn;

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;


    private String mCameraId;
    private InputConnection inputConnection;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private int buttonCount = 0;


    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private int getRotationCompensation(String cameraId) throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        int sensorOrientation;
        sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e("cameraPreview", "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(), "SurfaceView is available", Toast.LENGTH_SHORT).show();
            Log.d("cameraPreview", "width: " + width + " Height:" + height);
            setupCamera(mPreviewWidth, mPreviewHeight);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
            Toast.makeText(getApplicationContext(), "Camera Connection Made", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };


    private int rotation;

    private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();

            ByteBuffer byteBuffer = imageToByteBuffer(image);
           // ByteBuffer buffer = image.getPlanes()[0].getBuffer();
           // byte[] bytes = new byte[buffer.capacity()];
            //buffer.get(bytes);
           // Bitmap picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

            try {
                rotation = getRotationCompensation(mCameraId);
            } catch (CameraAccessException e) {
                Log.d("cameraPreview", "onImageAvailable: CameraAccessException");
                e.printStackTrace();
            }
            mTextRecognizer.setupResolution(mPreviewWidth, mPreviewHeight);
            if (image != null) {
                //mTextRecognizer.imageFromBitmap(picture);
                mTextRecognizer.imageFromByteBuffer(byteBuffer, rotation);

            }else{
                Log.d("txtP", "onImageAvailable: image is null");
            }
            image.close();
        }
    };

    private ByteBuffer imageToByteBuffer(final Image image) {
        final Rect crop = image.getCropRect();
        final int width = crop.width();
        final int height = crop.height();

        final Image.Plane[] planes = image.getPlanes();
        final byte[] rowData = new byte[planes[0].getRowStride()];
        final int bufferSize = mPreviewSize.getWidth() * mPreviewSize.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        final ByteBuffer output = ByteBuffer.allocateDirect(bufferSize);

        int channelOffset = 0;
        int outputStride = 0;

        for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
            if (planeIndex == 0) {
                channelOffset = 0;
                outputStride = 1;
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1;
                outputStride = 2;
            } else if (planeIndex == 2) {
                channelOffset = width * height;
                outputStride = 2;
            }

            final ByteBuffer buffer = planes[planeIndex].getBuffer();
            final int rowStride = planes[planeIndex].getRowStride();
            final int pixelStride = planes[planeIndex].getPixelStride();

            final int shift = (planeIndex == 0) ? 0 : 1;
            final int widthShifted = width >> shift;
            final int heightShifted = height >> shift;

            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

            for (int row = 0; row < heightShifted; row++) {
                final int length;

                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted;
                    buffer.get(output.array(), channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (widthShifted - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);

                    for (int col = 0; col < widthShifted; col++) {
                        output.array()[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return output;
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(cameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                int deviceOrientation = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = new Size(rotatedWidth, rotatedHeight);

                mImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, ImageFormat.YUV_420_888, 1);
                mImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Log.d("cameraP", "connectCamera: Camera Permission Granted ");

                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {

                    Toast.makeText(this, "This app requires access to camera", Toast.LENGTH_SHORT).show();
                    Log.d("cameraP", "connectCamera: Camera Permission Needed ");
                    mainActivity.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);

                }
            } else {
                Toast.makeText(this, "this app requires access to camera and api level >= 23",Toast.LENGTH_SHORT).show();
             //   cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        if (surfaceTexture == null) Log.d("cameraPreview", "startPreview:surface texture is null ");
        surfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
        Surface previewSurface = new Surface(surfaceTexture);
        Surface mImageSurface = mImageReader.getSurface();

        try {
            if (previewSurface == null)
                Log.d("cameraPreview", "startPreview:preview texture is null ");

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            // mCaptureRequestBuilder.addTarget(mImageSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface /*mImageSurface*/), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                        Log.d("cameraPreview", "onConfigured: ");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                    Log.d("cameraPreview", "onConfigureFailed: ");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillImage() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
        Surface captureSurface = new Surface(surfaceTexture);

        Surface mImageSurface = mImageReader.getSurface();

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(captureSurface);
            mCaptureRequestBuilder.addTarget(mImageSurface);

            int rotation = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            mCameraDevice.createCaptureSession(Arrays.asList(captureSurface, mImageSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(mCaptureRequestBuilder.build(), null, mBackgroundHandler);

                        Toast.makeText(getApplicationContext(), "Image Captured", Toast.LENGTH_SHORT).show();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Unable to capture a image", Toast.LENGTH_SHORT).show();

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("cameraPreview", "onDestroy: ");
        stopBackgroundThread();
        closeCamera();
    }

    @Override
    public View onCreateInputView() { // this method is kind of similar to onCreate here i will define all of my things

        ConstraintLayout mConstraintLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.keyboard_texture_view, null);
        FrameLayout mFrameLayout = (FrameLayout) mConstraintLayout.findViewById(R.id.FrameLayout);


        mTextureView = (TextureView) mFrameLayout.findViewById(R.id.textureView2);
        mGraphicOverlay = (GraphicOverlay) mFrameLayout.findViewById(R.id.graphicOverlay);
        mTextRecognizer.mGraphicOverlay = mGraphicOverlay;

        RecogniseBtn = (Button) mConstraintLayout.findViewById(R.id.RecogniseBtn);

        clearBtn = mConstraintLayout.findViewById(R.id.ClearBtn);
        backspaceBtn = mConstraintLayout.findViewById(R.id.backspace);
        returnBtn = mConstraintLayout.findViewById(R.id.enter);
        spaceBtn = mConstraintLayout.findViewById(R.id.space);

        inputConnection = getCurrentInputConnection();


        mTextureView.setOnTouchListener(
                new TextureView.OnTouchListener() {
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                       // Log.d("textureView", "Width:" + mTextureView.getWidth() + " | Height:" + mTextureView.getHeight());
                        inputConnection = getCurrentInputConnection();

                        handleTouch(motionEvent); // The onTap Method is placed in ACTION_DOWN otherwise it registers multiple touches.
                        /*int pointerCount = motionEvent.getPointerCount();

                        for (int i = 0; i < pointerCount; i++) {
                            touchX = motionEvent.getX(i);
                            touchY = motionEvent.getY(i);
                            Log.d("location", "X:" + touchX + " Y:" + touchY);
                        }

                        //Log.d("eventTime", String.valueOf(motionEvent.getEventTime()));
                        //Log.d("eventTime", "Historical Time:"+ motionEvent.getHistoricalX((int) touchX));
                        */
                        return true;
                    }
                }
        );
        startBackgroundThread();

        RecogniseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputConnection = getCurrentInputConnection();
                buttonCount++;
                if (buttonCount % 2 == 0) {
                    mGraphicOverlay.clear();
                    mGraphicOverlay.clearElements();
                    startPreview();
                } else {
                    captureStillImage();
                }
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputConnection = getCurrentInputConnection();
                if (textLength != 0)
                    inputConnection.deleteSurroundingText(textLength, 0);
            }
        });

        backspaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputConnection = getCurrentInputConnection();
                inputConnection.deleteSurroundingText(1,0);
            }
        });

        spaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputConnection.commitText(" ",1);
            }
        });

        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputConnection = getCurrentInputConnection();
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
            }
        });

        if (mTextureView == null) {
            Toast.makeText(getApplicationContext(), "Texture View Is null", Toast.LENGTH_SHORT).show();
        }

        if (mTextureView.isAvailable()) {
            setupCamera(mPreviewWidth, mPreviewHeight);
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        return mConstraintLayout;

    }

    private int textLength = 0;

    private void onTap(float rawX, float rawY) {
        Log.d("onTap", "onCalled:");

        TextGraphic graphic = (TextGraphic) mGraphicOverlay.getGraphicAtLocation(rawX, rawY);
        FirebaseVisionText.TextBlock text = null;
        FirebaseVisionText.Line lineText = null;

        ClipboardManager mClipBoardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); /** clipboard manager*/
        ClipData mClip;

        if (graphic != null) {
            text = graphic.getTextBlock();
            lineText = graphic.getLineText();
            if (text != null && text.getText() != null) {
                textLength = text.getText().length();
                Log.d("TTS", "TextBlock Contents: " + text.getText());

                /**This loop is for correcting the formatting of the text detected from a TextBlock, as raw text obtained from textBlocks have messed up formatting.
                 * This loop puts the text in a proper paragraph form */
                for (int i = 0; i < text.getLines().size(); i++){
                    inputConnection.commitText(text.getLines().get(i).getText() + " ", textLength + 1);
                }

                /** For copying Detected text to clipboard */
                mClip = ClipData.newPlainText("detectedText", text.getText());
                mClipBoardManager.setPrimaryClip(mClip);


            }

            /*else if (lineText != null && lineText.getText() != null) {
                textLength = lineText.getText().length();
                Log.d("TTS", "TextLine Contents: " + lineText.getText());
                inputConnection.commitText(lineText.getText() + " ", textLength + 1);
            } */
            else {
                Log.d("TTS", "text data is null");
            }
        } else {
            Log.d("TTS", "No text detected / Graphic is null");
        }

    }


      void handleTouch(MotionEvent motionEvent) {
          int pointerCount = motionEvent.getPointerCount();

          for (int i = 0; i < pointerCount; i++) {
              touchX = (int) motionEvent.getX(i);
              touchY = (int) motionEvent.getY(i);

              int id = motionEvent.getPointerId(i);
              int action = motionEvent.getActionMasked();
              int actionIndex = motionEvent.getActionIndex();
              String actionString;

                      switch (action) {
                          case MotionEvent.ACTION_DOWN:
                              actionString = "DOWN";
                              onTap(touchX, touchY);
                              Log.d("actionCode", actionString);
                              break;
                          case MotionEvent.ACTION_UP:
                              actionString = "UP";
                              Log.d("actionCode", actionString);
                              break;
                          case MotionEvent.ACTION_POINTER_DOWN:
                              actionString = "PNTR DOWN";
                              Log.d("actionCode", actionString);
                              break;
                          case MotionEvent.ACTION_POINTER_UP:
                              actionString = "PNTR UP";
                              Log.d("actionCode", actionString);
                              break;
                          case MotionEvent.ACTION_MOVE:
                              actionString = "MOVE";
                              Log.d("actionCode", actionString);
                              break;
                          default:
                              actionString = "";
                              Log.d("actionCode", actionString);
                      }
          }
      }


    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

      inputConnection = getCurrentInputConnection();

       /* if (inputConnection != null) {
            switch (primaryCode) {
                case Keyboard.KEYCODE_DELETE:
                    CharSequence selectedText = inputConnection.getSelectedText(0);

                    if (TextUtils.isEmpty(selectedText)) {
                        inputConnection.deleteSurroundingText(1, 0);
                    } else {
                        inputConnection.commitText("", 1);
                    }

                    break;
                default:
                    char code = (char) primaryCode;
                    inputConnection.commitText(String.valueOf(code), 1);
            }
        }*/
    }

    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }


    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera2Api");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}