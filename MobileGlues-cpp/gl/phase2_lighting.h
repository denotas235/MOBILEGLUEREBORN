// MobileGlues - gl/phase2_lighting.h
// Phase 2: PLS + FBFetch Lighting & Shadow Optimization for Mali-G52 (TBDR)
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef PHASE2_LIGHTING_H
#define PHASE2_LIGHTING_H

#include <string>
#include <cstdint>

// ============================================================
// Por que PLS + FBFetch é tão poderoso no Mali-G52?
// ============================================================
// O Mali-G52 usa arquitetura TBDR (Tile Based Deferred Rendering):
//   - A GPU divide a tela em tiles de 16x16 pixels
//   - Cada tile tem ~32 bytes/pixel de SRAM ultrarrápida (tile memory)
//   - PLS armazena dados de luz/sombra NESSA sram durante o frame
//   - FBFetch lê o framebuffer anterior SEM sair da tile memory
//   - Resultado: ZERO acesso à RAM principal durante passes de luz/sombra
//
// Ganho esperado no Minecraft Bedrock:
//   - 15-25% mais FPS em cenas com iluminação dinâmica
//   - 30-40% menos bandwidth de GPU
//   - Menos aquecimento e consumo de bateria
// ============================================================

// Status do sistema Phase 2
struct Phase2Status {
    bool pls_supported       = false;  // GL_EXT_shader_pixel_local_storage
    bool fbfetch_supported   = false;  // GL_EXT_shader_framebuffer_fetch
    bool tbdr_mode_active    = false;  // Usando tile memory para luz/sombra
    bool initialized         = false;
    int  shaders_patched     = 0;      // Shaders que receberam injeção PLS/FBFetch
    int  draw_calls_saved    = 0;      // Draw calls eliminados por merge de passes
};

// Inicializa o sistema de iluminação Phase 2
void phase2_init();

// Checa se o sistema está ativo
bool phase2_is_active();

// Retorna status atual (para logs/diagnóstico)
const Phase2Status& phase2_get_status();

// Injeta diretivas PLS em fragment shaders que fazem cálculo de luz
// Retorna o shader modificado ou a string original se não aplicável
std::string phase2_inject_pls(const std::string& glsl_src, bool is_fragment);

// Injeta FBFetch em fragment shaders que fazem blending de luz/sombra
std::string phase2_inject_fbfetch(const std::string& glsl_src);

// Chamado em glBindFramebuffer: notifica sistema de troca de FBO
void phase2_on_fbo_change(unsigned int new_fbo);

// Chamado em glDrawArrays/Elements: contabiliza draw calls mergeados
void phase2_on_draw_call();

// Reseta contadores por frame
void phase2_on_frame_end();

#endif // PHASE2_LIGHTING_H
