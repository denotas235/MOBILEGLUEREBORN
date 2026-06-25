#!/bin/bash

# =============================================================================
# NVR APK Build Script
# =============================================================================
# Este script:
# 1. Decompila a APK base (MobileGlRENDER_signed5.apk)
# 2. Insere todos os arquivos da interface Steampunk
# 3. Recompila a APK
# 4. Alinha com zipalign
# 5. Assina com apksigner
# 6. Gera NVR.apk final
# =============================================================================

set -e  # Sair em caso de erro

# =============================================================================
# CONFIGURAÇÕES
# =============================================================================

# Diretórios
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
WORK_DIR="$PROJECT_DIR/nvr_apk_build"
DECOMPILED_DIR="$WORK_DIR/decompiled"
BUILD_DIR="$WORK_DIR/build"
FINAL_APK="$PROJECT_DIR/NVR.apk"

# Arquivos
BASE_APK="$PROJECT_DIR/MobileGlRENDER_signed5.apk"
KEYSTORE="$WORK_DIR/debug.keystore"
KEYSTORE_PASS="123456"
KEY_ALIAS="debug"

# Ferramentas (caminhos padrão, ajustar se necessário)
APKTOOL="apktool"
ZIPALIGN="zipalign"
APKSIGNER="apksigner"

# =============================================================================
# FUNÇÕES AUXILIARES
# =============================================================================

echo_color() {
    local color=$1
    local message=$2
    case $color in
        "red") echo -e "\033[31m[ERRO] $message\033[0m" ;;
        "green") echo -e "\033[32m[OK] $message\033[0m" ;;
        "yellow") echo -e "\033[33m[AVISO] $message\033[0m" ;;
        "blue") echo -e "\033[34m[INFO] $message\033[0m" ;;
        *) echo "$message" ;;
    esac
}

check_tool() {
    local tool=$1
    if ! command -v $tool &> /dev/null; then
        echo_color "red" "Ferramenta '$tool' não encontrada. Instale antes de continuar."
        exit 1
    fi
    echo_color "green" "Ferramenta '$tool' encontrada"
}

cleanup() {
    echo_color "yellow" "Limpando diretórios temporários..."
    rm -rf "$WORK_DIR" 2>/dev/null || true
    mkdir -p "$WORK_DIR"
}

# =============================================================================
# VERIFICAÇÃO INICIAL
# =============================================================================

echo "=========================================="
echo "NVR APK Build Script v1.0"
echo "=========================================="
echo ""

# Verificar se a APK base existe
if [ ! -f "$BASE_APK" ]; then
    echo_color "red" "APK base não encontrada: $BASE_APK"
    echo "Baixe a MobileGlRENDER_signed5.apk do repositório:"
    echo "https://github.com/denotas235/MOBILEGLUEREBORN/raw/Brain/MobileGlRENDER_signed5.apk"
    exit 1
fi
echo_color "green" "APK base encontrada: $BASE_APK"

# Verificar ferramentas
check_tool "$APKTOOL"
check_tool "$ZIPALIGN"
check_tool "$APKSIGNER"

# =============================================================================
# PASSO 1: DECOMPILAR APK BASE
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 1: Decompilando APK base..."
echo "=========================================="

cleanup

$APKTOOL d "$BASE_APK" -o "$DECOMPILED_DIR" -f
if [ $? -ne 0 ]; then
    echo_color "red" "Falha ao decompilar a APK"
    exit 1
fi
echo_color "green" "APK decompilada em: $DECOMPILED_DIR"

# =============================================================================
# PASSO 2: COPIAR ARQUIVOS DA INTERFACE STEAMPUNK
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 2: Inserindo arquivos Steampunk..."
echo "=========================================="

# Criar backup da estrutura original
cp -r "$DECOMPILED_DIR" "$DECOMPILED_DIR.backup"

# Copiar AndroidManifest.xml
cp "$PROJECT_DIR/AndroidManifest.xml" "$DECOMPILED_DIR/AndroidManifest.xml"
echo_color "green" "AndroidManifest.xml copiado"

# Copiar mobileglues.conf
mkdir -p "$DECOMPILED_DIR/assets"
cp "$PROJECT_DIR/mobileglues.conf" "$DECOMPILED_DIR/assets/mobileglues.conf"
echo_color "green" "mobileglues.conf copiado"

# Copiar NexusVkBridge.smali
mkdir -p "$DECOMPILED_DIR/smali/com/nexus/vulkan"
cp "$PROJECT_DIR/NexusVkBridge.smali" "$DECOMPILED_DIR/smali/com/nexus/vulkan/NexusVkBridge.smali"
echo_color "green" "NexusVkBridge.smali copiado"

