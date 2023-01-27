package com.mosect.lib.scanpanel.shader;

import android.content.Context;
import android.opengl.GLES20;

import com.mosect.lib.easygl2.GLException;
import com.mosect.lib.easygl2.GLShader;
import com.mosect.lib.easygl2.util.GLUtils;

import java.nio.Buffer;

public abstract class Shader2D extends GLShader {

    protected final Context context;

    private int cameraMatrixHandle;
    private int textureMatrixHandle;
    private int positionHandle;
    private int textureCoordHandle;

    public Shader2D(Context context) {
        this.context = context;
    }

    @Override
    protected void onInitProgram() {
        super.onInitProgram();
        cameraMatrixHandle = getUniformLocation("cameraMatrix");
        textureMatrixHandle = getUniformLocation("textureMatrix");
        positionHandle = getAttribLocation("position");
        textureCoordHandle = getAttribLocation("textureCoord");
    }

    @Override
    protected String onLoadVertSource() {
        return GLUtils.loadAssetsText(context, "ScanPanel/shader_2d.vert");
    }

    protected void putCameraMatrix(float[] matrix) {
        putMatrix(cameraMatrixHandle, matrix);
    }

    protected void putTextureMatrix(float[] matrix) {
        putMatrix(textureMatrixHandle, matrix);
    }

    protected void putPosition(Buffer data) {
        putAttributeValue(positionHandle, 3, data);
    }

    protected void putTextureCoord(Buffer data) {
        putAttributeValue(textureCoordHandle, 2, data);
    }

    protected void putAttributeValue(int handle, int size, Buffer data) {
        GLES20.glVertexAttribPointer(handle, size, GLES20.GL_FLOAT, false, 4 * size, data);
        GLException.checkGLError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(handle);
        GLException.checkGLError("glEnableVertexAttribArray");
    }

    protected void putMatrix(int handle, float[] matrix) {
        GLES20.glUniformMatrix4fv(handle, 1, false, matrix, 0);
        GLException.checkGLError("glUniformMatrix4fv");
    }

    public void draw(int textureID, int textureType, float[] cameraMatrix, Buffer position,
                     float[] textureMatrix, Buffer textureCoord) {
        // 禁止深度写入
        GLES20.glDepthMask(false);
        // 开启混合模式
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(textureType, textureID);

        GLES20.glUseProgram(getProgramId());
        GLException.checkGLError("glUseProgram");

        putCameraMatrix(cameraMatrix);
        putPosition(position);
        putTextureCoord(textureCoord);
        putTextureMatrix(textureMatrix);
        putExt();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLException.checkGLError("glDrawArrays");

        GLES20.glBindTexture(textureType, 0);

        // 开启深度写入
        GLES20.glDepthMask(true);
        // 关闭混合模式
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    protected void putExt() {
    }
}
