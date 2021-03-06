package com.example.zensafety;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.robot.asus.robotactivity.RobotActivity;

import org.json.JSONObject;

public class Zenbostate extends RobotActivity {
    public TextView secured, moreinfo;
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
    public Zenbostate(RobotCallback robotCallback, RobotCallback.Listen robotListenCallback) {
        super(robotCallback, robotListenCallback);
    }
    public Zenbostate()
    {
        super(robotCallback,robotListenCallback);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zenbostate);
        secured = (TextView)findViewById(R.id.secured);
        moreinfo = (TextView)findViewById(R.id.moreinfo);
        secured.setText(NewCameraActivity.states);
        moreinfo.setText("Current security status and more information  "+NewCameraActivity.moreinfo);
    }
    public void showlog(View view){
        //OPEN FILE WITH LOG
        Intent intent = new Intent(getApplicationContext(),LogShowActivity.class);
        startActivity(intent);

    }
    public void gobackzenbostate(View view){
        finish();
    }
}