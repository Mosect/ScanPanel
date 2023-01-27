# ScanPanel
Android扫码面板视图，使用ScanPanel View即可完成扫码工作。

# 导入到项目

## 导入ScanPanel库
[![](https://jitpack.io/v/Mosect/ScanPanel.svg)](https://jitpack.io/#Mosect/ScanPanel) <<< 点击查看

## 导入解码库
推荐zxing：
```
dependencies {
    implementation 'com.google.zxing:core:3.5.0'
}
```
将demo实现的解码类复制进项目中：

com.mosect.app.scanpanel.ZxingDecoder

# 使用教程

## 1. 添加ScanPanel视图

### XML方式
```
<com.mosect.lib.scanpanel.ScanPanel
    android:id="@+id/sp_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:scanAutostart="true"
    app:scanDecoder="com.mosect.app.scanpanel.ZxingDecoder"
    app:scanUseTextureView="false" />
```

### JAVA方式
```
ScanPnael scanPanel = new ScanPanel(this);
// 设置解码器，必须，可以使用Demo中zxing编码器
// com.mosect.app.scanpanel.ZxingDecoder
scanPanel.setFrameDecoder("com.mosect.app.scanpanel.ZxingDecoder");
// 是否自动开始
scanPanel.setAutostart(true);
// 是否使用TextureView渲染
scanPanel.setUseTextureView(false);
setContentView(scanPanel);
```

## 2. 设置回调
```
scanPanel.setCallback(new ScanPanel.Callback() {
    @Override
    public void onScanStart(ScanPanel panel) {
        // 扫码开始
    }

    @Override
    public void onDrawMask(ScanPanel panel, Canvas canvas, int width, int height, Rect clip) {
        // 绘制遮罩层
    }

    @Override
    public int onSwitchCamera(ScanPanel panel) {
        // 选择相机，返回相机id
        return -1; // 返回负数，使用ScanPanel内部选定的相机
    }

    @Override
    public void onScanError(ScanPanel panel, Exception exp) {
        // 扫码错误
    }

    @Override
    public void onScanResult(ScanPanel panel, String result) {
        // 扫码结果
    }

    @Override
    public boolean onComputeClip(ScanPanel panel, int width, int height, Rect out) {
        // 计算裁剪区域
        return false; // true，使用计算的裁剪区域；false，使用ScanPanel内部裁剪区域
    }

    @Override
    public void onScanEnd(ScanPanel panel) {
        // 扫码结束
    }
});
```

## 3. 开始与销毁
未开启自动模式下

scanPanel.setAutostart(false)

app:scanAutostart="false"

应该自行处理开始与销毁：

### 获得相机权限后执行开始
```
scanPanel.start();
```

### activity销毁或者不再使用
```
scanPanel.destroy();
```

## 4. 设置显示方向

在开始之前或者屏幕方向发生变化时，需要设置屏幕方向：
```
int rotation = getWindowManager().getDefaultDisplay().getRotation();
scanPanel.setDisplayRotation(rotation);
```

## 5. 扫码结果处理

在onScanResult回调中，需要处理扫码结果：
```
    @Override
    public void onScanResult(ScanPanel panel, String result) {
        // 扫码结果
        // 如果是无法识别的结果，需要手动调用next方法，请求解析下一帧
        // panel.next();
    }
```

## 6. 绘制遮罩层

如果需要显示一些遮罩效果，需要在onDrawMask回调实现：
```
    // MaskBackground实现了简单的遮罩效果
    private MaskBackground maskBackground = new MaskBackground();
    
    @Override
    public void onDrawMask(ScanPanel panel, Canvas canvas, int width, int height, Rect clip) {
        // 绘制遮罩层
        maskBackground.draw(canvas, clip);
    }
```
如果需要刷新遮罩层，需要调用：
```
scanPanel.invalidateMask();
```

## 7. 关于帧解码
ScanPanel不包含帧解码代码，需要导入第三方解码库，比如zxing，让后编写FrameDecoder类，并在ScanPanel中指定解码库。

Demo中包含了已实现的FrameDecoder：

com.mosect.app.scanpanel.ZxingDecoder

可以直接复制进项目使用。

