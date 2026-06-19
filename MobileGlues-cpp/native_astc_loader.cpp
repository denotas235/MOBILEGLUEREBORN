// MobileGlues - native_astc_loader.cpp
// ASTC cache loader: zero-copy upload from /sdcard/MG/cache via glCompressedTexImage2D
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <jni.h>
#include <GLES3/gl3.h>
#include <cstdio>
#include <string>
#include <vector>
#include <android/log.h>

#define MLOG(...)  __android_log_print(ANDROID_LOG_INFO,  "MG_ASTC", __VA_ARGS__)
#define MLOGE(...) __android_log_print(ANDROID_LOG_ERROR, "MG_ASTC", __VA_ARGS__)

static GLenum astcFmt(int bx,int by){
    if(bx==4 &&by==4 )return 0x93B0u;
    if(bx==5 &&by==4 )return 0x93B1u;
    if(bx==5 &&by==5 )return 0x93B2u;
    if(bx==6 &&by==5 )return 0x93B3u;
    if(bx==6 &&by==6 )return 0x93B4u;
    if(bx==8 &&by==5 )return 0x93B5u;
    if(bx==8 &&by==6 )return 0x93B6u;
    if(bx==8 &&by==8 )return 0x93B7u;
    if(bx==10&&by==5 )return 0x93B8u;
    if(bx==10&&by==6 )return 0x93B9u;
    if(bx==10&&by==8 )return 0x93BAu;
    if(bx==10&&by==10)return 0x93BBu;
    if(bx==12&&by==10)return 0x93BCu;
    if(bx==12&&by==12)return 0x93BDu;
    return 0x93B4u;
}

static bool doUpload(const char* path,int w,int h){
    FILE* f=fopen(path,"rb"); if(!f){MLOGE("open fail: %s",path);return false;}
    unsigned char hdr[16];
    if(fread(hdr,1,16,f)!=16){fclose(f);return false;}
    if(hdr[0]!=0x13||hdr[1]!=0xAB||hdr[2]!=0xA1||hdr[3]!=0x5C){
        MLOGE("bad magic: %s",path); fclose(f); return false;
    }
    int bx=hdr[4],by=hdr[5];
    int fw=hdr[7]|(hdr[8]<<8)|(hdr[9]<<16);
    int fh=hdr[10]|(hdr[11]<<8)|(hdr[12]<<16);
    if(w<=0)w=fw; if(h<=0)h=fh;
    GLenum fmt=astcFmt(bx,by);
    fseek(f,0,SEEK_END); long total=ftell(f); long dsz=total-16;
    fseek(f,16,SEEK_SET);
    if(dsz<=0){fclose(f);return false;}
    std::vector<unsigned char> buf((size_t)dsz);
    if(fread(buf.data(),1,(size_t)dsz,f)!=(size_t)dsz){fclose(f);return false;}
    fclose(f);
    glCompressedTexImage2D(GL_TEXTURE_2D,0,fmt,w,h,0,(GLsizei)dsz,buf.data());
    MLOG("ASTC upload OK: %s [%dx%d blk=%dx%d fmt=0x%x]",path,w,h,bx,by,(unsigned)fmt);
    return true;
}

// ── Pending ASTC state ────────────────────────────────────────
static std::string g_pendingPath;
static int g_pendingW=0, g_pendingH=0;
static bool g_pendingSet=false;

extern "C" bool mg_hasPendingAstc(){ return g_pendingSet && !g_pendingPath.empty(); }

extern "C" bool mg_applyPendingAstc(){
    if(!g_pendingSet)return false;
    g_pendingSet=false;
    bool ok=doUpload(g_pendingPath.c_str(),g_pendingW,g_pendingH);
    g_pendingPath.clear(); g_pendingW=0; g_pendingH=0;
    return ok;
}

// ── JNI ──────────────────────────────────────────────────────

extern "C" JNIEXPORT void JNICALL
Java_com_nexus_astcmod_NativeASTCLoader_setNextAstcCache(
    JNIEnv* env,jclass,jstring jpath,jint w,jint h){
    const char* p=env->GetStringUTFChars(jpath,nullptr);
    g_pendingPath = p ? p : "";
    g_pendingW=(int)w; g_pendingH=(int)h;
    g_pendingSet=!g_pendingPath.empty();
    env->ReleaseStringUTFChars(jpath,p);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nexus_astcmod_NativeASTCLoader_uploadASTC(
    JNIEnv* env,jclass,jstring jpath,jint w,jint h){
    const char* p=env->GetStringUTFChars(jpath,nullptr);
    bool ok=p?doUpload(p,(int)w,(int)h):false;
    env->ReleaseStringUTFChars(jpath,p);
    return ok?JNI_TRUE:JNI_FALSE;
}

#endif
