package com.example.geomind;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LogoTime extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);

        Thread t=new Thread()
        {
            @Override
            public void run() {
                try {
                    sleep(2000);
                }catch (InterruptedException e)
                {
                  e.printStackTrace();
                }finally {
                    Intent intent=new Intent(LogoTime.this, MainActivity.class);
                startActivity(intent);
                finish();}
            }
        };
        t.start();
    }
}
