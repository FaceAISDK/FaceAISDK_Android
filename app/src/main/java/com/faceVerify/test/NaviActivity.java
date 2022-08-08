package com.faceVerify.test;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;
import com.AI.FaceVerify.utils.AiUtil;
import java.io.File;
import java.util.List;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 演示导航 Navi
 *
 */
public class NaviActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi);
        checkNeededPermission();

        String cacheMediasDir =
                Environment.getExternalStorageDirectory().toString() + "/cameraX/face/";

        findViewById(R.id.face_verify_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //可以自己录一张人脸底片，业务方可以根据自己的要求改写testBaseImgName 处理
                File file = new File(cacheMediasDir + "testBaseImgName.jpg");
                if (AiUtil.compressPath(NaviActivity.this, Uri.fromFile(file)) != null) {
                    startActivity(new Intent(NaviActivity.this, VerifyActivity.class));
                } else {
                    Toast.makeText(NaviActivity.this, "请先录入人脸底片", Toast.LENGTH_SHORT).show();
                }
            }
        });


        findViewById(R.id.more_about_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://github.com/AnyLifeZLB/FaceVerificationSDK");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(uri);
                startActivity(intent);
            }
        });


        findViewById(R.id.update_base_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NaviActivity.this, UpdateBaseFaceActivity.class)
                        .putExtra("baseImgName", "testBaseImgName"));
            }
        });


    }


    /**
     * 统一全局的拦截权限请求，给提示
     */
    private void checkNeededPermission() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {

        } else {
            EasyPermissions.requestPermissions(this, "", 11, perms);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    /**
     * 当用户点击了不再提醒的时候的处理方式
     */
    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }


}