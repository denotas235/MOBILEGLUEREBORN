// MobileGlues - native_astc_loader.cpp
// ASTC cache loader — zero-copy path: reads .astc from /sdcard/MG/cache
// and uploads directly via glCompressedTexImage2D, bypassing CPU decode.
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <jni.h>
#include <GLES3/gl3.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
#include <android/log.h>

#define MG_ASTC_LOG(...)  __android_log_print(ANDROID_LOG_INFO,  "MG_ASTC", __VA_ARGS__)
#define MG_ASTC_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "MG_ASTC", __VA_ARGS__)

// ── ASTC block-size → GL internal format ─────────────────────────────────────
static GLenum astcBlockFormat(int bx, int by) {
    if (bx == 4  && by == 4)  return 0x93B0; // GL_COMPRESSED_RGBA_ASTC_4x4_KHR
    if (bx == 5  && by == 4)  return 0x93B1;
    if (bx == 5  && by == 5)  return 0x93B2;
    if (bx == 6  && by == 5)  return 0x93B3;
    if (bx == 6  && by == 6)  return 0x93B4;
    if (bx == 8  && by == 5)  return 0x93B5;
    if (bx == 8  && by == 6)  return 0x93B6;
    if (bx == 8  && by == 8)  return 0x93B7;
    if (bx == 10 && by == 5)  return 0x93B8;
    if (bx == 10 && by == 6)  return 0x93B9;
    if (bx == 10 && by == 8)  return 0x93BA;
    if (bx == 10 && by == 10) return 0x93BB;
    if (bx == 12 && by == 10) return 0x93BC;
    if (bx == 12 && by == 12) return 0x93BD;
    return 0x93B4; // fallback: 6×6
}

// ── Shared load + upload ──────────────────────────────────────────────────────
static bool doUploadAstc(const char* nativePath, int w, int h) {
    FILE* f = fopen(nativePath, "rb");
    if (!f) {
        MG_ASTC_LOGE("Cannot open cache: %s", nativePath);
        return false;
    }

    // ASTC file header: 16 bytes
    // magic[4] | block_x | block_y | block_z | xsize[3] | ysize[3] | zsize[3]
    unsigned char hdr[16];
    if (fread(hdr, 1, 16, f) != 16) { fclose(f); return false; }

    // Validate ASTC magic: 0x13 0xAB 0xA1 0x5C
    if (hdr[0]!=0x13 || hdr[1]!=0xAB || hdr[2]!=0xA1 || hdr[3]!=0x5C) {
        MG_ASTC_LOGE("Not a valid ASTC file: %s", nativePath);
        fclose(f);
        return false;
    }

    int blockX = hdr[4], blockY = hdr[5];
    // Width/height from header (little-endian 24-bit) — use if caller passes 0
    int fileW = hdr[7] | (hdr[8]<<8) | (hdr[9]<<16);
    int fileH = hdr[10]| (hdr[11]<<8)| (hdr[12]<<16);
    if (w <= 0) w = fileW;
    if (h <= 0) h = fileH;

    GLenum fmt = astcBlockFormat(blockX, blockY);

    fseek(f, 0, SEEK_END);
    long totalSize = ftell(f);
    long dataSize  = totalSize - 16;
    fseek(f, 16, SEEK_SET);
    if (dataSize <= 0) { fclose(f); return false; }

    std::vector<unsigned char> buf((size_t)dataSize);
    if (fread(buf.data(), 1, (size_t)dataSize, f) != (size_t)dataSize) {
        fclose(f);
        return false;
    }
    fclose(f);

    // Zero-copy upload directly to currently-bound GL_TEXTURE_2D
    glCompressedTexImage2D(GL_TEXTURE_2D, 0, fmt, w, h, 0,
                           (GLsizei)dataSize, buf.data());
    MG_ASTC_LOG("ASTC upload OK: %s [%dx%d block=%dx%d fmt=0x%x]",
                nativePath, w, h, blockX, blockY, (unsigned)fmt);
    return true;
}

// ── Pending-ASTC state (set by Java Mixin before texture upload) ───────────────
static std::string g_pendingPath;
static int         g_pendingW = 0, g_pendingH = 0;
static bool        g_pendingSet = false;

// Called by texture.cpp before each glTexImage2D / glTexStorage2D
extern "C" bool mg_hasPendingAstc() {
    return g_pendingSet && !g_pendingPath.empty();
}

extern "C" bool mg_applyPendingAstc() {
    if (!g_pendingSet) return false;
    g_pendingSet = false;
    bool ok = doUploadAstc(g_pendingPath.c_str(), g_pendingW, g_pendingH);
    g_pendingPath.clear();
    return ok;
}

// ── JNI entry points ──────────────────────────────────────────────────────────

// Called from AstcTextureMixin before TextureAtlas.upload()
extern "C" JNIEXPORT void JNICALL
Java_com_nexus_astcmod_NativeASTCLoader_setNextAstcCache(
    JNIEnv* env, jclass, jstring jpath, jint w, jint h)
{
    const char* p = env->GetStringUTFChars(jpath, nullptr);
    g_pendingPath = p ? p : "";
    g_pendingW    = (int)w;
    g_pendingH    = (int)h;
    g_pendingSet  = !g_pendingPath.empty();
    env->ReleaseStringUTFChars(jpath, p);
}

// Direct upload from a given path (legacy / on-demand use)
extern "C" JNIEXPORT jboolean JNICALL
Java_com_nexus_astcmod_NativeASTCLoader_uploadASTC(
    JNIEnv* env, jclass, jstring jpath, jint w, jint h)
{
    const char* p = env->GetStringUTFChars(jpath, nullptr);
    bool ok = p ? doUploadAstc(p, (int)w, (int)h) : false;
    env->ReleaseStringUTFChars(jpath, p);
    return ok ? JNI_TRUE : JNI_FALSE;
}

#endif // !__APPLE__
