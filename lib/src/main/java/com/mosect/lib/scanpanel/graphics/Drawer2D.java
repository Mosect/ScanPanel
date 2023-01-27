package com.mosect.lib.scanpanel.graphics;

import android.graphics.RectF;
import android.opengl.Matrix;

import com.mosect.lib.easygl2.GLTexture;
import com.mosect.lib.scanpanel.shader.Shader2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class Drawer2D<T extends GLTexture, S extends Shader2D> {

    private final static float NERA = 0.5f;
    private final static float FAR = 2.5f;
    private final float[] cameraMatrix = new float[16];
    private final FloatBuffer position;
    private final float[] textureMatrix = new float[16];
    private final FloatBuffer textureCoord;

    public Drawer2D(ContentMatrix contentMatrix, float z) {
        RectF contentRect = contentMatrix.getContentRect();
        RectF viewportRect = contentMatrix.getViewportRect();
        float safeZ = -NERA - (FAR - NERA) * z;
        float[] points = new float[]{
                contentRect.left, contentRect.bottom, safeZ,
                contentRect.right, contentRect.bottom, safeZ,
                contentRect.left, contentRect.top, safeZ,
                contentRect.right, contentRect.top, safeZ,
        };
        float[] points2 = new float[points.length];
        contentMatrix.mapVec3Points(points, points2);

        position = ByteBuffer.allocateDirect(points.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        position.put(points2);
        textureCoord = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureCoord.put(new float[]{
                0f, 0f,
                1f, 0f,
                0f, 1f,
                1f, 1f,
        });

        Matrix.orthoM(cameraMatrix, 0, viewportRect.left, viewportRect.right,
                viewportRect.top, viewportRect.bottom, NERA, FAR);
    }

    public void draw(T texture, S shader) {
        position.position(0);
        textureCoord.position(0);
        getTextureMatrix(texture, textureMatrix);
        shader.draw(texture.getTextureId(), getTextureType(), cameraMatrix, position, textureMatrix, textureCoord);
    }

    protected abstract void getTextureMatrix(T texture, float[] out);

    protected abstract int getTextureType();
}
