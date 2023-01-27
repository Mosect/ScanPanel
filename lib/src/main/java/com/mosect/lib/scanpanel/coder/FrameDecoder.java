package com.mosect.lib.scanpanel.coder;

import android.graphics.Rect;

/**
 * 帧解码器
 */
public interface FrameDecoder {

    /**
     * 解码帧
     *
     * @param format 格式，目前只会是{@link android.graphics.ImageFormat#NV21}
     * @param data   数据
     * @param width  宽
     * @param height 高
     * @param clip   裁剪区域
     * @return 解码后的字符串，返回null，表示无内容
     * @throws Exception 解码异常
     */
    String decodeFrame(int format, byte[] data, int width, int height, Rect clip) throws Exception;
}
