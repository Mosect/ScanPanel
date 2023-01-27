package com.mosect.lib.scanpanel.graphics;

import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D矩阵计算
 */
public class Matrix3D {

    private final List<float[]> matrixList = new ArrayList<>();

    public void reset() {
        matrixList.clear();
    }

    public void mapPoints(float[] points, float[] out) {
        if (matrixList.isEmpty()) {
            System.arraycopy(points, 0, out, 0, points.length);
            return;
        }

        float[] temp = new float[8];
        for (int i = 0; i < points.length; i += 3) {
            System.arraycopy(points, i, temp, 0, 3);
            temp[3] = 1f;
            for (float[] matrix : matrixList) {
                Matrix.multiplyMV(temp, 4, matrix, 0, temp, 0);
                System.arraycopy(temp, 4, temp, 0, 4);
            }
            System.arraycopy(temp, 4, out, i, 3);
        }
    }

    public void mapVec4Points(float[] points, float[] out) {
        if (matrixList.isEmpty()) {
            System.arraycopy(points, 0, out, 0, points.length);
            return;
        }

        float[] temp = new float[8];
        for (int i = 0; i < points.length; i += 4) {
            System.arraycopy(points, i, temp, 0, 4);
            for (float[] matrix : matrixList) {
                Matrix.multiplyMV(temp, 4, matrix, 0, temp, 0);
                System.arraycopy(temp, 4, temp, 0, 4);
            }
            System.arraycopy(temp, 4, out, i, 4);
        }
    }

    public void postScale(float sx, float sy, float sz) {
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.scaleM(matrix, 0, sx, sy, sz);
        matrixList.add(matrix);
    }

    public void postRotate(float degrees, float x, float y, float z) {
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.rotateM(matrix, 0, degrees, x, y, z);
        matrixList.add(matrix);
    }

    public void postTranslate(float ox, float oy, float oz) {
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, ox, oy, oz);
        matrixList.add(matrix);
    }

    public void invert(Matrix3D out) {
        out.reset();
        for (int i = matrixList.size() - 1; i >= 0; i--) {
            float[] matrix = matrixList.get(i);
            float[] im = new float[16];
            Matrix.invertM(im, 0, matrix, 0);
            out.matrixList.add(im);
        }
    }
}
