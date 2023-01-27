package com.mosect.app.scanpanel;

import android.app.Application;
import android.graphics.RectF;
import android.opengl.Matrix;

import com.mosect.lib.scanpanel.graphics.ContentMatrix;
import com.mosect.lib.scanpanel.graphics.Matrix3D;

import java.util.Arrays;

public class MainApplication extends Application {

    private void testMatrix() {
        float[] point1 = {1, 1, 0f, 1f};
        float[] point2 = new float[point1.length];
        float[] matrix = new float[16 * 3];
        Matrix.setIdentityM(matrix, 0);

        Matrix.setIdentityM(matrix, 32);
        Matrix.rotateM(matrix, 32, 90, 0, 0, -1f);
        System.arraycopy(matrix, 0, matrix, 16, 16);
        Matrix.multiplyMM(matrix, 0, matrix, 16, matrix, 32);
        Matrix.multiplyMV(point2, 0, matrix, 0, point1, 0);
        System.out.printf("%s >>> %s%n", Arrays.toString(point1), Arrays.toString(point2));

        Matrix.setIdentityM(matrix, 32);
        Matrix.translateM(matrix, 32, 5, 5, 0f);
        System.arraycopy(matrix, 0, matrix, 16, 16);
        Matrix.multiplyMM(matrix, 0, matrix, 16, matrix, 32);
        Matrix.multiplyMV(point2, 0, matrix, 0, point1, 0);
        System.out.printf("%s >>> %s%n", Arrays.toString(point1), Arrays.toString(point2));

        Matrix.setIdentityM(matrix, 32);
        Matrix.scaleM(matrix, 32, 2, 2, 1f);
        System.arraycopy(matrix, 0, matrix, 16, 16);
        Matrix.multiplyMM(matrix, 0, matrix, 16, matrix, 32);
        Matrix.multiplyMV(point2, 0, matrix, 0, point1, 0);
        System.out.printf("%s >>> %s%n", Arrays.toString(point1), Arrays.toString(point2));
    }

    private void testMatrix3D() {
        Matrix3D matrix3D = new Matrix3D();
        float[] point1 = {1, 1, 0f};
        float[] point2 = new float[3];

        matrix3D.postRotate(90, 0, 0, -1f);
        matrix3D.mapPoints(point1, point2);
        System.out.printf("%s >>> %s%n", Arrays.toString(point1), Arrays.toString(point2));

        matrix3D.postScale(2f, 2f, 1f);
        matrix3D.mapPoints(point1, point2);
        System.out.printf("%s >>> %s%n", Arrays.toString(point1), Arrays.toString(point2));

        matrix3D.postTranslate(5f, 5f, 0f);
        matrix3D.mapPoints(point1, point2);
        System.out.printf("%s >>> %s%n", Arrays.toString(point1), Arrays.toString(point2));
    }

    private void testContentMatrix() {
        RectF viewportRect = new RectF(0, 1920, 1080, 0);
        RectF contentRect = new RectF(0, 300, 400, 0);
        ContentMatrix contentMatrix = new ContentMatrix(contentRect, viewportRect);
        contentMatrix.update(ContentMatrix.ScaleType.CENTER_CROP, 90, false);
        float[] points = {
                0, 0, 1,
                400, 0, 1,
                0, 300, 1,
                400, 300, 1,
        };
        float[] points2 = new float[points.length];
        contentMatrix.mapVec3Points(points, points2);
        for (int i = 0; i < points.length; i += 3) {
            System.out.printf("(%s, %s, %s) >>> (%s, %s, %s) %n",
                    points[i], points[i + 1], points[i + 2],
                    points2[i], points2[i + 1], points2[i + 2]);
        }
        RectF rect1 = new RectF(1000, 0, 1080, 300);
        RectF rect2 = new RectF();
        contentMatrix.viewportToContent2D(rect1, rect2);
        System.out.printf("%s >>> %s%n", rect1, rect2);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        testContentMatrix();
//        testMatrix();
//        testMatrix3D();
    }
}
