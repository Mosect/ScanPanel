package com.mosect.lib.scanpanel.shader;

import android.content.Context;
import android.opengl.GLES20;

import com.mosect.lib.easygl2.GLException;
import com.mosect.lib.easygl2.util.GLUtils;

import java.nio.Buffer;

public class ShaderTEX extends Shader2D {

    private int textureNumHandle;

    public ShaderTEX(Context context) {
        super(context);
    }

    @Override
    protected String onLoadFragSource() {
        return GLUtils.loadAssetsText(context, "ScanPanel/shader_tex.frag");
    }

    @Override
    protected void onInitProgram() {
        super.onInitProgram();
        textureNumHandle = getUniformLocation("textureNum");
    }

    protected void putTextureNum(int num) {
        GLES20.glUniform1i(textureNumHandle, num);
        GLException.checkGLError("glUniform1i");
    }

    @Override
    protected void putExt() {
        super.putExt();
        putTextureNum(0);
    }

    public void draw(int textureID, float[] cameraMatrix, Buffer position, float[] textureMatrix, Buffer textureCoord) {
        draw(textureID, GLES20.GL_TEXTURE_2D, cameraMatrix, position, textureMatrix, textureCoord);
    }
}
