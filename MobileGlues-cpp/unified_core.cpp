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

}
#endif
