package com.mosect.lib.scanpanel.coder;

import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FrameHandler {

    private static final String TAG = "FrameHandler";

    private int state = 0;
    private final byte[] lock = new byte[0];
    private FrameDecoder decoder;
    private Camera camera;
    private int width;
    private int height;
    private int format;
    private Rect clip;
    private ExecutorService decodeExecutor;
    private Callback callback;

    public void start(Camera camera) {
        synchronized (lock) {
            if (state == 0) {
                state = 1;
                this.camera = camera;
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                width = size.width;
                height = size.height;
                format = parameters.getPreviewFormat();
                decodeExecutor = Executors.newFixedThreadPool(1);
            }
        }
    }

    public void destroy() {
        synchronized (lock) {
            if (state != 2) {
                state = 2;
                decodeExecutor.shutdown();
            }
        }
    }

    public void requestNextFrame() {
        synchronized (lock) {
            if (state == 1) {
                camera.setOneShotPreviewCallback((data, camera1) -> {
                    synchronized (lock) {
                        if (state == 1) {
                            handleFrameData(data);
                        }
                    }
                });
            }
        }
    }

    private void handleFrameData(byte[] data) {
        decodeExecutor.execute(() -> {
            FrameDecoder decoder = this.decoder;
            String text = null;
            if (null != decoder) {
                try {
                    text = decoder.decodeFrame(format, data, width, height, clip);
                } catch (Exception e) {
                    Log.e(TAG, "handleFrameData: ", e);
                }
            }
            handleDecodeResult(text);
        });
    }

    private void handleDecodeResult(String text) {
        Callback callback = null;
        synchronized (lock) {
            if (state == 1) {
                callback = this.callback;
            }
        }
        if (null != callback) {
            callback.onFrameDecodeResult(text);
        }
    }

    public void setDecoder(FrameDecoder decoder) {
        this.decoder = decoder;
    }

    public FrameDecoder getDecoder() {
        return decoder;
    }

    public void setClip(Rect clip) {
        this.clip = clip;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {

        void onFrameDecodeResult(String text);
    }
}
