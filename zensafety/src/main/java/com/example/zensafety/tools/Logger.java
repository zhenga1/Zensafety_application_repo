package com.example.zensafety.tools;

import android.util.Log;

import com.example.zensafety.BuildConfig;

public class Logger {
    private String tag;
    private boolean isDebug = BuildConfig.DEBUG;

    public Logger(Class<?> clazz) {
        tag = clazz.getSimpleName();
    }

    public void d(String msg) {
        if(isDebug)
            Log.d(tag, msg);
    }

    public void e(String msg) {
        if(isDebug)
            Log.e(tag, msg);
    }

    public void i(String msg) {
        if(isDebug)
            Log.i(tag, msg);
    }

    public void w(String msg) {
        if(isDebug)
            Log.w(tag, msg);
    }
}

