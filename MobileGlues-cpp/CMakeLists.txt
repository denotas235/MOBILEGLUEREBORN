cmake_minimum_required(VERSION 3.22.1)
project("mobileglues")
enable_language(CXX)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# glslang & SPIRV-Cross
set(ENABLE_RTTI                ON  CACHE BOOL "Enables RTTI" FORCE)
set(ENABLE_HLSL                OFF CACHE BOOL "Disable HLSL" FORCE)
set(BUILD_EXTERNAL             OFF CACHE BOOL "Disable build external dependencies for glslang" FORCE)
set(ENABLE_GLSLANG_BINARIES    OFF CACHE BOOL "Disable glslangValidator/spirv-remap" FORCE)
set(ENABLE_OPT                 OFF CACHE BOOL "Disable SPIRV-Tools opt usage in glslang" FORCE)

set(SPIRV_CROSS_ENABLE_C_API   ON  CACHE BOOL "Enable C API" FORCE)
set(SPIRV_CROSS_ENABLE_GLSL    ON  CACHE BOOL "Enable GLSL backend" FORCE)
set(SPIRV_CROSS_ENABLE_REFLECT OFF CACHE BOOL "Disable reflect" FORCE)
set(SPIRV_CROSS_ENABLE_HLSL    OFF CACHE BOOL "Disable HLSL backend" FORCE)
set(SPIRV_CROSS_ENABLE_MSL     OFF CACHE BOOL "Disable MSL backend" FORCE)
set(SPIRV_CROSS_ENABLE_CPP     OFF CACHE BOOL "Disable C++ API target" FORCE)
set(SPIRV_CROSS_CLI            OFF CACHE BOOL "Disable CLI binary" FORCE)
set(SPIRV_CROSS_STATIC         ON  CACHE BOOL "Prefer static libs" FORCE)
set(SPIRV_CROSS_ENABLE_UTIL    OFF CACHE BOOL "Disable util" FORCE)

add_subdirectory(3rdparty/glslang)
add_subdirectory(3rdparty/SPIRV-Cross)

if(MACOS)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -w -fvisibility=default -funwind-tables -g -D_THREAD_SAFE -fPIC -stdlib=libc++")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -w -g -std=gnu99 -funwind-tables -fvisibility=hidden")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libstdc++")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -w -fvisibility=hidden -funwind-tables -g -D_THREAD_SAFE -fPIC")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -w -g -std=gnu99 -funwind-tables -fvisibility=hidden")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libstdc++")
    set(CMAKE_ANDROID_STL_TYPE c++_static)
endif()

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    include(CheckIPOSupported)
    include(CheckCCompilerFlag)
    include(CheckCXXCompilerFlag)
    check_ipo_supported(RESULT LTOSupported OUTPUT LTOError)
    check_c_compiler_flag("-flto" HAS_LTO_C)
    check_cxx_compiler_flag("-flto" HAS_LTO_CXX)
    if (LTOSupported OR (HAS_LTO_C AND HAS_LTO_CXX))
        check_c_compiler_flag("-flto=thin" HAS_THINLTO_C)
        check_cxx_compiler_flag("-flto=thin" HAS_THINLTO_CXX)
        if (HAS_THINLTO_C AND HAS_THINLTO_CXX)
            message(STATUS "ThinLTO supported, using -flto=thin")
            add_compile_options(-flto=thin)
            add_link_options(-flto=thin)
        else()
            message(STATUS "ThinLTO not available, fallback to CMAKE IPO")
            set(CMAKE_INTERPROCEDURAL_OPTIMIZATION TRUE)
        endif()
    else()
        message(STATUS "IPO not supported: ${LTOError}")
    endif()
endif()

