#include "unified_core.h"

// Instâncias globais do Motor Unificado
MobileGlues::NativeChunkManager g_ChunkManager;

#ifndef __APPLE__
extern "C" {

// 1. JNI para o V-Sight Culling
JNIEXPORT jboolean JNICALL Java_com_nexus_astcmod_UnifiedCoreJNI_isEntityInFocus(
    JNIEnv* env, jclass clazz, 
    jfloat px, jfloat py, jfloat pz, 
    jfloat lx, jfloat ly, jfloat lz, 
    jfloat ex, jfloat ey, jfloat ez) 
{
    return MobileGlues::VSightController::isEntityInFocus(px, py, pz, lx, ly, lz, ex, ey, ez);
}

// 2. JNI para Memória Nativa (Chunks)
JNIEXPORT void JNICALL Java_com_nexus_astcmod_UnifiedCoreJNI_allocateChunk(JNIEnv* env, jclass clazz, jint x, jint z) {
    g_ChunkManager.allocateChunk(x, z);
}

JNIEXPORT void JNICALL Java_com_nexus_astcmod_UnifiedCoreJNI_freeChunk(JNIEnv* env, jclass clazz, jint x, jint z) {
    g_ChunkManager.freeChunk(x, z);
}

// 3. JNI para Luz Paralela
JNIEXPORT void JNICALL Java_com_nexus_astcmod_UnifiedCoreJNI_propagateLightAsync(JNIEnv* env, jclass clazz, jint chunkX, jint chunkZ) {
    MobileGlues::StarlightNative::propagateLightAsync(chunkX, chunkZ);
}

// 4. JNI para Fix de Shaders
JNIEXPORT jstring JNICALL Java_com_nexus_astcmod_UnifiedCoreJNI_fixShaderForMali(JNIEnv* env, jclass clazz, jstring shaderSource) {
    const char* nativeString = env->GetStringUTFChars(shaderSource, 0);
    std::string source(nativeString);
    
    // 1. Injeta precisao
    if (source.find("precision ") == std::string::npos) {
        size_t versionPos = source.find("#version");
        if (versionPos != std::string::npos) {
            size_t insertPos = source.find('\n', versionPos);
            if (insertPos != std::string::npos) {
                source.insert(insertPos + 1, "precision highp float;\nprecision highp int;\n");
            }
        } else {
            source.insert(0, "precision highp float;\nprecision highp int;\n");
        }
    }
    
    // 2. Transpila
    size_t pos = 0;
    while((pos = source.find("varying ", pos)) != std::string::npos) { source.replace(pos, 8, "in "); pos += 3; }
    pos = 0;
    while((pos = source.find("attribute ", pos)) != std::string::npos) { source.replace(pos, 10, "in "); pos += 3; }

    env->ReleaseStringUTFChars(shaderSource, nativeString);
    return env->NewStringUTF(source.c_str());
}

}
#endif
