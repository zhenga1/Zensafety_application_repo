package com.example.zensafety;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.SpeakConfig;
import com.robot.asus.robotactivity.RobotActivity;

import org.json.JSONObject;

public class NotificationView extends RobotActivity {
    public static final int requestCodeCamera = 0,requestCodeNotifications=1,requestCodeChoice=2, requestCodeSecurity=3;
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
    @Override
    public void onStart(){
        super.onStart();
       // robotAPI.robot.setExpression(RobotFace.HAPPY);

    }
    public NotificationView(){
        super(robotCallback,robotListenCallback);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_view);
        SpeakConfig config = new SpeakConfig();
        config.volume(60);
        robotAPI.robot.speak("Zensafety can help you secure your most valued posessions. Feel free to explore!",config);
    }
    public void changewhattosecure(View view)
    {
        Intent intent = new Intent(getApplicationContext(),ChooseItemActivity.class);
        startActivityForResult(intent,requestCodeChoice);
    }
    public void seezenbo(View view)
    {
        Intent intent = new Intent(getApplicationContext(),CameraActivityView.class);
        startActivityForResult(intent,requestCodeCamera);
    }
    public void checksecurity(View view)
    {
        Intent intent = new Intent(getApplicationContext(),NewCameraActivity.class);
        startActivityForResult(intent,requestCodeSecurity);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode) {
            case requestCodeCamera:
                //codeblock
                break;
            case requestCodeChoice:
                //codeblock
                break;
            case requestCodeNotifications:
                //codeblock
                break;
            case requestCodeSecurity:
                //codeblock
                break;
            default:
                break;

        }

    }
    public void viewnotifications(View view)
    {
        Intent intent = new Intent(getApplicationContext(),Zenbostate.class);
        startActivityForResult(intent,requestCodeNotifications);
    }
    public void goback(View view)
    {
        finish();
    }
}