# Copiar SettingsActivity.smali
cp "$PROJECT_DIR/smali/com/nexus/vulkan/SettingsActivity.smali" "$DECOMPILED_DIR/smali/com/nexus/vulkan/SettingsActivity.smali"
echo_color "green" "SettingsActivity.smali copiado"

# Copiar todos os arquivos de layout
mkdir -p "$DECOMPILED_DIR/res/layout"
cp "$PROJECT_DIR/res/layout/activity_settings.xml" "$DECOMPILED_DIR/res/layout/activity_settings.xml"
echo_color "green" "activity_settings.xml copiado"

# Copiar todos os drawables XML
mkdir -p "$DECOMPILED_DIR/res/drawable"
cp "$PROJECT_DIR/res/drawable/"*.xml "$DECOMPILED_DIR/res/drawable/"
echo_color "green" "Drawables XML copiados"

# Copiar colors.xml, strings.xml, styles.xml
mkdir -p "$DECOMPILED_DIR/res/values"
cp "$PROJECT_DIR/res/values/colors.xml" "$DECOMPILED_DIR/res/values/colors.xml"
cp "$PROJECT_DIR/res/values/strings.xml" "$DECOMPILED_DIR/res/values/strings.xml"
cp "$PROJECT_DIR/res/values/styles.xml" "$DECOMPILED_DIR/res/values/styles.xml"
echo_color "green" "Resources (colors, strings, styles) copiados"

# Criar slot para libmobileglues.so
mkdir -p "$DECOMPILED_DIR/lib/arm64-v8a"
touch "$DECOMPILED_DIR/lib/arm64-v8a/.gitkeep"
echo "# Slot para libmobileglues.so" > "$DECOMPILED_DIR/lib/arm64-v8a/.gitkeep"
echo_color "yellow" "Slot para libmobileglues.so criado (vazio)"

# =============================================================================
# PASSO 3: PROCESSAR PNGs (OPcional - se as imagens já estiverem prontas)
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 3: Processando PNGs (opcional)..."
echo "=========================================="

# Verificar se as PNGs existem
ICON_PNG="$PROJECT_DIR/1782380310184.png"
UI_PNG="$PROJECT_DIR/1782380458552.png"

if [ -f "$ICON_PNG" ] && [ -f "$UI_PNG" ]; then
    echo_color "blue" "PNGs encontradas. Processando..."
    
    # Criar diretórios para diferentes densidades
    for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
        mkdir -p "$DECOMPILED_DIR/res/drawable-$density"
    done
    
    # Processar ícone (1782380310184.png)
    # mdpi: 48x48, hdpi: 72x72, xhdpi: 96x96, xxhdpi: 144x144, xxxhdpi: 192x192
    if command -v convert &> /dev/null; then
        convert "$ICON_PNG" -resize 48x48 "$DECOMPILED_DIR/res/drawable-mdpi/ic_launcher.png"
        convert "$ICON_PNG" -resize 72x72 "$DECOMPILED_DIR/res/drawable-hdpi/ic_launcher.png"
        convert "$ICON_PNG" -resize 96x96 "$DECOMPILED_DIR/res/drawable-xhdpi/ic_launcher.png"
        convert "$ICON_PNG" -resize 144x144 "$DECOMPILED_DIR/res/drawable-xxhdpi/ic_launcher.png"
        convert "$ICON_PNG" -resize 192x192 "$DECOMPILED_DIR/res/drawable-xxxhdpi/ic_launcher.png"
        
        # ic_launcher_round
        for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
            cp "$DECOMPILED_DIR/res/drawable-$density/ic_launcher.png" "$DECOMPILED_DIR/res/drawable-$density/ic_launcher_round.png"
        done
        
        echo_color "green" "Ícone processado para todas as densidades"
    else
        echo_color "yellow" "ImageMagick (convert) não encontrado. Copie manualmente as PNGs para:"
        echo "  - res/drawable-mdpi/ic_launcher.png (48x48)"
        echo "  - res/drawable-hdpi/ic_launcher.png (72x72)"
        echo "  - res/drawable-xhdpi/ic_launcher.png (96x96)"
        echo "  - res/drawable-xxhdpi/ic_launcher.png (144x144)"
        echo "  - res/drawable-xxxhdpi/ic_launcher.png (192x192)"
    fi
    
    # Processar UI PNG (1782380458552.png) - extrair elementos
    # Esta parte requer recorte manual ou um script mais complexo
    echo_color "yellow" "PNG da interface requer recorte manual. Veja README_NVR.md para detalhes."
