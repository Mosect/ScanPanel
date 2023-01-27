package com.mosect.app.scanpanel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.mosect.lib.scanpanel.coder.FrameDecoder;

import java.io.ByteArrayOutputStream;

public class ZxingDecoder implements FrameDecoder {

    private final MultiFormatReader multiFormatReader;

    public ZxingDecoder() {
        multiFormatReader = new MultiFormatReader();
    }

    @Override
    public String decodeFrame(int format, byte[] data, int width, int height, Rect clip) throws Exception {
//        YuvImage image = new YuvImage(data, format, width, height, new int[]{width, width});
//        ByteArrayOutputStream temp = new ByteArrayOutputStream(512);
//        image.compressToJpeg(clip, 100, temp);
//        byte[] jpeg = temp.toByteArray();
//        Bitmap bm = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
//        bm.recycle();
        int cl = 0, ct = 0, cw = width, ch = height;
        if (null != clip) {
            cl = clip.left;
            ct = clip.top;
            cw = clip.width();
            ch = clip.height();
        }
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, cl, ct, cw, ch, false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result rawResult = multiFormatReader.decode(bitmap);
            return rawResult.getText();
        } finally {
            multiFormatReader.reset();
        }
    }
}
