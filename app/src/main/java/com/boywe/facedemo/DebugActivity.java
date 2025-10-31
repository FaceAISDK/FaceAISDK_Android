package com.boywe.facedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DebugActivity extends AppCompatActivity {

    private TextView debugTextView;
    private Button copyButton;
    private String debugReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        setupToolbar();
        initViews();
        loadDebugReport();
        setupClickListeners();
    }

    private void setupToolbar() {
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // 显示返回按钮，禁用默认标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initViews() {
        debugTextView = findViewById(R.id.tv_debug_info);
        copyButton = findViewById(R.id.btn_copy_debug);
        

    }

    private void loadDebugReport() {
        debugReport = getIntent().getStringExtra("debug_report");
        if (debugReport == null) {
            // 如果没有传入报告，生成新的
            DebugHelper debugHelper = new DebugHelper(this);
            debugReport = debugHelper.generateDebugReport();
        }
        debugTextView.setText(debugReport);
    }

    private void setupClickListeners() {
        copyButton.setOnClickListener(v -> copyToClipboard());
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("调试报告", debugReport);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "调试报告已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}