// MobileGlues - Shader Header Manager
// Copyright (c) 2025-2026 Denocorp
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#include "shader_header_manager.h"
#include <vector>
#include <sstream>
#include <algorithm>

std::string normalise_shader_header(std::string source, GLenum shader_type, unsigned int target_es_version) {
    std::vector<std::string> lines;
    std::istringstream iss(source);
    std::string line;

    // 1. Separar linhas e remover whitespace inicial
    while (std::getline(iss, line)) {
        size_t start = line.find_first_not_of(" \t");
        if (start != std::string::npos) {
            lines.push_back(line.substr(start));
        }
    }

    // 2. Extrair e remover linhas de #version, #extension, e precision
    std::string version_line;
    std::vector<std::string> extension_lines;
    std::vector<std::string> precision_lines;
    std::vector<std::string> body_lines;

    for (const auto& l : lines) {
        if (l.find("#version") == 0) {
            version_line = l;
        } else if (l.find("#extension") == 0) {
            // Remover extensões não suportadas (ex.: ASTC no shader)
            if (l.find("GL_EXT_texture_compression_astc") == std::string::npos) {
                extension_lines.push_back(l);
            }
        } else if (l.find("precision") == 0) {
            precision_lines.push_back(l);
        } else if (!l.empty()) {
            body_lines.push_back(l);
        }
    }

    // 3. Construir o novo cabeçalho
    std::ostringstream new_source;

    // 3.1. #version (sempre primeiro)
    if (version_line.empty()) {
        // Se não houver #version, adicione um padrão para ES 3.2
        new_source << "#version " << target_es_version << " es\n";
    } else {
        // Se já houver #version, garanta que é ES 3.0+
        if (version_line.find("es") == std::string::npos) {
            new_source << "#version " << target_es_version << " es\n";
        } else {
            new_source << version_line << "\n";
        }
    }

    // 3.2. #extension (após #version)
    for (const auto& ext : extension_lines) {
        new_source << ext << "\n";
    }

    // 3.3. precision (após #extension)
    bool has_precision = !precision_lines.empty();
    for (const auto& prec : precision_lines) {
        new_source << prec << "\n";
    }

    // 3.4. Adicionar precision padrão se não existir
    if (shader_type == GL_FRAGMENT_SHADER && !has_precision) {
        new_source << "precision highp float;\n";
        new_source << "precision highp int;\n";
        new_source << "precision mediump sampler2D;\n";
        new_source << "precision mediump sampler2DArray;\n";
    } else if (shader_type == GL_VERTEX_SHADER && !has_precision) {
        new_source << "precision highp float;\n";
        new_source << "precision highp int;\n";
    }

    // 3.5. Corpo do shader
    for (const auto& body_line : body_lines) {
        new_source << body_line << "\n";
    }

    return new_source.str();
}
