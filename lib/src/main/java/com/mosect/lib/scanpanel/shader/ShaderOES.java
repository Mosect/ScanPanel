package com.mosect.lib.scanpanel.shader;

import android.content.Context;
import android.opengl.GLES11Ext;

import com.mosect.lib.easygl2.util.GLUtils;

import java.nio.Buffer;

public class ShaderOES extends Shader2D {

    public ShaderOES(Context context) {
        super(context);
    }

    @Override
    protected String onLoadFragSource() {
        return GLUtils.loadAssetsText(context, "ScanPanel/shader_oes.frag");
    }

    public void draw(int textureID, float[] cameraMatrix, Buffer position,
                     float[] textureMatrix, Buffer textureCoord) {
        draw(textureID, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraMatrix, position, textureMatrix, textureCoord);
    }
}
