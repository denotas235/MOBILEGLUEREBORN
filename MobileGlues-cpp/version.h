// NEXUS_VK_RENDER (NVR) - version.h
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef MOBILEGLUES_VERSION_H

#define VERSION_DEVELOPMENT 0
#define VERSION_ALPHA 1
#define VERSION_BETA 2
#define VERSION_RC 3
#define VERSION_RELEASE 10

#define MAJOR 1
#define MINOR 3
#define REVISION 4
#define PATCH 0

#define VERSION_TYPE VERSION_RELEASE

#if VERSION_TYPE == VERSION_RC
#define VERSION_RC_NUMBER 2
#endif

#define VERSION_SUFFIX ""

// Renderer identity — NEXUS_VK_RENDER (NVR)
#define RENDERERNAME      "NEXUS_VK_RENDER"
#define RENDERERNAME_SHORT "NVR"

#define MOBILEGLUES_VERSION_H
#endif // MOBILEGLUES_VERSION_H
