package com.mosect.lib.scanpanel.graphics;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.mosect.lib.scanpanel.shader.ShaderTEX;

public class DrawerTEX extends Drawer2D<BitmapTexture, ShaderTEX> {

    public DrawerTEX(ContentMatrix contentMatrix, float z) {
        super(contentMatrix, z);
    }

    @Override
    protected void getTextureMatrix(BitmapTexture texture, float[] out) {
        Matrix.setIdentityM(out, 0);
    }

    @Override
    protected int getTextureType() {
        return GLES20.GL_TEXTURE_2D;
    }
}
