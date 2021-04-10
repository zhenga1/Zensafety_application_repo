package com.example.zensafety;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.robot.asus.robotactivity.RobotActivity;

import org.json.JSONObject;

public class NotificationView extends RobotActivity {
    public int requestCodeCamera = 0;
    public static RobotCallback robotCallback = new RobotCallback() {
        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
        }

        @Override
        public void initComplete() {
            super.initComplete();

        }
    };

    public static RobotCallback.Listen robotListenCallback = new RobotCallback.Listen() {
        @Override
        public void onFinishRegister() {

        }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) {

        }

        @Override
        public void onSpeakComplete(String s, String s1) {

        }

        @Override
        public void onEventUserUtterance(JSONObject jsonObject) {

        }

        @Override
        public void onResult(JSONObject jsonObject) {

        }

        @Override
        public void onRetry(JSONObject jsonObject) {

        }
    };
    public NotificationView(RobotCallback robotCallback, RobotCallback.Listen robotListenCallback) {
        super(robotCallback, robotListenCallback);
    }
    public NotificationView(){
        super(robotCallback,robotListenCallback);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_view);
    }
    public void changewhattosecure(View view)
    {

    }
    public void checksecurity(View view)
    {
        Intent intent = new Intent(getApplicationContext(),CameraActivityView.class);
        startActivityForResult(intent,requestCodeCamera);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //dosomething
    }
    public void viewnotifications(View view)
    {
        Intent intent = new Intent(getApplicationContext(),Zenbostate.class);
        startActivity(intent);
    }
    public void goback(View view)
    {
        finish();
    }
}