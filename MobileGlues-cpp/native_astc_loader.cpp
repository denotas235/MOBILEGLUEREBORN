#ifndef __APPLE__
#include <jni.h>
#include <GLES3/gl3.h>
#include <cstdio>
#include <vector>

extern "C" JNIEXPORT jboolean JNICALL Java_com_nexus_astcmod_NativeASTCLoader_uploadASTC(JNIEnv* env, jclass clazz, jstring path, jint w, jint h) {
    const char* nativePath = env->GetStringUTFChars(path, nullptr);
    FILE* file = fopen(nativePath, "rb");
    
    if (!file) {
        env->ReleaseStringUTFChars(path, nativePath);
        return JNI_FALSE;
    }

    // LER CABEÇALHO ASTC (16 bytes)
    unsigned char header[16];
    fread(header, 1, 16, file);

    // O formato interno do bloco
    // O byte 4 e 5 representam o block_x e block_y do ASTC
    int blockX = header[4];
    int blockY = header[5];
    
    // Fallback default = 6x6
    GLenum internalFormat = 0x93B4; 
    
    if (blockX == 4 && blockY == 4) internalFormat = 0x93B0;
    else if (blockX == 5 && blockY == 5) internalFormat = 0x93B2; // 5x5
    else if (blockX == 6 && blockY == 6) internalFormat = 0x93B4; // 6x6
    else if (blockX == 8 && blockY == 8) internalFormat = 0x93B7; // 8x8
    else if (blockX == 10 && blockY == 10) internalFormat = 0x93B9; // 10x10

    // Ler dados comprimidos (o arquivo inteiro menos os 16 bytes do cabeçalho)
    fseek(file, 0, SEEK_END);
    long size = ftell(file) - 16;
    fseek(file, 16, SEEK_SET);

    std::vector<unsigned char> buffer(size);
    fread(buffer.data(), 1, size, file);
    fclose(file);

    // MÁGICA DO ZERO-COPY E UPLOAD DIRETO:
    // Pula completamente a descodificação STB/libpng da CPU
    glCompressedTexImage2D(GL_TEXTURE_2D, 0, internalFormat, w, h, 0, size, buffer.data());

    env->ReleaseStringUTFChars(path, nativePath);
    return JNI_TRUE;
}
#endif
