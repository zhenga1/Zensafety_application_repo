package com.example.zensafety;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class LogShowActivity extends AppCompatActivity implements View.OnClickListener{

    private File fileDir;
    private LinearLayout linearLayout;
    private File[] files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_show);
        File parent = Environment.getExternalStorageDirectory();
        fileDir = new File(parent, "Logs_for_zensafety");
        linearLayout = findViewById(R.id.linearlayoutlog);
        if(fileDir.exists())
        {
            /*File newfile = new File(fileDir, "JAVA.TEXT.SIMPLEDATAFORMAT@2FE62116.txt");
            if(newfile.exists())
            {
                newfile.delete();
            }*/
            files = fileDir.listFiles();
            for(int i=0;i<files.length;i++) {
                Button button = new Button(this);
                String curdate = files[i].getName();
                ListView.LayoutParams layoutParams = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT);
                button.setText(curdate.substring(0,curdate.length()-4));
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200));
                button.setTag(i);
                button.setTextColor(Color.parseColor("#000000"));
                button.setLayoutParams(layoutParams);
                button.setOnClickListener(this);
                linearLayout.addView(button);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int order = (int) view.getTag();
        //Toast.makeText(this,date, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(),LogActivity.class);
        intent.putExtra("File:",files[order]);
        startActivity(intent);

    }
    public void log_back_button(View view){ finish(); }
}