if (CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang" AND NOT MATCHES "AppleClang")
    add_compile_options(-O3 -ffunction-sections -fdata-sections)
    add_link_options(-Wl,--gc-sections)
elif (CMAKE_CXX_COMPILER_ID MATCHES "MSVC")
else ()
    add_compile_options(-O2)
endif()

if (CMAKE_CXX_COMPILER_ID MATCHES "MSVC")
    add_compile_options(/Zc:preprocessor)
    add_compile_options(/Zc:__cplusplus)
endif()

set(PROFILING OFF)

add_library(${CMAKE_PROJECT_NAME} SHARED
    # ── Core ──────────────────────────────────────────────
    init.cpp
    native_astc_loader.cpp
    unified_core.cpp
    main.cpp

    # ── GL stub / native / state ───────────────────────────
    gl/gl_stub.cpp
    gl/gl_native.cpp
    gl/gl.cpp
    gl/envvars.cpp
    gl/log.cpp
    gl/program.cpp
    gl/shader.cpp
    gl/shader_sanitizer.cpp
    gl/shader_sanitizer.h
    gl/shader_patcher.cpp
    gl/shader_patcher.h
    gl/glsl/shader_header_manager.cpp
    gl/glsl/shader_header_manager.h
    gl/framebuffer.cpp
    gl/texture.cpp
    gl/getter.cpp
    gl/pixel.cpp
    gl/random_string_gen.cpp
    gl/drawing.cpp
    gl/buffer.cpp
    gl/vertexattrib.cpp
    gl/mg.cpp

    # ── Extension & capability detection ──────────────────
    gl/extension_scanner.cpp
    core/mali_caps.cpp
    core/gl_context.cpp

    # ── Lighting & Phase 2 (PLS / FBFetch) ────────────────
    gl/phase2_lighting.cpp
    lighting/shadow_pipeline.cpp
    lighting/ambient_occlusion.cpp
    lighting/global_illumination.cpp

    # ── Rendering pipeline & effects ──────────────────────
    gl/rendering_pipeline.cpp
    effects/bloom_pls.cpp
    effects/reflections_ssr.cpp
    effects/water_tessellation.cpp

    # ── GPU-Driven / culling ───────────────────────────────
    gl/gpu_driven.cpp
    gl/multidraw.cpp
    gl/earlyZ.cpp
    gpu_driven/hzb_occlusion.cpp
    gpu_driven/async_compute.cpp

    # ── Buffers & atlas ───────────────────────────────────
    gl/persistent_buffers.cpp
    buffers/texture_atlas.cpp

    # ── Sorting & optimization ─────────────────────────────
    gl/mali_sorter.cpp
    gl/v_sight_math.cpp
    optimization/dynamic_lod.cpp

    # ── Rendering subsystems ──────────────────────────────
    rendering/entity_renderer.cpp
    rendering/particle_renderer.cpp
    rendering/weather_renderer.cpp
    rendering/ui_renderer.cpp

    # ── Shader binary cache ────────────────────────────────
    gl/shader_binary_cache.cpp

    # ── Pipeline passes (deferred / transparent) ───────────
    pipeline/geometry_pass.cpp
    pipeline/lighting_pass.cpp
    pipeline/transparent_pass.cpp

    # ── Time of day ────────────────────────────────────────
    time/time_of_day.cpp

    # ── GLSL utilities ─────────────────────────────────────
    gl/glsl/glsl_for_es.cpp
    gl/glsl/cache.cpp

    # ── Extensions wrappers ────────────────────────────────
    gl/ExtWrappers/DSAWrapper.cpp
    gl/ExtWrappers/MultiBindWrapper.cpp

    # ── FSR1 upscaler ──────────────────────────────────────
    gl/FSR1/FSR1.cpp

    # ── EGL / GLX / GLES loader ───────────────────────────
    glx/lookup.cpp
    egl/egl.cpp
    egl/loader.cpp
    gles/loader.cpp

    # ── Config ────────────────────────────────────────────
    config/cJSON.c
    config/config.cpp
    config/gpu_utils.cpp
    config/settings.cpp
)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
    # Existing search paths
    ${CMAKE_CURRENT_SOURCE_DIR}/gl
    ${CMAKE_CURRENT_SOURCE_DIR}/gl/glsl
    ./includes
    ./3rdparty/glm
    ./3rdparty/xxhash
    # New module paths (headers accessible without path prefix)
    ${CMAKE_CURRENT_SOURCE_DIR}/core
    ${CMAKE_CURRENT_SOURCE_DIR}/pipeline
    ${CMAKE_CURRENT_SOURCE_DIR}/gpu_driven
    ${CMAKE_CURRENT_SOURCE_DIR}/buffers
    ${CMAKE_CURRENT_SOURCE_DIR}/lighting
    ${CMAKE_CURRENT_SOURCE_DIR}/effects
    ${CMAKE_CURRENT_SOURCE_DIR}/rendering
    ${CMAKE_CURRENT_SOURCE_DIR}/optimization
    ${CMAKE_CURRENT_SOURCE_DIR}/time
)
target_include_directories(${CMAKE_PROJECT_NAME} PUBLIC ${CMAKE_SOURCE_DIR}/include)

target_link_libraries(${CMAKE_PROJECT_NAME} PRIVATE
    glslang::glslang
    spirv-cross-c
)

if(MACOS)
    target_link_libraries(${CMAKE_PROJECT_NAME} PUBLIC
        "-F${CMAKE_SOURCE_DIR}/external"
        "-framework libEGL"
        "-framework libGLESv2"
        "-framework Metal"
        "-framework Foundation"
        "-framework QuartzCore"
    )
else()
    target_link_libraries(${CMAKE_PROJECT_NAME} PUBLIC
        android
        log
    )
endif()

if (PROFILING)
    find_package(Threads)
    include_directories(${CMAKE_SOURCE_DIR}/3rdparty/perfetto/sdk)
    add_library(perfetto STATIC ${CMAKE_SOURCE_DIR}/3rdparty/perfetto/sdk/perfetto.cc)
    target_link_libraries(${CMAKE_PROJECT_NAME} perfetto ${CMAKE_THREAD_LIBS_INIT})
    target_compile_definitions(${CMAKE_PROJECT_NAME} PUBLIC PROFILING=1)
endif()
