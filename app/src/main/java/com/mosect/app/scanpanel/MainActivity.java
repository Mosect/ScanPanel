package com.mosect.app.scanpanel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private CheckBox cbFront;
    private CheckBox cbUseTextureView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityResultLauncher<String> cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) {
                        jumpAutostartActivity();
                    }
                });

        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_autostart).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                jumpAutostartActivity();
            } else {
                cameraLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        findViewById(R.id.btn_manual).setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualActivity.class);
            intent.putExtra("facing", cbFront.isChecked() ? "front" : "back");
            intent.putExtra("useTextureView", cbUseTextureView.isChecked());
            startActivity(intent);
        });
        cbFront = findViewById(R.id.cb_front);
        cbUseTextureView = findViewById(R.id.cb_useTextureView);
    }

    private void jumpAutostartActivity() {
        Intent intent = new Intent(this, AutostartActivity.class);
        intent.putExtra("facing", cbFront.isChecked() ? "front" : "back");
        intent.putExtra("useTextureView", cbUseTextureView.isChecked());
        startActivity(intent);
    }
}
