#!/bin/bash

# =============================================================================
# Script para Assinar APK - NVR APK
# =============================================================================
# Este script assina uma APK com uma keystore debug.
# Uso: ./sign_apk.sh input.apk output.apk
# =============================================================================

set -e

# =============================================================================
# CONFIGURAÇÕES
# =============================================================================

# Diretório do script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Keystore (será criada se não existir)
KEYSTORE="$SCRIPT_DIR/debug.keystore"
KEYSTORE_PASS="123456"
KEY_ALIAS="debug"
KEY_PASS="$KEYSTORE_PASS"

# Ferramentas
ZIPALIGN="zipalign"
APKSIGNER="apksigner"

# =============================================================================
# FUNÇÕES
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

create_keystore() {
    echo_color "blue" "Criando keystore debug..."
    keytool -genkey -v -keystore "$KEYSTORE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEY_PASS" \
        -dname "CN=Denocorp, OU=Nexus, O=Denocorp, L=Maputo, ST=Maputo, C=MZ"
    
    if [ $? -eq 0 ]; then
        echo_color "green" "Keystore criada: $KEYSTORE"
    else
        echo_color "red" "Falha ao criar keystore"
        exit 1
    fi
}

# =============================================================================
# VERIFICAÇÃO DE ARGUMENTOS
# =============================================================================

if [ "$#" -ne 2 ]; then
    echo "Uso: $0 <input.apk> <output.apk>"
    echo "Exemplo: $0 NVR_unsigned.apk NVR_signed.apk"
    exit 1
fi

INPUT_APK="$1"
OUTPUT_APK="$2"

if [ ! -f "$INPUT_APK" ]; then
    echo_color "red" "APK de entrada não encontrada: $INPUT_APK"
    exit 1
fi

# =============================================================================
# PRINCIPAL
# =============================================================================

echo "=========================================="
echo "NVR APK Signing Script"
echo "=========================================="
echo ""

# Criar keystore se não existir
if [ ! -f "$KEYSTORE" ]; then
    create_keystore
fi

# Alinhar APK
echo_color "blue" "Alinhando APK..."
$ZIPALIGN -v 4 "$INPUT_APK" "${INPUT_APK%.apk}_aligned.apk"
if [ $? -ne 0 ]; then
    echo_color "red" "Falha ao alinhar APK"
    exit 1
fi
echo_color "green" "APK alinhada"

# Assinar APK
echo_color "blue" "Assinando APK..."
$APKSIGNER sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:"$KEYSTORE_PASS" \
    --ks-key-alias "$KEY_ALIAS" \
    --key-pass pass:"$KEY_PASS" \
    --out "$OUTPUT_APK" \
    "${INPUT_APK%.apk}_aligned.apk"

if [ $? -ne 0 ]; then
    echo_color "red" "Falha ao assinar APK"
    exit 1
fi
echo_color "green" "APK assinada: $OUTPUT_APK"

# Verificar assinatura
echo_color "blue" "Verificando assinatura..."
$APKSIGNER verify --print-certs "$OUTPUT_APK"
if [ $? -eq 0 ]; then
    echo_color "green" "✅ APK está corretamente assinada!"
else
    echo_color "yellow" "⚠️ APK pode não estar assinada corretamente"
fi

# Limpar arquivo temporário
rm -f "${INPUT_APK%.apk}_aligned.apk"

echo ""
echo "=========================================="
echo "Assinatura concluída!"
echo "APK final: $OUTPUT_APK"
echo "=========================================="
