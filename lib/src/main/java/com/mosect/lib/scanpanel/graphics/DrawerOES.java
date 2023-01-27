package com.mosect.lib.scanpanel.graphics;

import android.opengl.GLES11Ext;
import android.opengl.Matrix;

import com.mosect.lib.easygl2.GLTextureWindow;
import com.mosect.lib.scanpanel.shader.ShaderOES;

public class DrawerOES extends Drawer2D<GLTextureWindow, ShaderOES> {

    public DrawerOES(ContentMatrix contentMatrix, float z) {
        super(contentMatrix, z);
    }

    @Override
    protected void getTextureMatrix(GLTextureWindow texture, float[] out) {
        texture.updateTexImage();
        texture.getSurfaceTexture().getTransformMatrix(out);
        Matrix.scaleM(out, 0, 1f, -1f, 1f);
        Matrix.translateM(out, 0, 0, -1f, 0f);
    }

    @Override
    protected int getTextureType() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }
}
