package com.example.zensafety;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.zensafety.Texts.OverlayView;
import com.example.zensafety.tflite.ImageUtils;
import com.example.zensafety.tflite.MultiBoxTracker;
import com.example.zensafety.tflite.ObjectDetectionModel;
import com.example.zensafety.tflite.ObjectDetector;
import com.example.zensafety.tools.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class NewCameraActivity extends CameraActivity {
    private Logger logger = new Logger(this.getClass());

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean MAINTAIN_ASPECT = false;

    private static short state=-1;
    private static Double rating=Double.POSITIVE_INFINITY;
    private static final String MODEL_FILE = "objectdetect.tflite";
    private static final String LABELS_FILE = "file:///android_asset/objectlabelmap.txt";
    private static final int MODEL_INPUT_SIZE = 300;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE = 0.5f;
    public static String states = "Not Secure", moreinfo = "Processing";
    private boolean HAS_FRONT_CAMERA = false;

    private ObjectDetector detector;

    private long timestamp = 0;
    private boolean computingImage = false;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private OverlayView trackingOverlay;
    private MultiBoxTracker tracker;
    private TextView tv_debug;
    private LinearLayout security_window;
    private TextView basic_stat,detailed_stat,whats_secured;
    private final float Threshold_security_rating = 0.5f;
    private File fileDirLocation,file;
    private boolean error=false;
    private ArrayList<List<String>> getdata = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makefile();
        showWindow();
        tv_debug = findViewById(R.id.tv_debug);
    }
    private void makefile(){
        File parent = Environment.getExternalStorageDirectory();
        fileDirLocation = new File(parent, "Logs_for_zensafety");
        /*String[] children = fileDirLocation.list();
        for (int i = 0; i < children.length; i++)
        {
            new File(fileDirLocation, children[i]).delete();
        }*/
        if(!fileDirLocation.exists())
        {
            fileDirLocation.mkdirs();
        }

        file = new File(fileDirLocation,new SimpleDateFormat("MM-dd-yyyy").format(new Date())+".txt");
        if(!file.exists())
        {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                error = true;
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        computingImage = false;
        setText();
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        HAS_FRONT_CAMERA = hasFrontCamera();


        try {
            detector =
                    ObjectDetectionModel.create(
                            getAssets(),
                            MODEL_FILE,
                            LABELS_FILE,
                            MODEL_INPUT_SIZE,
                            true
                    );
        } catch (final IOException e) {
            logger.e("Module could not be initialized");
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        int sensorOrientation = rotation - getScreenOrientation();

        logger.i(getString(R.string.camera_orientation_relative, sensorOrientation));

        logger.i(getString(R.string.initializing_size, previewWidth, previewHeight));
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        MODEL_INPUT_SIZE, MODEL_INPUT_SIZE,
                        sensorOrientation,
                        MAINTAIN_ASPECT
                );
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        //IMPORTANT LINE
        tracker = new MultiBoxTracker(this);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> {
                    tracker.draw(canvas);
//                    if (BuildConfig.DEBUG) {
//                        tracker.drawDebug(canvas);
//                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    private boolean hasFrontCamera() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK) return true;
        }

        return false;
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingImage) {
            readyForNextImage();
            return;
        }
        computingImage = true;
        logger.i("Preparing image " + currTimestamp + " for module in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        runInBackground(
                () -> {
                    final long startTime = SystemClock.uptimeMillis();
                    // 輸出結果
                    // importantthing
                    final List<ObjectDetector.Recognition> results = detector.recognizeImage(
                            HAS_FRONT_CAMERA? croppedBitmap : flip(croppedBitmap));
                    long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                    logger.i("Running detection on image " + lastProcessingTimeMs);

                    final List<ObjectDetector.Recognition> mappedRecognitions = new LinkedList<>();
                    ArrayList<Float> confidences = new ArrayList<>();
                    for (final ObjectDetector.Recognition result : results) {
                        final RectF location = result.getLocation();
                        if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE) {
                            if(ChooseItemActivity.chosenone.equals(result.getTitle()))
                            {
                                confidences.add(result.getConfidence());
                            }
                            cropToFrameTransform.mapRect(location);
                            result.setLocation(location);
                            mappedRecognitions.add(result);
                        }
                    }

                    tracker.trackResults(mappedRecognitions, currTimestamp);
                    trackingOverlay.postInvalidate();
                    computingImage = false;

                    runOnUiThread(
                            () -> {
                                calculateSecurity(confidences);
                                tv_debug.setText("Time taken to process frame"+lastProcessingTimeMs +"ms");
                                tv_debug.setVisibility(View.VISIBLE);
                            });
                });
    }
    private void storeData(){
        List<String> lis = Arrays.asList(new String[3]);
        lis.set(0, ChooseItemActivity.chosenone);
        lis.set(1, states);
        lis.set(2,moreinfo);
        getdata.add(lis);

    }
    private void writeToFile() throws IOException{
       // FileOutputStream fOut = new FileOutputStream(file);

        //BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fOut));
        FileWriter fw = new FileWriter(file, true);
        for (int i = 0; i < getdata.size(); i++) {
            //One, two and three at the same time
            fw.write("Secured Object:  " + getdata.get(i).get(0));
            fw.write(System.getProperty( "line.separator" ));
            fw.write("Security Status:  " + getdata.get(i).get(1));
            fw.write(System.getProperty( "line.separator" ));
            fw.write("More Info For the Security Rating:  " + getdata.get(i).get(2));
            fw.write(System.getProperty( "line.separator" ));
            fw.write("Time: " + new SimpleDateFormat("MM-dd-yyyy 'at' HH:mm:ss").format(new Date()));
            fw.write(System.getProperty( "line.separator" ));
            fw.write(System.getProperty( "line.separator" ));
        }
        fw.flush();
        fw.close();
    }

    private void calculateSecurity(ArrayList<Float> confidences){
        if(confidences.size()<=0)
        {
            detailed_stat.setText(Integer.toString(0));
            if(ChooseItemActivity.chosenone.equals("nothing"))
            {
                basic_stat.setText("Not Applicable");
            }else {
                basic_stat.setText("Not Found");
            }
            return;
        }
        Float f = Collections.max(confidences);
        Double newrating = f + (1-f)/(2.5*confidences.size());
        short status=0;
        if(newrating >= 0.909f)status=10;
        else if(newrating >= 0.8181f) status = 9;
        else if(newrating >= 0.7272f) status = 8;
        else if(newrating >= 0.6363f) status = 7;
        else if(newrating >= 0.5454f) status = 6;
        else if(newrating >= 0.4545f) status = 5;
        else if(newrating >= 0.3636f) status = 4;
        else if(newrating >= 0.2727f) status = 3;
        else if(newrating >= 0.1818f) status = 2;
        else if(newrating >= 0.0909f) status = 1;
        state = status;
        detailed_stat.setText(Short.toString(status));
        rating = newrating;
        if(newrating>Threshold_security_rating) basic_stat.setText("Secure");
        else basic_stat.setText("Not Secure");
        states = (String) basic_stat.getText();
        moreinfo = (String) detailed_stat.getText();
        storeData();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            writeToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Bitmap flip(Bitmap d) {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        Bitmap dst = Bitmap.createBitmap(d, 0, 0, d.getWidth(), d.getHeight(), m, false);
        dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return dst;
    }
    public void setText(){
        if(state==-1){
            basic_stat.setText("Processing");
            states = "Processing";
        }else{
            basic_stat.setText(Short.toString(state));
            states = Short.toString(state);
        }
        if(rating==Double.POSITIVE_INFINITY){
            detailed_stat.setText("Processing");
            moreinfo = "Processing";
        }else{
            if(rating>Threshold_security_rating) {
                basic_stat.setText("Secure");
                states = "Secure";
            }
            else {
                basic_stat.setText("Not Secure");
                states = "Not Secure";
            }
        }
        storeData();
        whats_secured.setText(ChooseItemActivity.chosenone);
    }
    public void showWindow(){
        LayoutInflater layoutInflater = getLayoutInflater();
        security_window = (LinearLayout)layoutInflater.inflate(R.layout.security_info_window_view,null);
        basic_stat = (TextView)security_window.findViewById(R.id.basicsecuritystat);
        detailed_stat = (TextView)security_window.findViewById(R.id.detailedsecuritystat);
        whats_secured = (TextView)security_window.findViewById(R.id.whatsbeingsecured);
        basic_stat.setVisibility(View.VISIBLE);
        detailed_stat.setVisibility(View.VISIBLE);
        whats_secured.setVisibility(View.VISIBLE);
        setText();
        FrameLayout parent = (FrameLayout)findViewById(R.id.containerthree);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT);
        parent.addView(security_window,layoutParams);
    }
}