else
    echo_color "yellow" "PNGs não encontradas. Baixe de:"
    echo "  - Ícone: https://github.com/denotas235/MOBILEGLUEREBORN/raw/Brain/1782380310184.png"
    echo "  - UI: https://github.com/denotas235/MOBILEGLUEREBORN/raw/Brain/1782380458552.png"
fi

# =============================================================================
# PASSO 4: RECOMPILAR APK
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 4: Recompilando APK..."
echo "=========================================="

mkdir -p "$BUILD_DIR"

$APKTOOL b "$DECOMPILED_DIR" -o "$BUILD_DIR/NVR_unsigned.apk"
if [ $? -ne 0 ]; then
    echo_color "red" "Falha ao recompilar a APK"
    exit 1
fi
echo_color "green" "APK recompilada: $BUILD_DIR/NVR_unsigned.apk"

# =============================================================================
# PASSO 5: ALINHAR COM ZIPALIGN
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 5: Alinhando APK..."
echo "=========================================="

$ZIPALIGN -v 4 "$BUILD_DIR/NVR_unsigned.apk" "$BUILD_DIR/NVR_aligned.apk"
if [ $? -ne 0 ]; then
    echo_color "red" "Falha ao alinhar a APK"
    exit 1
fi
echo_color "green" "APK alinhada: $BUILD_DIR/NVR_aligned.apk"

# =============================================================================
# PASSO 6: CRIAR KEYSTORE (se não existir)
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 6: Criando keystore..."
echo "=========================================="

if [ ! -f "$KEYSTORE" ]; then
    keytool -genkey -v -keystore "$KEYSTORE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEYSTORE_PASS" \
        -dname "CN=Denocorp, OU=Nexus, O=Denocorp, L=Maputo, ST=Maputo, C=MZ"
    if [ $? -ne 0 ]; then
        echo_color "red" "Falha ao criar keystore"
        exit 1
    fi
    echo_color "green" "Keystore criado: $KEYSTORE"
else
    echo_color "green" "Keystore já existe: $KEYSTORE"
fi

# =============================================================================
# PASSO 7: ASSINAR APK
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 7: Assinando APK..."
echo "=========================================="

$APKSIGNER sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:"$KEYSTORE_PASS" \
    --ks-key-alias "$KEY_ALIAS" \
    --key-pass pass:"$KEYSTORE_PASS" \
    --out "$FINAL_APK" \
    "$BUILD_DIR/NVR_aligned.apk"

if [ $? -ne 0 ]; then
    echo_color "red" "Falha ao assinar a APK"
    exit 1
fi
echo_color "green" "APK assinada: $FINAL_APK"

# =============================================================================
# PASSO 8: VERIFICAR APK FINAL
# =============================================================================

echo ""
echo "=========================================="
echo "PASSO 8: Verificando APK final..."
echo "=========================================="

if [ -f "$FINAL_APK" ]; then
    FILE_SIZE=$(stat -c%s "$FINAL_APK")
    echo_color "green" "APK final criada: $FINAL_APK ($FILE_SIZE bytes)"
    
    # Verificar assinatura
    $APKSIGNER verify --print-certs "$FINAL_APK"
    if [ $? -eq 0 ]; then
        echo_color "green" "APK está corretamente assinada"
    else
        echo_color "yellow" "APK pode não estar assinada corretamente"
    fi
else
    echo_color "red" "APK final não encontrada: $FINAL_APK"
    exit 1
fi

# =============================================================================
# FINALIZAÇÃO
# =============================================================================

echo ""
echo "=========================================="
echo "✅ BUILD CONCLUÍDO!"
echo "=========================================="
echo ""
echo "APK final: $FINAL_APK"
echo ""
echo "Próximos passos:"
echo "1. Copie a libmobileglues.so para: $DECOMPILED_DIR/lib/arm64-v8a/"
echo "2. Reexecute este script para reconstruir com a lib"
echo "3. Ou insira manualmente a lib na APK final"
echo ""
echo "Para instalar no dispositivo:"
echo "  adb install $FINAL_APK"
echo ""

# Limpeza opcional
# read -p "Deseja limpar os arquivos temporários? (s/n): " -n 1 -r
# echo
# if [[ $REPLY =~ ^[Ss]$ ]]; then
#     rm -rf "$WORK_DIR"
#     echo_color "green" "Arquivos temporários removidos"
# fi
