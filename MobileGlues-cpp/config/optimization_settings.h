#ifndef MOBILEGLUES_OPTIMIZATION_SETTINGS_H
#define MOBILEGLUES_OPTIMIZATION_SETTINGS_H

#include <string>
#include <vector>

struct OptimizationSettings {
    struct {
        bool enabled = true;
        int max_level = 8;        // 4x causava corrompimento de cores (slime, etc). 8x é seguro.
        bool force_limit = true;
    } anisotropic_filtering;

    struct {
        bool enabled = true;
        bool invalidate_depth = false;   // Desativado: causava player invisível e piscar (FBO 0 ainda usa depth)
        bool invalidate_stencil = false; // Desativado: causava piscar no céu e blocos na mão
    } framebuffer_invalidation;

    struct {
        bool enabled = true;
        bool scan_system_libs = true;
        bool cache_result = true;
        std::string cache_path = "/sdcard/MG/extensions_cache.json";
    } extension_scanner;

    struct {
        bool enabled = true;
        size_t max_ubo_size_kb = 64;
        bool auto_convert = true;
    } ubo_ssbo_conversion;

    struct {
        bool enabled = true;
        std::string cache_path = "/sdcard/MG/shader_binaries";
        bool use_arm_binary = true;
        bool use_arb_binary = false;
    } shader_binary_cache;

    struct {
        bool enabled = false;
        bool track_dirty_regions = true;
        int max_regions = 16;
    } damage_regions;

    struct {
        bool enabled = false;
        bool rewrite_shaders = true;
        bool detect_patterns = true;
        std::vector<std::string> supported_ops = {"bloom", "dof", "motion_blur"};
    } framebuffer_fetch;

    struct {
        bool enabled = false;
        int async_thread_pool = 4;
        bool cache_transcoded = true;
        std::vector<std::string> formats_to_transcode = {"DXT1", "BC7"};
        std::string quality = "balanced";
    } astc_transcoding;

    struct {
        bool enabled = true;
    } phase2_lighting;
};

#endif // MOBILEGLUES_OPTIMIZATION_SETTINGS_H
