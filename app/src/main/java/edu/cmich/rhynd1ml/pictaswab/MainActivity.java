package edu.cmich.rhynd1ml.pictaswab;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextureView textureView;

    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        tutorialCheck();
    }

    /**
     *  On Texture available, open camera.
     */
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform your image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    /**
     *  Called after camera is opened or closed.
     *  Create preview if camera opened.
     *  Disconnect camera if closed.
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Toast.makeText(MainActivity.this, "Error opening camera.", Toast.LENGTH_SHORT).show();
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /**
     *  Check if tutorial was passed
     *  if now, show it.
     */
    private void tutorialCheck() {
        SharedPreferences sp = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.app_name), MODE_PRIVATE);
        if (sp.contains("Tutorial_Complete")) {
            if (sp.getBoolean("Tutorial_Complete", false)) {
                return;
            }
        }
        Intent intent = new Intent(this, TutorialPanel.class);
        startActivity(intent);
    }

    /**
     * Open swab overlay
     * @param colorInfo
     */
    private void openSwabPanel(HashMap colorInfo) {
        Intent intent = new Intent(this, SwabPanel.class);
        intent.putExtra("COLOR_INFO", colorInfo);
        startActivity(intent);
    }

    /**
     * Start the background thread that the camera preview uses
     */
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Close the background thread that the camera preview uses
     */
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display Camera content on screen in surface.
     */
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            Toast.makeText(MainActivity.this, "Camera Surface failed to load.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Get Camera and Dimensions. Request permissions if needed.
     */
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Camera failed to open.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Update the surface with new camera info
     */
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the Camera device
     */
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    /**
     * Get the most Common Element in an Integer List
     * @param list
     * @return
     */
    private static Integer mostCommonElement(List<Integer> list) {
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        for(int i=0; i< list.size(); i++) {
            Integer frequency = map.get(list.get(i));

            if(frequency == null) {
                map.put(list.get(i), 1);
            } else {
                map.put(list.get(i), frequency+1);
            }
        }
        int mostCommonKey = 0;
        int maxValue = -1;
        for(HashMap.Entry<Integer, Integer> entry: map.entrySet()) {
            if(entry.getValue() > maxValue) {
                mostCommonKey = entry.getKey();
                maxValue = entry.getValue();
            }
        }

        return mostCommonKey;
    }

    /**
     * Base64 encode image
     * @param bmp
     * @return
     */
    private String getBase64(Bitmap bmp) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * Create Hashmap of the bmp information
     * Includes HEX, RGB, HSV and the Base64 encoded image
     * @param bmp
     * @return
     */
    private HashMap getColorInfo(Bitmap bmp) {
        HashMap<String, String> colorInfo = new HashMap<>();

        int smoothFactor = 10;
        int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
        bmp.getPixels(pixels, 0, bmp.getWidth(),0,0, bmp.getWidth(), bmp.getHeight());
        List<Integer> R = new ArrayList<>();
        List<Integer> G = new ArrayList<>();
        List<Integer> B = new ArrayList<>();

        for (int y = 0; y < bmp.getHeight(); y++) {
            for (int x = 0; x < bmp.getWidth(); x++) {
                int index = y * bmp.getWidth() + x;
                R.add((pixels[index] >> 16) & 0xff);
                G.add((pixels[index] >> 8) & 0xff);
                B.add(pixels[index] & 0xff);
            }
        }

        int r = mostCommonElement(R); if (r <= smoothFactor) { r = 0;}
        int g = mostCommonElement(G); if (g <= smoothFactor) { g = 0;}
        int b = mostCommonElement(B); if (b <= smoothFactor) { b = 0;}
        float[] hsv = new float[3];
        Color.RGBToHSV(r,g,b,hsv);

        colorInfo.put("R", String.valueOf(r));
        colorInfo.put("G", String.valueOf(g));
        colorInfo.put("B", String.valueOf(b));
        colorInfo.put("H", String.valueOf(hsv[0]));
        colorInfo.put("S", String.valueOf(hsv[1]));
        colorInfo.put("V", String.valueOf(hsv[2]));
        colorInfo.put("DECIMAL", String.valueOf((0xff000000 | (r << 16) | (g << 8) | b)));
        colorInfo.put("HEX", String.format("#%02x%02x%02x", r, g, b));
        colorInfo.put("AREA", getBase64(bmp));

        return colorInfo;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * on touch, get the area, expand by radius to create bitmap
     * Send the bitmap to the swabPanel for processing
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(cameraDevice != null) {
            closeCamera();
        } else {
            return false;
        }

        int radius = 25;
        int x = (int)event.getX();
        int y = (int)event.getY();

        if ((x-radius) < 0) {
            x += radius;
        }
        if ((y - radius) < 0) {
            y += radius;
        }
        Bitmap touchedBmp = Bitmap.createBitmap(
                textureView.getBitmap(),
                x-radius,
                y-radius,
                radius*2,
                radius*2
        ).copy(Bitmap.Config.RGB_565, false);

//        getBase64(touchedBmp);
        HashMap info = getColorInfo(touchedBmp);

        openSwabPanel(info);
        return false;
    }
}