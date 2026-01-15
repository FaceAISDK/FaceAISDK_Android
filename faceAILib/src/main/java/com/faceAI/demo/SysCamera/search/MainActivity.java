//package com.example.demo.nanohttpd;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Picture;
//import android.net.wifi.WifiInfo;
//import android.net.wifi.WifiManager;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.ai.face.core.engine.FaceAISDKEngine;
//import com.ai.face.faceSearch.search.FaceSearchFeatureManger;
//import com.ai.face.faceSearch.search.Image2FaceFeature;
//import com.example.demo.R;
//import com.example.demo.utils.CusUtil;
//import com.example.demo.utils.QRUtils;
//import com.faceAI.demo.FaceSDKConfig;
//
//import java.io.File;
//import java.io.IOException;
//
//public class MainActivity extends AppCompatActivity {
//    private static final String TAG = "MainActivity";
//    private HttpServer mHttpd;
//    private TextView mUploadInfo, mDownloadInfo;
//    private ProgressBar mProgressBar;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main_http);
//
//        // 找到 ImageView
//        ImageView qrCodeImage = findViewById(R.id.qr_code_image);
//
//        // 获取 WiFi IP（使用 CusUtil 工具类）
//        String ipAddr = CusUtil.getWifiIpaddr(); // 注意：这个方法依赖 MyApplication.CONTEXT
//        String uploadUrl = "http://" + ipAddr + ":8080/upload.html";
//
//        // 生成二维码
//        Bitmap qrBitmap = QRUtils.createQRCode(uploadUrl, 300); // 300x300 像素
//        if (qrBitmap != null) {
//            qrCodeImage.setImageBitmap(qrBitmap);
//        }
//
//        // 原有逻辑：显示服务器信息文本
//        TextView tv = (TextView) findViewById(R.id.server_info);
//        StringBuilder serverInfo = new StringBuilder()
//                .append("HTTP Server Address: ").append(uploadUrl).append("\n")
//                .append("Scan the QR code above to open upload page in WeChat");
//        tv.setText(serverInfo);
//
//        mUploadInfo = (TextView) findViewById(R.id.upload_info);
//        mDownloadInfo = (TextView) findViewById(R.id.download_info);
//        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
//
//        mHttpd = new HttpServer(8080);
//        mHttpd.setOnStatusUpdateListener(new HttpServer.OnStatusUpdateListener() {
//            @Override
//            public void onUploadingProgressUpdate(final int progress) {
//                MainActivity.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mProgressBar.setProgress(progress);
//                    }
//                });
//            }
//
//            @Override
//            public void onUploadingFile(final File file, final boolean done) {
//                MainActivity.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (done) {
//                            mUploadInfo.setText("上传照片:" + file.getName() + " done!");
////                            File file = new File("/sdcard/Pictures/face.jpg");
//                            Bitmap bitmap = getBitmapFromFileSafe(file, 1920, 720); // 缩放
//                            Image2FaceFeature.getInstance(getApplicationContext()).getFaceFeatureByBitmap(bitmap, "周上", new Image2FaceFeature.Callback() {
//                                @Override
//                                public void onSuccess(@NonNull Bitmap bitmap, @NonNull String s, @NonNull String s1) {
//                                    if(null != FaceSearchFeatureManger.getInstance(getApplicationContext()).queryFaceFeatureByID("周上")){
//                                        FaceSearchFeatureManger.getInstance(getApplicationContext()).deleteFaceFaceFeature(s);
//                                        Toast.makeText(getApplicationContext(), "ID已存在", Toast.LENGTH_SHORT).show();
//                                    }
//                                    FaceSearchFeatureManger.getInstance(getApplicationContext()).insertFaceFeature("周上", s1, System.currentTimeMillis(), "tag","group");
//                                    FaceAISDKEngine.getInstance(getApplicationContext()).saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR,s);
//                                    mUploadInfo.setText("上传照片:" + file.getName() + " 人像已录入!");
//                                }
//
//                                @Override
//                                public void onFailed(@NonNull String s) {
//                                    mUploadInfo.setText("上传照片:" + file.getName() + " 失败！照片不符合要求 ！");
//                                }
//                            });
//                        } else {
//                            mProgressBar.setProgress(0);
//                            mUploadInfo.setText("Uploading file " + file.getName() + "...");
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onDownloadingFile(final File file, final boolean done) {
//                MainActivity.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (done) {
//                            mDownloadInfo.setText("Download file " + file.getName() + " done!") ;
//                        } else {
//                            mDownloadInfo.setText("Downloading file " + file.getName() + " ...");
//                        }
//                    }
//                });
//            }
//        });
//
//        try {
//            mHttpd.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        mHttpd.stop();
//        super.onDestroy();
//    }
//
//    public String getWifiIpAddress() {
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//        WifiInfo info = wifiManager.getConnectionInfo();
//        return android.text.format.Formatter.formatIpAddress(info.getIpAddress());
//    }
//
//    public Bitmap getBitmapFromFileSafe(File file, int reqWidth, int reqHeight) {
//        if (file == null || !file.exists()) return null;
//
//        // 第一步：获取图片尺寸（不解码像素）
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
//
//        // 第二步：计算 inSampleSize（采样率）
//        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//
//        // 第三步：真正解码
//        options.inJustDecodeBounds = false;
//        options.inPreferredConfig = Bitmap.Config.RGB_565; // 节省内存（无透明通道）
//        options.inDither = false;
//
//        try {
//            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
//        final int height = options.outHeight;
//        final int width = options.outWidth;
//        int inSampleSize = 1;
//
//        if (height > reqHeight || width > reqWidth) {
//            final int halfHeight = height / 2;
//            final int halfWidth = width / 2;
//
//            while ((halfHeight / inSampleSize) >= reqHeight
//                    && (halfWidth / inSampleSize) >= reqWidth) {
//                inSampleSize *= 2;
//            }
//        }
//        return inSampleSize;
//    }
//}
