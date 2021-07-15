package com.example.zensafety;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ChooseItemActivity extends AppCompatActivity implements View.OnClickListener{
    private LinearLayout linearLayout;
    public static String chosenone = "nothing";
    private ArrayList<String> strings = new ArrayList<String>();
    private Toast prevtoast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_item);
        linearLayout = (LinearLayout)findViewById(R.id.linearlayout);
        makebuttons("???");
    }
    public void backagain(View view){
        finish();
    }
    private void makebuttons(String ignore){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("objectlabelmap.txt")));

            // do reading, usually loop until end of file reading
            String mLine = "";
            int count = 1;
            while ((mLine = reader.readLine()) != null) {
                //process line
                if(mLine.contains(ignore)) continue;
                Button button = new Button(this);
                ListView.LayoutParams layoutParams = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,ListView.LayoutParams.WRAP_CONTENT);
                button.setText(mLine);
                button.setBackgroundColor(ContextCompat.getColor(this,R.color.teal_200));
                button.setTag(count);
                button.setTextColor(Color.parseColor("#000000"));
                button.setLayoutParams(layoutParams);
                strings.add(mLine);
                button.setOnClickListener(this);
                linearLayout.addView(button);
                count++;
            }
        } catch (IOException e) {
            //log the exception
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        int order = (int) view.getTag();
        String string = strings.get(order-1);
        //DO SOMETHING
        if(prevtoast!=null)
        {
            prevtoast.cancel();
        }
        Toast toast = Toast.makeText(getApplicationContext(),string+" has been chosen succcessfully!", Toast.LENGTH_SHORT);
        toast.show();
        prevtoast = toast;
        chosenone = string;
    }
}