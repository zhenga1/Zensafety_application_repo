package com.example.zensafety.tflite;

import android.graphics.Bitmap;

public interface MultipleClassifier {
    float[] recognizeImage(Bitmap bitmap);

    void setNumThreads(int num_threads);
}

