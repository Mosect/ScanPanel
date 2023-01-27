package com.mosect.lib.scanpanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.mosect.lib.scanpanel.coder.FrameDecoder;

import java.lang.reflect.Constructor;

/**
 * 扫码面板
 * <p>
 * &lt;com.mosect.lib.scanpanel.ScanPanel
 * android:id="@+id/sp_main"
 * android:layout_width="match_parent"
 * android:layout_height="match_parent"
 * app:scanAutostart="true"
 * app:scanDecoder="你的解码器类路径"
 * app:scanUseTextureView="false" /&rt;
 * <p>
 * scanAutostart：是否自动开始，默认false；true，试图依附到父视图中就开始处理扫码；false，需要手动调用{@link #start()}和{@link #destroy()}
 * <p>
 * scanDecoder：帧解码类路径，支持无参构造方法和带有一个{@link Context}参数的构造方法
 * <p>
 * scanUseTextureView：是否使用TextureView显示，默认false，使用SurfaceView；
 */
public class ScanPanel extends ViewGroup {

    private Callback callback;
    private boolean autostart = false; // 自动开始，即依附到父视图中就开始处理扫码
    private boolean useTextureView = false; // 是否使用TextureView
    private int displayRotation = 0; // 显示方向
    private FrameDecoder frameDecoder = null; // 帧解码器

    private ScanHandler scanHandler; // 扫码处理对象
    private final Rect clipRect = new Rect(); // 裁剪区域

    private final Rect clipRect2 = new Rect();

    private View renderView;
    private Surface surface;
    private int surfaceWidth;
    private int surfaceHeight;

    public ScanPanel(Context context) {
        super(context);
        init(context, null);
    }

