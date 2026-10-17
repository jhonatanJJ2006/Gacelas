package com.gacelas

import com.gacelas.ui.three.rendererProblematico
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RendererTest {
    @Test fun swiftShaderUsaImagenDeReemplazo() {
        assertTrue(rendererProblematico("Android Emulator OpenGL ES Translator (Google SwiftShader)"))
        assertFalse(rendererProblematico("Adreno (TM) 650"))
        assertFalse(rendererProblematico("Android Emulator OpenGL ES Translator (ANGLE (Mesa, Vulkan 1.4 (llvmpipe)))"))
        assertFalse(rendererProblematico(null))
    }
}
