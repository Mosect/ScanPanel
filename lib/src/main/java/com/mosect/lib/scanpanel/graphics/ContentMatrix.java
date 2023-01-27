package com.mosect.lib.scanpanel.graphics;

import android.graphics.RectF;

/**
 * 内容矩阵，无特别说明，使用的是左下角为原点的坐标系
 */
public class ContentMatrix {

    private final RectF contentRect;
    private final RectF viewportRect;
    private final Matrix3D matrix = new Matrix3D();
    private final Matrix3D matrix2 = new Matrix3D();
    private final float[] contentPoints;
    private final float[] viewportPoints;
    private ScaleType scaleType;
    private int degrees;
    private boolean flipX;

    public ContentMatrix(RectF contentRect, RectF viewportRect) {
        this.contentRect = contentRect;
        this.viewportRect = viewportRect;
        contentPoints = getVec4Points(contentRect);
        viewportPoints = getVec4Points(viewportRect);

        update(ScaleType.CENTER_CROP, 0, false);
    }

    public void update(ScaleType scaleType, int degrees, boolean flipX) {
        // 初始化矩阵
        matrix.reset();
        // 旋转
        matrix.postRotate(degrees, 0, 0, -1f);

        float[] temp = new float[contentPoints.length];

        // 计算缩放
        matrix.mapVec4Points(contentPoints, temp);
        float contentWidth = getRectPointsWidth(temp);
        float contentHeight = getRectPointsHeight(temp);
        float viewportWidth = getRectPointsWidth(viewportPoints);
        float viewportHeight = getRectPointsHeight(viewportPoints);
        float scaleX = viewportWidth / contentWidth;
        float scaleY = viewportHeight / contentHeight;
        float contentScale = contentWidth / contentHeight;
        float viewportScale = viewportWidth / viewportHeight;
        float finalScaleX, finalScaleY;
        switch (scaleType) {
            case CENTER_INSIDE:
                if (contentScale > viewportScale) {
                    finalScaleX = scaleX;
                    finalScaleY = scaleX;
                } else {
                    finalScaleX = scaleY;
                    finalScaleY = scaleY;
                }
                break;
            case CENTER_CROP:
                if (contentScale > viewportScale) {
                    finalScaleX = scaleY;
                    finalScaleY = scaleY;
                } else {
                    finalScaleX = scaleX;
                    finalScaleY = scaleX;
                }
                break;
            case FIT_XY:
                finalScaleX = scaleX;
                finalScaleY = scaleY;
                break;
            default:
                throw new IllegalArgumentException("Unsupported scaleType: " + scaleType);
        }
        if (flipX) finalScaleX = -finalScaleX;
        // 缩放
        matrix.postScale(finalScaleX, finalScaleY, 1f);

        // 居中处理
        matrix.mapVec4Points(contentPoints, temp);
        float ccx = getRectPointsCenterX(temp);
        float ccy = getRectPointsCenterY(temp);
        float vcx = getRectPointsCenterX(viewportPoints);
        float vcy = getRectPointsCenterY(viewportPoints);
        float ox = vcx - ccx;
        float oy = vcy - ccy;
        // 偏移
        matrix.postTranslate(ox, oy, 0);

        // 计算反转后的矩阵
        matrix.invert(matrix2);

        this.scaleType = scaleType;
        this.degrees = degrees;
        this.flipX = flipX;
    }

    /**
     * 转换点
     *
     * @param points 点列表：x1,y1,z1,x2,y2,z2 ... xN,yN,zN
     * @param out    输出
     */
    public void mapVec3Points(float[] points, float[] out) {
        matrix.mapPoints(points, out);
    }

    /**
     * viewport上的矩阵转换成content上的矩形，注意：此方法使用左上角为原点的坐标系
     *
     * @param src 矩形
     */
    public void viewportToContent2D(RectF src, RectF dst) {
        float[] points = getVec4Points(src);
        float vh = getRectPointsHeight(viewportPoints);
        // 坐标系转换
        points[1] = vh - points[1];
        points[5] = vh - points[5];
        float[] dstPoints = new float[points.length];
        matrix2.mapVec4Points(points, dstPoints);
        // 转换会原本坐标系
        float ch = getRectPointsHeight(contentPoints);
        dstPoints[1] = ch - dstPoints[1];
        dstPoints[5] = ch - dstPoints[5];
        // 转换成RectF对象
        pointsToRect(dstPoints, dst);
    }

    public ScaleType getScaleType() {
        return scaleType;
    }

    public int getDegrees() {
        return degrees;
    }

    public boolean isFlipX() {
        return flipX;
    }

    public RectF getContentRect() {
        return contentRect;
    }

    public RectF getViewportRect() {
        return viewportRect;
    }

    private static float[] getVec4Points(RectF rect) {
        return new float[]{
                rect.left, rect.bottom, 0, 1,
                rect.right, rect.top, 0, 1,
        };
    }

    private static float getRectPointsWidth(float[] points) {
        return Math.abs(points[0] - points[4]);
    }

    private static float getRectPointsHeight(float[] points) {
        return Math.abs(points[1] - points[5]);
    }

    private static float getRectPointsCenterX(float[] points) {
        return (points[0] + points[4]) / 2f;
    }

    private static float getRectPointsCenterY(float[] points) {
        return (points[1] + points[5]) / 2f;
    }

    private static void pointsToRect(float[] points, RectF out) {
        if (points[0] > points[4]) {
            out.left = points[4];
            out.right = points[0];
        } else {
            out.left = points[0];
            out.right = points[4];
        }
        if (points[1] > points[5]) {
            out.top = points[5];
            out.bottom = points[1];
        } else {
            out.top = points[1];
            out.bottom = points[5];
        }
    }

    public enum ScaleType {
        CENTER_INSIDE,
        CENTER_CROP,
        FIT_XY,
    }
}
