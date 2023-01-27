package com.mosect.lib.scanpanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import com.mosect.lib.easygl2.GLBitmapProvider;
import com.mosect.lib.easygl2.GLContext;
import com.mosect.lib.easygl2.GLException;
import com.mosect.lib.easygl2.GLSurfaceWindow;
import com.mosect.lib.easygl2.GLTextureWindow;
import com.mosect.lib.scanpanel.coder.FrameDecoder;
import com.mosect.lib.scanpanel.coder.FrameHandler;
import com.mosect.lib.scanpanel.graphics.BitmapTexture;
import com.mosect.lib.scanpanel.graphics.ContentMatrix;
import com.mosect.lib.scanpanel.graphics.DrawerOES;
import com.mosect.lib.scanpanel.graphics.DrawerTEX;
import com.mosect.lib.scanpanel.shader.ShaderOES;
import com.mosect.lib.scanpanel.shader.ShaderTEX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 扫码处理，不推荐使用此类进行扫码处理，推荐使用{@link ScanPanel}
 */
public class ScanHandler {

    private static final String TAG = "ScanHandler";

    private final Context context;
    private Callback callback;
    private int state = 0; // 状态：0，未开始；1，运行中；2，已销毁
    private int displayRotation = 0; // 屏幕显示方向

    private final byte[] lock = new byte[0]; // 锁
    private final LinkedList<Runnable> actions = new LinkedList<>(); // 在loop中执行的action

    private Camera camera = null; // 摄像头
    private final Camera.CameraInfo cameraInfo = new Camera.CameraInfo(); // 摄像头信息

    private GLContext glContext; // GL上下文
    private GLTextureWindow glTextureWindow; // 纹理窗口，用来接收摄像头预览图像
    private GLSurfaceWindow glSurfaceWindow; // Surface窗口，用来显示摄像头预览图像以及遮罩层
    private ShaderOES shaderOES; // OES shader
    private ShaderTEX shaderTEX; // 纹理shader
    private DrawerOES cameraDrawer; // 摄像头预览图像绘制器
    private ContentMatrix cameraMatrix; // 摄像头预览图像变换矩阵

    private Rect clipRect; // 裁剪部分，即扫码部分
    private FrameDecoder frameDecoder; // 帧解码器
    private FrameHandler frameHandler; // 帧解码处理

    private DrawerTEX maskDrawer; // 遮罩层绘制器
    private Bitmap maskBitmap; // 遮罩层位图
    private Canvas maskCanvas; // 遮罩层画布
    private BitmapTexture maskTexture; // 遮罩层纹理
    private boolean maskChanged; // 遮罩层是否发生更改，如果发生更改，则会更新maskTexture
    private long nextFocusTime = -1; // 下次对焦的时间，负数，表示对焦中

    public ScanHandler(Context context) {
        this.context = context;
    }

    /**
     * 开始处理扫码
     */
    public void start() {
        synchronized (lock) {
            if (state == 0) {
                state = 1;
                new Thread(this::loop).start();
            }
        }
    }

    /**
     * 设置surface，显示摄像头预览图像和遮罩层
     *
     * @param surface surface
     * @param width   宽
     * @param height  高
     */
    public void setSurface(Surface surface, int width, int height) {
        runAction(() -> {
            clearSurface();
            if (null != surface && width > 0 && height > 0) {
                glSurfaceWindow = new GLSurfaceWindow(surface, width, height);
                glSurfaceWindow.init(glContext);
                // 创建遮罩层
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                maskBitmap = bitmap;
                maskCanvas = new Canvas(maskBitmap);
                maskTexture = new BitmapTexture(new GLBitmapProvider() {
                    @Override
                    public Bitmap getBitmap(Object host) {
                        return bitmap;
                    }

                    @Override
                    public void destroyBitmap(Object host, Bitmap bitmap) {
                        bitmap.recycle();
                    }
                });
                maskTexture.init(glContext);
                maskChanged = true;
                RectF contentRect = new RectF(0, 0, width, height);
                RectF viewportRect = new RectF(0, 0, width, height);
                ContentMatrix maskMatrix = new ContentMatrix(contentRect, viewportRect);
                maskMatrix.update(ContentMatrix.ScaleType.CENTER_CROP, 0, false);
                maskDrawer = new DrawerTEX(maskMatrix, 0.9f);
                if (null == camera) {
                    // 打开相机
                    camera = openCamera(cameraInfo);
                    if (null != camera) {
                        // 初始化相机
                        initCamera();
                    }
                }
            } else {
                // 关闭相机
                closeCamera();
            }
        });
    }

