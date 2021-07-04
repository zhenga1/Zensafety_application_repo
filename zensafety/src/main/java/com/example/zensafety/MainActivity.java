package com.example.zensafety;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.asus.robotframework.API.DialogSystem;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.SpeakConfig;
import com.robot.asus.robotactivity.RobotActivity;

import org.json.JSONObject;

public class MainActivity extends RobotActivity {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpeakConfig config = new SpeakConfig();
        config.volume(60);
        config.speed(100);
        robotAPI.robot.speak("Hello world. I am Zenbo Junior and this is Zensafety at your service. Nice to meet you.",config);
    }
    @Override
    protected void onStart() {

        super.onStart();
        //robotAPI.robot.setExpression(RobotFace.HAPPY);
        //robotAPI.robot.speak("Hello world. I am Zenbo Junior and this is Zensafety at your service. Nice to meet you.");
        setContentView(R.layout.activity_main);
        //robotAPI.release();
    }
    public MainActivity() {
        super(robotCallback, robotListenCallback);
        //zenbo = robotAPI.robot;
    }
    public void launchapp(View view)
    {
        Intent intent = new Intent(getApplicationContext(),NotificationView.class);
        startActivity(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    public void aboutus(View view){
        Intent intent = new Intent(getApplicationContext(),Aboutus.class);
        startActivity(intent);
    }
}
