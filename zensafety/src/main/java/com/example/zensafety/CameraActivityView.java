package com.example.zensafety;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class CameraActivityView extends AppCompatActivity {
    private TextureView textureView;
    private String cameraid;
    private Handler handler;
    private HandlerThread handlerthread;
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(),"Surface Texture is available", Toast.LENGTH_LONG);
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
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice=null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice=null;
        }
    };
    private void setupCamera(int width,int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId:cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        !=CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                cameraid=cameraId;
                return;

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);

    }
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(!textureView.isAvailable()){
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }else{
            setupCamera(textureView.getWidth(),textureView.getHeight());
        }
    }

    @Override
    protected void onPause(){
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
}