    /**
     * 设置显示方向，可以从{@link Display#getRotation()}获取<br>
     * activity.getWindowManager().getDefaultDisplay().getRotation()
     *
     * @param displayRotation 显示方向
     */
    public void setDisplayRotation(int displayRotation) {
        runAction(() -> {
            this.displayRotation = displayRotation;
            if (null != camera && null != glSurfaceWindow) {
                initCamera();
            }
        });
    }

    /**
     * 设置裁剪区域，即扫码部分
     *
     * @param rect 区域
     */
    public void setClip(Rect rect) {
        runAction(() -> {
            if (null == rect) {
                clipRect = null;
            } else {
                clipRect = new Rect(rect);
            }
            // 同步到FrameHandler
            if (null != frameHandler) {
                frameHandler.setClip(convertClipRect());
            }
            maskChanged = true;
        });
    }

    /**
     * 设置帧解码器
     *
     * @param frameDecoder 帧解码器
     */
    public void setFrameDecoder(FrameDecoder frameDecoder) {
        runAction(() -> {
            this.frameDecoder = frameDecoder;
            // 同步到FrameHandler
            if (null != frameHandler) {
                frameHandler.setDecoder(frameDecoder);
            }
        });
    }

    /**
     * 继续解码下一帧；注意：在{@link Callback#onScanResult(ScanHandler, String)}触发后，将停止解码，需要再次调用此方法才能继续解码
     */
    public void next() {
        runAction(() -> {
            if (null != frameHandler) {
                frameHandler.requestNextFrame();
            }
        });
    }

    /**
     * 销毁并释放此对象，此对象将不可用
     */
    public void destroy() {
        synchronized (lock) {
            if (state != 2) {
                state = 2;
            }
        }
    }

    /**
     * 如果需要刷新遮罩层图像，需要调用此方法
     */
    public void invalidateMask() {
        runAction(() -> {
            if (!maskChanged) {
                maskChanged = true;
            }
        });
    }

