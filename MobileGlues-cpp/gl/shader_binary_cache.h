// MobileGlues - gl/shader_binary_cache.h
// Phase 3: Shader Program Binary Cache to eliminate compilation stutters (stuttering)
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef SHADER_BINARY_CACHE_H
#define SHADER_BINARY_CACHE_H

#include <GLES3/gl32.h>
#include <string>

// Inicializa o cache de binários
void shader_binary_cache_init();

// Tenta carregar o binário para o programa a partir do cache
bool shader_binary_cache_load(GLuint program);

// Salva o binário do programa compilado no cache
void shader_binary_cache_save(GLuint program);

#endif // SHADER_BINARY_CACHE_H
