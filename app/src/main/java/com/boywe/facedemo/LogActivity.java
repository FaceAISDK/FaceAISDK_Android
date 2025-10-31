package com.boywe.facedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class LogActivity extends AppCompatActivity {
    private TextView logTextView;
    private ScrollView scrollView;
    private Button refreshButton;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        setupToolbar();
        initViews();
        setupClickListeners();
        loadLogs();
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
        logTextView = findViewById(R.id.logTextView);
        scrollView = findViewById(R.id.scrollView);
        refreshButton = findViewById(R.id.refreshButton);
        clearButton = findViewById(R.id.clearButton);
        
        // 添加调试按钮
        Button debugButton = findViewById(R.id.btn_debug);
        if (debugButton != null) {
            debugButton.setOnClickListener(v -> showDebugInfo());
        }
        

    }

    private void setupClickListeners() {
        refreshButton.setOnClickListener(v -> {
            loadLogs();
            Toast.makeText(this, "日志已刷新", Toast.LENGTH_SHORT).show();
        });

        clearButton.setOnClickListener(v -> {
            BootReceiver.clearLogs(this);
            loadLogs();
            Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void showDebugInfo() {
        DebugHelper debugHelper = new DebugHelper(this);
        String debugReport = debugHelper.generateDebugReport();
        
        // 创建新的Activity或Dialog显示调试信息
        Intent intent = new Intent(this, DebugActivity.class);
        intent.putExtra("debug_report", debugReport);
        startActivity(intent);
    }

    private void loadLogs() {
        String logs = BootReceiver.getLogs(this);
        logTextView.setText(logs);
        
        // 滚动到底部显示最新日志
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLogs(); // 页面恢复时刷新日志
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}