    /**
     * 扫码处理核心循环
     */
    private void loop() {
        Log.d(TAG, "loop: start");
        onStart();
        try {
            // 创建opengl环境
            glContext = new GLContext(64, 64);
            glContext.init();
            glContext.makeCurrentWithException();

            // 创建shader
            shaderOES = new ShaderOES(context);
            shaderOES.init(glContext);
            shaderTEX = new ShaderTEX(context);
            shaderTEX.init(glContext);

            // 进入循环
            while (true) {
                // 切换成默认输出surface，必要，否则GLContext不可用
                glContext.makeCurrentWithException();
                // 执行action
                synchronized (lock) {
                    if (state != 1) break;
                    while (actions.size() > 0) {
                        actions.removeFirst().run();
                    }
                }

                if (null != glSurfaceWindow) {
                    // 存在surface，则需要进行绘制
                    if (glSurfaceWindow.makeCurrent()) {
                        // 图像输出成功切换成surface

                        // 渲染背景
                        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        GLException.checkGLError("glClear");

                        // 绘制摄像头图像
                        if (null != cameraDrawer) {
                            cameraDrawer.draw(glTextureWindow, shaderOES);
                        }

                        // 绘制遮罩层
                        if (maskChanged) {
                            maskCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.DST_OVER);
                            onDrawMask(maskCanvas, glSurfaceWindow.getWidth(), glSurfaceWindow.getHeight(), clipRect);
                            // 将位图内容同步到纹理中
                            maskTexture.updateTextureImage();
                            maskChanged = false;
                        }
                        maskDrawer.draw(maskTexture, shaderTEX);

                        // 提交最终图像
                        boolean ok = glSurfaceWindow.commit();
                        if (!ok) {
                            Log.w(TAG, "loop: commitFailed");
                        }
                    } else {
                        Log.w(TAG, "loop: makeCurrentFailed");
                    }
                }

                // 处理对焦
                long now = System.currentTimeMillis();
                if (null != camera) {
                    if (nextFocusTime >= 0 && now >= nextFocusTime) {
                        // 符合对焦时间
                        nextFocusTime = -1; // 需要设置成对焦中
                        camera.autoFocus((success, camera1) -> {
                            // 对焦完成
                            runAction(() -> {
                                if (nextFocusTime < 0) {
                                    // 还在对焦中，处理此处对焦结果
                                    if (success) {
                                        // 自动对焦成功，4秒后重新对焦
                                        nextFocusTime = System.currentTimeMillis() + 4000;
                                    } else {
                                        // 自动对焦失败，下一次马上对焦
                                        nextFocusTime = System.currentTimeMillis();
                                    }
                                }
                            });
                        });
                    }
                }
            }
        } catch (Exception e) {
            onError(e);
        } finally {
            // 释放必要的资源
            closeCamera();
            clearSurface();
            if (null != shaderOES) {
                shaderOES.close();
                shaderOES = null;
            }
            if (null != shaderTEX) {
                shaderTEX.close();
                shaderTEX = null;
            }
            if (null != glContext) {
                glContext.close();
                glContext = null;
            }
        }
        Log.d(TAG, "loop: end");
        onEnd();
    }

    /**
     * 打开摄像头
     *
     * @param cameraInfo 输出摄像头信息
     * @return 摄像头对象，返回null，表示打开摄像头失败
     */
    protected Camera openCamera(Camera.CameraInfo cameraInfo) {
        Callback callback = this.callback;
        int id = -1;
        if (null != callback) {
            // 从callback中获取需要打开的摄像头
            id = callback.onSwitchCamera(this);
        }
        if (id >= 0) {
            Camera camera = Camera.open(id);
            if (null != camera) {
                // 需要获取对应摄像头信息
                Camera.getCameraInfo(id, cameraInfo);
                Log.d(TAG, String.format("openCamera: id=%s, facing=%s, orientation=%s",
                        id, cameraInfo.facing, cameraInfo.orientation));
                return camera;
            }
        }
        return null;
    }

    /**
     * 初始化摄像头
     */
    protected void initCamera() {
        camera.stopPreview();
        if (null != glTextureWindow) {
            glTextureWindow.close();
        }
        int degrees = getCameraDisplayOrientation();
        Camera.Parameters parameters = camera.getParameters();

        // 设置帧率
        int[] fpsRange = switchFpsRange(parameters);
        parameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);

        // 设置格式
        int previewFormat = switchPreviewFormat(parameters);
        parameters.setPreviewFormat(previewFormat);

        // 设置大小
        PreviewSize previewSize = switchPreviewSize(parameters, degrees);
        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        camera.setParameters(parameters);

        // 创建纹理窗口，用于接收摄像头预览图像
        glTextureWindow = new GLTextureWindow(previewSize.getWidth(), previewSize.getHeight(), false);
        glTextureWindow.init(glContext);
        // 创建摄像头绘制器
        RectF contentRect = new RectF(0, 0, glTextureWindow.getWidth(), glTextureWindow.getHeight());
        RectF viewportRect = new RectF(0, 0, glSurfaceWindow.getWidth(), glSurfaceWindow.getHeight());
        cameraMatrix = new ContentMatrix(contentRect, viewportRect);
        cameraMatrix.update(ContentMatrix.ScaleType.CENTER_CROP, degrees, false);
        cameraDrawer = new DrawerOES(cameraMatrix, 0.5f);

        // 创建帧处理
        FrameHandler frameHandler = new FrameHandler();
        this.frameHandler = frameHandler;
        // 同步裁剪和解码器
        frameHandler.setClip(convertClipRect());
        frameHandler.setDecoder(frameDecoder);
        // 监听扫码回调
        frameHandler.setCallback(text -> {
            // 扫码结果
            runAction(() -> {
                if (!TextUtils.isEmpty(text)) {
                    Callback callback = this.callback;
                    if (null != callback) {
                        callback.onScanResult(this, text);
                    }
                } else {
                    // 没有解析到文本，继续解析下一帧
                    frameHandler.requestNextFrame();
                }
            });
        });

        try {
            // 设置预览纹理
            camera.setPreviewTexture(glTextureWindow.getSurfaceTexture());
            // 开始预览
            camera.startPreview();
            // 开始解码
            frameHandler.start(camera);
            frameHandler.requestNextFrame();
            // 需要马上对焦
            nextFocusTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 选择摄像头帧率范围
     *
     * @param parameters 摄像头参数
     * @return 摄像头帧率范围
     */
    protected int[] switchFpsRange(Camera.Parameters parameters) {
        List<int[]> fpsRangeList = parameters.getSupportedPreviewFpsRange();
        Collections.sort(fpsRangeList, (o1, o2) -> {
            if (o1[1] > o2[1]) return -1;
            else if (o1[1] < o2[1]) return 1;
            else if (o1[0] > o2[0]) return -1;
            else if (o1[0] < o2[0]) return 1;
            return 0;
        });
        return fpsRangeList.get(0);
    }

    /**
     * 选择预览格式，默认使用{@link ImageFormat#NV21}
     *
     * @param parameters 摄像头参数
     * @return 预览格式
     */
    protected int switchPreviewFormat(Camera.Parameters parameters) {
        // 默认使用NV21格式
        return ImageFormat.NV21;
    }

    /**
     * 选择预览大小，默认返回最接近surface大小的预览大小
     *
     * @param parameters 摄像头参数
     * @param degrees    角度
     * @return 预览大小
     */
    protected PreviewSize switchPreviewSize(Camera.Parameters parameters, int degrees) {
        List<PreviewSize> previewSizes = new ArrayList<>();
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            previewSizes.add(new PreviewSize(size.width, size.height, degrees));
        }
        Collections.sort(previewSizes, (o1, o2) -> {
            int h1 = Math.abs(o1.getRotateHeight() - glSurfaceWindow.getHeight());
            int h2 = Math.abs(o2.getRotateHeight() - glSurfaceWindow.getHeight());
            if (h1 < h2) return -1;
            else if (h1 > h2) return 1;
            int w1 = Math.abs(o1.getRotateWidth() - glSurfaceWindow.getWidth());
            int w2 = Math.abs(o2.getRotateHeight() - glSurfaceWindow.getHeight());
            if (w1 < w2) return -1;
            else if (w1 > w2) return 1;
            return 0;
        });
        return previewSizes.get(0);
    }

    /**
     * 转换裁剪区域，因为可能会发生旋转，所以不能直接使用设定的裁剪，无特殊需求，不要复写此方法
     *
     * @return 符合预览图像的裁剪
     */
    protected Rect convertClipRect() {
        if (null != clipRect) {
            RectF rect = new RectF(clipRect);
            RectF temp = new RectF();
            // 转换裁剪区域
            cameraMatrix.viewportToContent2D(rect, temp);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 注意：前置摄像头需要翻转水平方向
                float width = cameraMatrix.getContentRect().width();
                float right = width - temp.left;
                float left = width - temp.right;
                temp.left = left;
                temp.right = right;
            }
            Rect result = new Rect();
            result.left = (int) temp.left;
            result.top = (int) temp.top;
            result.right = (int) temp.right;
            result.bottom = (int) temp.bottom;
            return result;
        }
        return null;
    }

    /**
     * 关闭摄像头，只要是释放摄像头相关资源
     */
    protected void closeCamera() {
        if (null != glTextureWindow) {
            glTextureWindow.close();
            glTextureWindow = null;
        }
        if (null != camera) {
            camera.release();
            camera = null;
        }
        if (null != frameHandler) {
            frameHandler.destroy();
            frameHandler = null;
        }
        cameraDrawer = null;
        cameraMatrix = null;
    }

    /**
     * 清空surface，只要是释放surface相关资源
     */
    protected void clearSurface() {
        if (null != glSurfaceWindow) {
            glSurfaceWindow.close();
            glSurfaceWindow = null;
        }
        if (null != maskTexture) {
            maskTexture.close();
            maskTexture = null;
        }
        if (null != maskBitmap) {
            maskBitmap.recycle();
            maskBitmap = null;
        }
        maskCanvas = null;
        maskDrawer = null;
    }

    /**
     * 获取画面旋转的角度，使用了{@link Camera#setDisplayOrientation(int)}推荐算法，无特殊需求，不要复写此方法
     *
     * @return 画面旋转角度
     */
    protected int getCameraDisplayOrientation() {
        int degrees = 0;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 执行一个action，此action不会马上执行，会方法队列最后
     *
     * @param action action
     */
    protected void runAction(Runnable action) {
        synchronized (lock) {
            if (state != 2) {
                actions.addLast(action);
            }
        }
    }

    /**
     * 绘制遮罩层
     *
     * @param canvas 画布
     * @param width  宽度
     * @param height 高度
     * @param clip   裁剪区域
     */
    protected void onDrawMask(Canvas canvas, int width, int height, Rect clip) {
        Callback callback = this.callback;
        if (null != callback) {
            callback.onDrawMask(this, canvas, width, height, clip);
        }
    }

    protected void onStart() {
        Callback callback = this.callback;
        if (null != callback) {
            callback.onScanStart(this);
        }
    }

    /**
     * 发生错误
     *
     * @param exp 异常
     */
    protected void onError(Exception exp) {
        Callback callback = this.callback;
        if (null != callback) {
            callback.onScanError(this, exp);
        }
    }

    protected void onEnd() {
        Callback callback = this.callback;
        if (null != callback) {
            callback.onScanEnd(this);
        }
    }

    /**
     * 设置回调
     *
     * @param callback 回调对象
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * 获取摄像头
     *
     * @return 摄像头
     */
    protected Camera getCamera() {
        return camera;
    }

    /**
     * 获取摄像头信息
     *
     * @return 摄像头信息
     */
    protected Camera.CameraInfo getCameraInfo() {
        return cameraInfo;
    }

    protected Context getContext() {
        return context;
    }

    /**
     * 获取显示方向
     *
     * @return 显示方向
     */
    protected int getDisplayRotation() {
        return displayRotation;
    }

    /**
     * 获取裁剪区域
     *
     * @return 裁剪区域
     */
    protected Rect getClipRect() {
        return clipRect;
    }

    /**
     * 获取裁剪区域
     *
     * @param out 输出的裁剪区域
     */
    public void getClipRect(Rect out) {
        synchronized (lock) {
            if (null != clipRect) {
                out.set(clipRect);
            } else {
                out.setEmpty();
            }
        }
    }

    /**
     * opengl上下文
     *
     * @return opengl上下文
     */
    protected GLContext getGLContext() {
        return glContext;
    }

    /**
     * Surface窗口对象
     *
     * @return surface窗口对象
     */
    protected GLSurfaceWindow getGLSurfaceWindow() {
        return glSurfaceWindow;
    }

    /**
     * 纹理窗口对象，用于接收摄像头图像
     *
     * @return 纹理窗口对象
     */
    protected GLTextureWindow getGLTextureWindow() {
        return glTextureWindow;
    }

    /**
     * 获取回调
     *
     * @return 回调
     */
    public Callback getCallback() {
        return callback;
    }

    /**
     * 回调
     */
    public interface Callback {

        /**
         * 扫码开始
         *
         * @param handler 扫码处理对象
         */
        void onScanStart(ScanHandler handler);

        /**
         * 选择摄像头
         *
         * @param handler 扫码处理对象
         * @return 摄像头id
         */
        int onSwitchCamera(ScanHandler handler);

        /**
         * 绘制遮罩层
         *
         * @param handler 扫码处理对象
         * @param canvas  画布
         * @param width   宽
         * @param height  高
         * @param clip    裁剪区域
         */
        void onDrawMask(ScanHandler handler, Canvas canvas, int width, int height, Rect clip);

        /**
         * 扫码错误
         *
         * @param handler 扫码处理对象
         * @param exp     异常
         */
        void onScanError(ScanHandler handler, Exception exp);

        /**
         * 扫码结果，触发此方法，表示已经扫码成功
         *
         * @param handler 扫码处理对象
         * @param result  结果
         */
        void onScanResult(ScanHandler handler, String result);

        /**
         * 扫码结束
         *
         * @param handler 扫码处理对象
         */
        void onScanEnd(ScanHandler handler);
    }

    public static class PreviewSize {

        private final int width;
        private final int height;
        private final int rotateWidth;
        private final int rotateHeight;

        public PreviewSize(int width, int height, int degrees) {
            this.width = width;
            this.height = height;
            RectF rect = new RectF(0, 0, width, height);
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            matrix.mapRect(rect);
            rotateWidth = (int) rect.width();
            rotateHeight = (int) rect.height();
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getRotateWidth() {
            return rotateWidth;
        }

        public int getRotateHeight() {
            return rotateHeight;
        }
    }
}
