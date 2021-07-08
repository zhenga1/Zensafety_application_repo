package com.example.zensafety;

import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected boolean hasPermissions(String... permissions){
        for (String permission : permissions)
            if(checkSelfPermission(permission)!= PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    public void requestPermission(int requestCode, String... permissions) {
        for(String permission: permissions) {
            if (shouldShowRequestPermissionRationale(permission)) {
                showLongToast("Permission is required for this application.");
                return;
            }
        }

        requestPermissions(permissions, requestCode);
    }

    public void showToast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }

    public void showLongToast(String text){
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_LONG
        ).show();
    }
}
