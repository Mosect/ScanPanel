package com.mosect.app.scanpanel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;

public class TextMask {

    private final Paint paint;
    private final String text = "Hello world!";
    private final float textWidth;
    private final float textHeight;
    private final float yOffset;

    public TextMask(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18,
                context.getResources().getDisplayMetrics());
        paint.setTextSize(textSize);
        textWidth = paint.measureText(text);
        Paint.FontMetrics fm = paint.getFontMetrics();
        textHeight = fm.bottom - fm.top;
        yOffset = -fm.top;
    }

    public void draw(Canvas canvas, int width, int height) {
        float y = (height - textHeight) / 2 + yOffset;
        float x = (width - textWidth) / 2;
        canvas.drawText(text, x, y, paint);
    }
}
