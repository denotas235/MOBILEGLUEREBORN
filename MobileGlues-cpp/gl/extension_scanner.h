#ifndef EXTENSION_SCANNER_H
#define EXTENSION_SCANNER_H

#include <string>
#include <set>

struct GPUCapabilities {
    std::set<std::string> gl_extensions;
    std::set<std::string> egl_extensions;
    std::set<std::string> vk_extensions;
    bool has_astc = false;
    bool has_pls = false;
    bool has_fbfetch = false;
    bool has_shader_binary = false;
};

void init_extension_scanner();
const GPUCapabilities& get_gpu_capabilities();
bool has_extension(const char* ext_name);

#endif // EXTENSION_SCANNER_H
