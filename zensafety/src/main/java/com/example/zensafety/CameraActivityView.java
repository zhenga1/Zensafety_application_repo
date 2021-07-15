package com.example.zensafety;

import android.Manifest;
import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.zensafety.Texts.OverlayView;
import com.example.zensafety.tflite.ImageUtils;
import com.example.zensafety.tflite.MultiBoxTracker;
import com.example.zensafety.tflite.ObjectDetectionModel;
import com.example.zensafety.tflite.ObjectDetector;
import com.example.zensafety.tools.Logger;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class CameraActivityView extends AppCompatActivity {
    private static final boolean MAINTAIN_ASPECT = false;
    private TextureView textureView;
    private ImageButton record;
    private String cameraid;
    private Size previewsize;
    private Size videosize = new Size(640,480);
    private MediaRecorder mediaRecorder;
    private int totalrotation;
    private static final float MINIMUM_CONFIDENCE = 0.5f;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private Handler handler;
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0, REQUEST_STORAGE_PERMISSION_RESULT=1;
    private HandlerThread handlerthread;
    private boolean recordingornot=false;
    private static final String MODEL_FILE = "objectdetect.tflite";
    private static final String LABELS_FILE = "file:/objectlabelmap.txt";
    private Chronometer chronometer;
    private File videofolder;
    private String videofilename;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    private ObjectDetector detector;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 1);
        ORIENTATIONS.append(Surface.ROTATION_180, 2);
        ORIENTATIONS.append(Surface.ROTATION_270, 3);
    }

    private Logger logger = new Logger(this.getClass());
    private int MODEL_INPUT_SIZE = 300;
    private Bitmap rgbFrameBitmap,croppedBitmap;
    private MultiBoxTracker tracker;
    private Matrix frameToCropTransform,cropToFrameTransform;
    private OverlayView overlayView;
    private long timestamp;
    private boolean computingImage;

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum(((long) o1.getWidth() * o1.getHeight()) / ((long) o2.getWidth() * o2.getHeight()));
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width,height);
            connectCamera();

            //Toast.makeText(getApplicationContext(), "Surface Texture is available", Toast.LENGTH_LONG);
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
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameradevicestatecallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            if(recordingornot){
                try {
                    createFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mediaRecorder.start();
            }
            else
            {
                startPreview();
            }
            Toast.makeText(getApplicationContext(),"Camera has now been connected",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceorientation = getWindowManager().getDefaultDisplay().getRotation();
                totalrotation = sensorToDeviceRotation(cameraCharacteristics, deviceorientation);
                boolean swaprotation = totalrotation == 90 || totalrotation == 270;
                int rotatedWidth = width, rotatedHeight = height;
                if (swaprotation) {
                    rotatedHeight = width;
                    rotatedWidth = height;
                }
                previewsize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                videosize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class),rotatedWidth,rotatedHeight);
                cameraid = cameraId;
                return;

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        checkperm();
        try {
            cameraManager.openCamera(cameraid, cameradevicestatecallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void startRecord(){
        try {
            setupMediaRecorder();
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewsize.getWidth(), previewsize.getHeight());
            Surface previewsurface = new Surface(surfaceTexture);
            Surface recordsurface = mediaRecorder.getSurface();
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewsurface);
            mCaptureRequestBuilder.addTarget(recordsurface);
            cameraDevice.createCaptureSession(Arrays.asList(previewsurface, recordsurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },null);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    private void startPreview(){
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewsize.getWidth(),previewsize.getHeight());
        Surface previewsurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewsurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewsurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(mCaptureRequestBuilder.build(),null,handler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(),"Unable to setup camera preview.",Toast.LENGTH_LONG).show();
                        }
                    },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void checkperm(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
            {
                Toast.makeText(this,"Video app requires the access to the camera",Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION_RESULT);
        }
        return;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        createvideofolder();
        mediaRecorder = new MediaRecorder();
        textureView = (TextureView) findViewById(R.id.textureView);
        record = (ImageButton)findViewById(R.id.imageButton);
        record.setOnClickListener(view -> {
            if(recordingornot)
            {
                recordingornot = false;
                record.setImageResource(R.mipmap.zenbo_start_recording);
                mediaRecorder.stop();
                mediaRecorder.reset();
                startPreview();
            }else{
                checkwritestoragepermission();
            }
        });
    }
    public void backbitten(View view)
    {
        finish();
    }
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        computingImage=false;
        if(!textureView.isAvailable()){
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }else{
            setupCamera(textureView.getWidth(),textureView.getHeight());
            connectCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_CAMERA_PERMISSION_RESULT)
        {
            if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),"APPLICATION WILL NOT RUN WITHOUT CAMERA", Toast.LENGTH_LONG).show();
            }
        }
        if(requestCode==REQUEST_STORAGE_PERMISSION_RESULT)
        {
            if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),"APPLICATION IS NOW NOT ABLE TO STORE DATA", Toast.LENGTH_LONG).show();
            }
            else{
                recordingornot = true;
                record.setImageResource(R.mipmap.zenbo_recording);
                try {
                    createFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(),"DATA PERMISSION HAS BEEN SUCCESSFULY GRANTED", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPause(){
        closecamera();
        super.onPause();
        stopBackgroundThread();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus)
        {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
    private void closecamera(){
        if(cameraDevice!=null)
        {
            cameraDevice.close();
            cameraDevice=null;
        }
    }
    private void startBackgroundThread(){
        handlerthread = new HandlerThread("CameraHandlerVideoImage");
        handlerthread.start();
        handler = new Handler(handlerthread.getLooper());
    }
    private void stopBackgroundThread(){
        handlerthread.quitSafely();
        try{
            handlerthread.join();
            handlerthread = null;
            handler = null;
        }catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    private static int sensorToDeviceRotation(CameraCharacteristics characteristics,int deviceOrientation)
    {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation+deviceOrientation+360) % 360;
    }
    private static Size chooseOptimalSize(Size[] choices,int width, int height)
    {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option:choices){
            if(option.getHeight() == option.getWidth()*height/width
                    && option.getWidth()>=width && option.getHeight()>=height)
            {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size()>0)
        {
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        else{
            return choices[0];
        }
    }
    private void createvideofolder(){
        File videoFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        videofolder = new File(videoFile,"ZensafetyVideoImage");
        if(!videofolder.exists())
        {
            videofolder.mkdirs();
        }
    }
    private File createFileName() throws IOException{
        String timestamp = new SimpleDateFormat("yyyy.MM.dd  'at' HH:mm:ss z").format(new Date());
        String prepend = "Video_"+timestamp+"__";
        File videofile = File.createTempFile(prepend,".mp4",videofolder);
        videofilename = videofile.getAbsolutePath();
        Log.i("cameraactivityview",videofilename);
        return videofile;
    }
    private void checkwritestoragepermission(){
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M){
            recordingornot = true;
            record.setImageResource(R.mipmap.zenbo_recording);
            try {
                createFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            startRecord();
            mediaRecorder.start();
        }else{
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                {
                    Toast.makeText(this,"Permission is needed to save file.",Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_STORAGE_PERMISSION_RESULT);
            }else if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            {
                if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO))
                {
                    Toast.makeText(this,"Permission is needed to record audio.",Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_STORAGE_PERMISSION_RESULT);
            }
            else{
                recordingornot = true;
                record.setImageResource(R.mipmap.zenbo_recording);
                try {
                    createFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mediaRecorder.start();
            }
        }
    }
    private void setupMediaRecorder() throws IOException{
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videofilename);
        mediaRecorder.setVideoEncodingBitRate(1000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videosize.getWidth(),videosize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOrientationHint(totalrotation);
        mediaRecorder.prepare();
    }
}