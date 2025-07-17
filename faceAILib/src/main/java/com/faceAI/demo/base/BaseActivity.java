package com.faceAI.demo.base;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 方便根据Demo App 找到对应的代码
 *
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(this,getClass().getSimpleName(),Toast.LENGTH_SHORT).show();
    }
}