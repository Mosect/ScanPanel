package com.mosect.app.scanpanel;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class ManualActivity extends ScanActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResultLauncher<String> perLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), result -> {
                    if (result) {
                        // 获得权限
                        spMain.start();
                    }
                });
        int status = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (status == PackageManager.PERMISSION_GRANTED) {
            // 已获得权限
            spMain.start();
        } else {
            perLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onInitContentView() {
        setContentView(R.layout.activity_manual);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        spMain.destroy();
    }
}
