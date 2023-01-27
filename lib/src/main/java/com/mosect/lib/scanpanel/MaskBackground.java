package com.mosect.lib.scanpanel;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

/**
 * 遮罩层背景，可以在{@link ScanHandler.Callback#onDrawMask(Canvas, int, int, Rect)}中执行{@link #draw(Canvas, Rect)}，实现遮罩效果
 */
public class MaskBackground {

    private final Paint paint;
    private int color = Color.parseColor("#a0000000");

    public MaskBackground() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void draw(Canvas canvas, Rect clip) {
        if (null != clip) {
            canvas.drawColor(color, PorterDuff.Mode.DST_OVER);
            canvas.drawRect(clip, paint);
        }
    }
}
