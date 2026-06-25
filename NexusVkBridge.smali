.class public Lcom/nexus/vulkan/NexusVkBridge;
.super Ljava/lang/Object;


# =============================================================================
# JNI Interface for libmobileglues.so
# =============================================================================
# This class provides Java <-> Native bridge for NVR (Nexus Vulkan Render)
# All native methods are implemented in libmobileglues.so
# =============================================================================


# --- Static Initializer ---
.method static clinit()V
    .locals 0
    
    # Load the native library
    const-string v0, "mobileglues"
    invokestatic {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
    
    return-void
.end method


# =============================================================================
# Public Static Methods (Called from Java/Kotlin)
# =============================================================================

# --- NVR Mode ---
.method public static isActive()Z
    .locals 1
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->nativeIsActive()I
    move-result v0
    if-eqz v0, :false
    const/4 v0, 0x1
    :
    return v0
    :false
    const/4 v0, 0x0
    goto :return
.end method


.method public static setNvrMode(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetNvrMode(Z)V
    return-void
.end method


# --- GLSL to SPIR-V Compilation ---
.method public static compileGlslToSpirv(Ljava/lang/String;I)[B
    .locals 3
    .param p0, "glslSource"    # String
    .param p1, "glslType"      # int (GL_VERTEX_SHADER=0x8B31, GL_FRAGMENT_SHADER=0x8B30, etc.)
    invokestatic {p0, p1}, Lcom/nexus/vulkan/NexusVkBridge;->nativeCompileGlslToSpirv(Ljava/lang/String;I)[B
    move-result-object v0
    return-object v0
.end method


# --- ANGLE Settings ---
.method public static setAngleMode(I)V
    .locals 1
    .param p0, "mode"    # int (0=Disabled, 1=Enabled)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetAngleMode(I)V
    return-void
.end method


# --- Shader Cache ---
.method public static setShaderCacheSize(I)V
    .locals 1
    .param p0, "sizeMB"    # int (size in megabytes)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetShaderCacheSize(I)V
    return-void
.end method


.method public static getShaderCacheSize()I
    .locals 1
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->nativeGetShaderCacheSize()I
    move-result v0
    return v0
.end method


# --- MultiDraw Emulation ---
.method public static setMultidrawMode(I)V
    .locals 1
    .param p0, "mode"    # int (0=Auto, 1=PreferIndirect, 2=PreferBaseVertex, 3=PreferMultidrawIndirect, 4=DrawElements, 5=Compute)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetMultidrawMode(I)V
    return-void
.end method


.method public static getMultidrawMode()I
    .locals 1
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->nativeGetMultidrawMode()I
    move-result v0
    return v0
.end method


# --- ANGLE Depth Clear Fix ---
.method public static setAngleDepthClearFixMode(I)V
    .locals 1
    .param p0, "mode"    # int (0=Disabled, 1=Mode1, 2=Mode2)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetAngleDepthClearFixMode(I)V
    return-void
.end method


# --- Custom OpenGL Version ---
.method public static setCustomGlVersion(I)V
    .locals 1
    .param p0, "version"    # int (0=Default, 32=ES3.2, 46=4.6, etc.)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetCustomGlVersion(I)V
    return-void
.end method


.method public static getCustomGlVersion()I
    .locals 1
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->nativeGetCustomGlVersion()I
    move-result v0
    return v0
.end method


# --- Extensions ---
.method public static setExtComputeShader(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetExtComputeShader(Z)V
    return-void
.end method


.method public static setExtTimerQuery(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetExtTimerQuery(Z)V
    return-void
.end method


.method public static setExtDirectStateAccess(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetExtDirectStateAccess(Z)V
    return-void
.end method


# --- Error Handling ---
.method public static setIgnoreErrorLevel(I)V
    .locals 1
    .param p0, "level"    # int (0=None, 1=Partial, 2=Full)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetIgnoreErrorLevel(I)V
    return-void
.end method


# --- Hide MG Environment ---
.method public static setHideMGEnvLevel(I)V
    .locals 1
    .param p0, "level"    # int (0=Disabled, 1=Level1)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetHideMGEnvLevel(I)V
    return-void
.end method


# --- FSR1 Settings ---
.method public static setFsr1Setting(I)V
    .locals 1
    .param p0, "preset"    # int (0=Disabled, 1=UltraQuality, 2=Quality, 3=Balanced, 4=Performance)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetFsr1Setting(I)V
    return-void
.end method


# --- GPU Optimizations ---

## Anisotropic Filtering
.method public static setAnisotropicFilteringEnabled(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetAnisotropicFilteringEnabled(Z)V
    return-void
.end method


.method public static setAnisotropicFilteringMaxLevel(I)V
    .locals 1
    .param p0, "maxLevel"    # int (1-16)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetAnisotropicFilteringMaxLevel(I)V
    return-void
.end method


## Framebuffer Fetch
.method public static setFramebufferFetchEnabled(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetFramebufferFetchEnabled(Z)V
    return-void
.end method


## Phase 2 Lighting
.method public static setPhase2LightingEnabled(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetPhase2LightingEnabled(Z)V
    return-void
.end method


## Shader Binary Cache
.method public static setShaderBinaryCacheEnabled(Z)V
    .locals 1
    .param p0, "enabled"    # boolean
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetShaderBinaryCacheEnabled(Z)V
    return-void
.end method


.method public static setShaderBinaryCachePath(Ljava/lang/String;)V
    .locals 1
    .param p0, "path"    # String
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetShaderBinaryCachePath(Ljava/lang/String;)V
    return-void
.end method


# --- Debug & Logging ---
.method public static setDebugLogLevel(I)V
    .locals 1
    .param p0, "level"    # int (0=Off, 1=Error, 2=Warn, 3=Info, 4=Debug)
    invokestatic {p0}, Lcom/nexus/vulkan/NexusVkBridge;->nativeSetDebugLogLevel(I)V
    return-void
.end method


.method public static getVersionString()Ljava/lang/String;
    .locals 1
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->nativeGetVersionString()Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method


.method public static getGPUInfo()Ljava/lang/String;
    .locals 1
    invokestatic {}, Lcom/nexus/vulkan/NexusVkBridge;->nativeGetGPUInfo()Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method


# =============================================================================
# Native Methods (Implemented in libmobileglues.so)
# =============================================================================

.method public static native nativeIsActive()I
.end method

.method public static native nativeSetNvrMode(Z)V
.end method

.method public static native nativeCompileGlslToSpirv(Ljava/lang/String;I)[B
.end method

.method public static native nativeSetAngleMode(I)V
.end method

.method public static native nativeSetShaderCacheSize(I)V
.end method

.method public static native nativeGetShaderCacheSize()I
.end method

.method public static native nativeSetMultidrawMode(I)V
.end method

.method public static native nativeGetMultidrawMode()I
.end method

.method public static native nativeSetAngleDepthClearFixMode(I)V
.end method

.method public static native nativeSetCustomGlVersion(I)V
.end method

.method public static native nativeGetCustomGlVersion()I
.end method

.method public static native nativeSetExtComputeShader(Z)V
.end method

.method public static native nativeSetExtTimerQuery(Z)V
.end method

.method public static native nativeSetExtDirectStateAccess(Z)V
.end method

.method public static native nativeSetIgnoreErrorLevel(I)V
.end method

.method public static native nativeSetHideMGEnvLevel(I)V
.end method

.method public static native nativeSetFsr1Setting(I)V
.end method

.method public static native nativeSetAnisotropicFilteringEnabled(Z)V
.end method

.method public static native nativeSetAnisotropicFilteringMaxLevel(I)V
.end method

.method public static native nativeSetFramebufferFetchEnabled(Z)V
.end method

.method public static native nativeSetPhase2LightingEnabled(Z)V
.end method

.method public static native nativeSetShaderBinaryCacheEnabled(Z)V
.end method

.method public static native nativeSetShaderBinaryCachePath(Ljava/lang/String;)V
.end method

.method public static native nativeSetDebugLogLevel(I)V
.end method

.method public static native nativeGetVersionString()Ljava/lang/String;
.end method

.method public static native nativeGetGPUInfo()Ljava/lang/String;
.end method
