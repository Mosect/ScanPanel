package com.mosect.app.scanpanel;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mosect.lib.scanpanel.MaskBackground;
import com.mosect.lib.scanpanel.ScanPanel;

public abstract class ScanActivity extends AppCompatActivity {

    private static final String TAG = "Act/Autostart";

    protected ScanPanel spMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onInitContentView();

        spMain = findViewById(R.id.sp_main);

        int facing = "front".equals(getIntent().getStringExtra("facing")) ?
                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        boolean useTextureView = getIntent().getBooleanExtra("useTextureView", false);
        spMain.setCallback(new ScanPanel.Callback() {

            private final MaskBackground maskBackground = new MaskBackground();
            private final TextMask textMask = new TextMask(ScanActivity.this);

            @Override
            public void onScanStart(ScanPanel panel) {
                Log.d(TAG, "onScanStart: ");
            }

            @Override
            public void onDrawMask(ScanPanel panel, Canvas canvas, int width, int height, Rect clip) {
                maskBackground.draw(canvas, clip);
                textMask.draw(canvas, width, height);
            }

            @Override
            public int onSwitchCamera(ScanPanel panel) {
                int count = Camera.getNumberOfCameras();
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < count; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (facing == cameraInfo.facing) {
                        return i;
                    }
                }
                return -1;
            }

            @Override
            public void onScanError(ScanPanel panel, Exception exp) {
                Log.e(TAG, "onScanError: ", exp);
            }

            @Override
            public void onScanResult(ScanPanel panel, String result) {
                Log.d(TAG, "onScanResult: " + result);
                panel.next();
            }

            @Override
            public boolean onComputeClip(ScanPanel panel, int width, int height, Rect out) {
                int cw = width / 2;
                int ch;
                if (width > height) {
                    ch = height / 2;
                } else {
                    ch = Math.min(cw, height / 4);
                }
                out.left = (width - cw) / 2;
                out.right = out.left + cw;
                out.top = (height - ch) / 2;
                out.bottom = out.top + ch;
                return true;
            }

            @Override
            public void onScanEnd(ScanPanel panel) {
                Log.d(TAG, "onScanEnd: ");
            }
        });

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        spMain.setDisplayRotation(rotation);
        spMain.setUseTextureView(useTextureView);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        spMain.setDisplayRotation(rotation);
    }

    protected abstract void onInitContentView();
}