    public ScanPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ScanPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScanPanel);
        autostart = ta.getBoolean(R.styleable.ScanPanel_scanAutostart, false);
        useTextureView = ta.getBoolean(R.styleable.ScanPanel_scanUseTextureView, false);
        String decoderText = ta.getString(R.styleable.ScanPanel_scanDecoder);
        // 加载设定的帧解码器
        if (!TextUtils.isEmpty(decoderText)) {
            try {
                Class<?> cls = Class.forName(decoderText);
                try {
                    Constructor<?> def = cls.getConstructor();
                    frameDecoder = (FrameDecoder) def.newInstance();
                } catch (NoSuchMethodException e) {
                    Constructor<?> c = cls.getConstructor(Context.class);
                    frameDecoder = (FrameDecoder) c.newInstance(context);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        ta.recycle();
        if (useTextureView) {
            initTextureView();
        } else {
            initSurfaceView();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (autostart) {
            start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (autostart) {
            destroy();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
        setMeasuredDimension(width, height);

        boolean callbackClip = false;
        clipRect2.setEmpty();
        if (null != callback) {
            callbackClip = callback.onComputeClip(this, width, height, clipRect2);
        }
        if (!callbackClip) {
            onComputeClip(width, height, clipRect2);
        }
        clipRect.set(clipRect2);
        if (null != scanHandler) {
            scanHandler.setClip(clipRect);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }

    /**
     * 开始处理扫码
     */
    public void start() {
        if (null == scanHandler) {
            scanHandler = new ScanHandler(getContext());
            scanHandler.setSurface(surface, surfaceWidth, surfaceHeight);
            scanHandler.setClip(clipRect);
            scanHandler.setDisplayRotation(displayRotation);
            scanHandler.setFrameDecoder(frameDecoder);
            scanHandler.setCallback(new ScanHandler.Callback() {
                @Override
                public void onScanStart(ScanHandler handler) {
                    post(() -> {
                        if (null != callback) {
                            callback.onScanStart(ScanPanel.this);
                        }
                    });
                }

                @Override
                public int onSwitchCamera(ScanHandler handler) {
                    Callback callback = ScanPanel.this.callback;
                    if (null != callback) {
                        int id = callback.onSwitchCamera(ScanPanel.this);
                        if (id >= 0) return id;
                    }
                    return ScanPanel.this.onSwitchCamera();
                }

                @Override
                public void onDrawMask(ScanHandler handler, Canvas canvas, int width, int height, Rect clip) {
                    Callback callback = ScanPanel.this.callback;
                    if (null != callback) {
                        callback.onDrawMask(ScanPanel.this, canvas, width, height, clip);
                    }
                }

                @Override
                public void onScanError(ScanHandler handler, Exception exp) {
                    post(() -> {
                        if (null != callback) {
                            callback.onScanError(ScanPanel.this, exp);
                        }
                    });
                }

                @Override
                public void onScanResult(ScanHandler handler, String result) {
                    post(() -> {
                        if (null != callback) {
                            callback.onScanResult(ScanPanel.this, result);
                        }
                    });
                }

                @Override
                public void onScanEnd(ScanHandler handler) {
                    post(() -> {
                        if (null != callback) {
                            callback.onScanEnd(ScanPanel.this);
                        }
                    });
                }
            });
            scanHandler.start();
        }
    }

    /**
     * 继续解码下一帧
     */
    public void next() {
        if (null != scanHandler) {
            scanHandler.next();
        }
    }

    /**
     * 刷新遮罩层
     */
    public void invalidateMask() {
        if (null != scanHandler) {
            scanHandler.invalidateMask();
        }
    }

    /**
     * 销毁扫码处理对象
     */
    public void destroy() {
        if (null != scanHandler) {
            scanHandler.destroy();
            scanHandler = null;
        }
    }

    /**
     * 设置显示方法，{@link ScanHandler#setDisplayRotation(int)}
     *
     * @param rotation 方向
     */
    public void setDisplayRotation(int rotation) {
        if (this.displayRotation != rotation) {
            this.displayRotation = rotation;
            if (null != scanHandler) {
                scanHandler.setDisplayRotation(rotation);
            }
        }
    }

    /**
     * 设置帧解码器，{@link ScanHandler#setFrameDecoder(FrameDecoder)}
     *
     * @param decoder 解码器
     */
    public void setFrameDecoder(FrameDecoder decoder) {
        frameDecoder = decoder;
        if (null != scanHandler) {
            scanHandler.setFrameDecoder(frameDecoder);
        }
    }

    /**
     * 计算裁剪区域，如果{@link Callback#onComputeClip(ScanPanel, int, int, Rect)}返回false，则触发此方法
     *
     * @param width  宽
     * @param height 高
     * @param out    输出的裁剪区域
     */
    protected void onComputeClip(int width, int height, Rect out) {
        out.set(0, 0, width, height);
    }

    private void initSurfaceView() {
        SurfaceView surfaceView = new SurfaceView(getContext());
        renderView = surfaceView;
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (renderView == surfaceView) {
                    setSurface(holder.getSurface(), width, height);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (renderView == surfaceView) {
                    setSurface(null, 0, 0);
                }
            }
        });
        addView(surfaceView);
    }

    private void initTextureView() {
        TextureView textureView = new TextureView(getContext());
        renderView = textureView;
        textureView.setLayerType(LAYER_TYPE_HARDWARE, null);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            private Surface surface;

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (renderView == textureView) {
                    this.surface = new Surface(surface);
                    setSurface(this.surface, width, height);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                if (null != this.surface) {
                    this.surface.release();
                    this.surface = null;
                }
                if (renderView == textureView) {
                    this.surface = new Surface(surface);
                    setSurface(this.surface, width, height);
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (null != this.surface) {
                    this.surface.release();
                    this.surface = null;
                }
                if (renderView == textureView) {
                    setSurface(null, 0, 0);
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        addView(textureView);
    }

    /**
     * 选择摄像头，{@link Callback#onSwitchCamera(ScanPanel)}
     *
     * @return 摄像头id
     */
    protected int onSwitchCamera() {
        int count = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 设置是否自动开始，非自动模式大多用于处理摄像头权限；自动模式适用于已经确保获取摄像头权限的场景
     *
     * @param autostart true，自动开始；false，不自动开始，需要手动处理{@link #start()}和{@link #destroy()}
     */
    public void setAutostart(boolean autostart) {
        if (isAttachedToWindow()) {
            throw new IllegalStateException("Unsupported changed autostart on attached state");
        }
        this.autostart = autostart;
    }

    /**
     * 判断是否自动开始
     *
     * @return true，自动开始；false，不自动开始
     */
    public boolean isAutostart() {
        return autostart;
    }

    /**
     * 设置回调
     *
     * @param callback 回调
     */
    public void setCallback(Callback callback) {
        if (this.callback != callback) {
            this.callback = callback;
            requestLayout();
        }
    }

    /**
     * 判断是否使用TextureView
     *
     * @return true，使用TextureView；false，不使用TextureView，使用SurfaceView
     */
    public boolean isUseTextureView() {
        return useTextureView;
    }

    /**
     * 设置是否使用TextureView
     *
     * @param useTextureView true，使用TextureView；false，不使用TextureView，使用SurfaceView
     */
    public void setUseTextureView(boolean useTextureView) {
        if (this.useTextureView != useTextureView) {
            this.useTextureView = useTextureView;
            if (null != renderView) {
                removeView(renderView);
                renderView = null;
            }
            setSurface(null, 0, 0);
            if (useTextureView) {
                initTextureView();
            } else {
                initSurfaceView();
            }
        }
    }

    private void setSurface(Surface surface, int surfaceWidth, int surfaceHeight) {
        this.surface = surface;
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        if (null != scanHandler) {
            scanHandler.setSurface(surface, surfaceWidth, surfaceHeight);
        }
    }

    /**
     * 回调
     */
    public interface Callback {

        /**
         * 扫码开始
         *
         * @param panel 扫码面板
         */
        void onScanStart(ScanPanel panel);

        /**
         * 绘制遮罩层
         *
         * @param panel  扫码面板
         * @param canvas 画布
         * @param width  宽
         * @param height 高
         * @param clip   裁剪区域，即扫码区域
         */
        void onDrawMask(ScanPanel panel, Canvas canvas, int width, int height, Rect clip);

        /**
         * 选择摄像头，返回负数表示使用内部{@link #onSwitchCamera()}方法获取
         *
         * @param panel 扫码面板
         * @return 摄像头id
         */
        int onSwitchCamera(ScanPanel panel);

        /**
         * 扫码错误
         *
         * @param panel 扫码面板
         * @param exp   异常
         */
        void onScanError(ScanPanel panel, Exception exp);

        /**
         * 扫码成功
         *
         * @param panel  扫码面板
         * @param result 扫码后的文本
         */
        void onScanResult(ScanPanel panel, String result);

        /**
         * 计算裁剪区域，即扫码区域
         *
         * @param panel  扫码面板
         * @param width  宽
         * @param height 高
         * @param out    输出的裁剪区域
         * @return true，计算成功；false，使用内部{@link #onComputeClip(int, int, Rect)}方法提供的区域
         */
        boolean onComputeClip(ScanPanel panel, int width, int height, Rect out);

        /**
         * 扫码结束
         *
         * @param panel 扫码面板
         */
        void onScanEnd(ScanPanel panel);
    }
}
