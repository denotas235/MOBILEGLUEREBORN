# 📦 **NVR APK - Instruções de Build Completo**

---

## 🎯 **Objetivo**

Criar a **NVR.apk** com:
- **Interface Steampunk** (baseada nas PNGs `1782380310184.png` e `1782380458552.png`)
- **Integração com libmobileglues.so** (slot vazio para você inserir manualmente)
- **Fluxo do ANGLE preservado** (sem alterações no fluxo existente)
- **Todas as configurações mapeadas** (20+ toggles na UI)
- **Assinatura debug** (você pode substituir por sua própria chave)

---

## 📂 **Estrutura da Branch `nvr-steampunk-interface`**

```
MOBILEGLUEREBORN/
├── AndroidManifest.xml              # Manifest com Vulkan support
├── mobileglues.conf                 # Configurações padrão (Mali-G52 otimizado)
├── NexusVkBridge.smali             # Interface JNI para libmobileglues.so
├── README_NVR.md                    # Documentação completa
├── BUILD_INSTRUCTIONS.md            # Este arquivo
├── lib/
│   └── arm64-v8a/
│       └── .gitkeep                 # SLOT PARA libmobileglues.so
├── res/
│   ├── drawable/
│   │   ├── bg_steampunk_texture.xml
│   │   ├── bg_bronze_frame.xml
│   │   ├── bg_metallic_strip.xml
│   │   ├── ic_bronze_dial.xml
│   │   ├── ic_gear_arrow_indicator.xml
│   │   ├── ic_lever_switch_on.xml
│   │   ├── ic_lever_switch_off.xml
│   │   ├── ic_nexus_v_logo.xml
│   │   ├── ic_launcher.xml
│   │   ├── ic_launcher_background.xml
│   │   ├── ic_launcher_foreground.xml
│   │   ├── btn_arrow_back.xml
│   │   ├── btn_home.xml
│   │   ├── btn_menu.xml
│   │   ├── custom_seekbar.xml
│   │   └── custom_seekbar_thumb.xml
│   ├── layout/
│   │   └── activity_settings.xml      # Layout principal Steampunk
│   └── values/
│       ├── colors.xml               # Paleta Steampunk
│       ├── strings.xml              # Todos os textos da UI
│       └── styles.xml               # Tema Steampunk
├── smali/
│   └── com/
│       └── nexus/
│           └── vulkan/
│               ├── NexusVkBridge.smali     # JNI Interface
│               └── SettingsActivity.smali  # Activity principal
└── scripts/
    ├── build_nvr_apk.sh             # Script completo de build
    └── sign_apk.sh                  # Script para assinar APK
```

---

## 🛠️ **Requisitos**

### **Ferramentas Necessárias**
| Ferramenta | Descrição | Instalação |
|------------|-----------|------------|
| **apktool** | Decompila/Recompila APKs | `sudo apt install apktool` |
| **zipalign** | Alinha APKs | Vem com Android SDK |
| **apksigner** | Assina APKs | Vem com Android SDK |
| **ImageMagick** | Processa PNGs | `sudo apt install imagemagick` |
| **Java JDK 8+** | Requerido pelo apktool | `sudo apt install openjdk-11-jdk` |

### **Verificação de Ferramentas**
```bash
# Verificar se todas as ferramentas estão instaladas
apktool --version
zipalign -v
apksigner --version
convert --version
java -version
```

---

## 🚀 **Passo a Passo para Build**

### **Passo 1: Clonar a Branch**
```bash
cd ~
git clone -b nvr-steampunk-interface https://github.com/denotas235/MOBILEGLUEREBORN.git
cd MOBILEGLUEREBORN
```

---

### **Passo 2: Baixar a APK Base**
```bash
# Baixar a APK original do repositório
wget https://github.com/denotas235/MOBILEGLUEREBORN/raw/Brain/MobileGlRENDER_signed5.apk
```

---

### **Passo 3: Baixar as PNGs**
```bash
# Baixar as PNGs para a interface
wget https://github.com/denotas235/MOBILEGLUEREBORN/raw/Brain/1782380310184.png
wget https://github.com/denotas235/MOBILEGLUEREBORN/raw/Brain/1782380458552.png
```

---

### **Passo 4: Processar as PNGs (Opcional)**

#### **Opção A: Processamento Automático (Requer ImageMagick)**
```bash
# Criar ícone para todas as densidades
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    size=""
    case $density in
        mdpi) size="48x48" ;;
        hdpi) size="72x72" ;;
        xhdpi) size="96x96" ;;
        xxhdpi) size="144x144" ;;
        xxxhdpi) size="192x192" ;;
    esac
    convert 1782380310184.png -resize $size res/drawable-$density/ic_launcher.png
    cp res/drawable-$density/ic_launcher.png res/drawable-$density/ic_launcher_round.png
done
```

#### **Opção B: Processamento Manual**
1. Abrir `1782380310184.png` (ícone) em **GIMP/Photoshop**
2. Recortar e redimensionar para:
   - **mdpi**: 48x48 → `res/drawable-mdpi/ic_launcher.png`
   - **hdpi**: 72x72 → `res/drawable-hdpi/ic_launcher.png`
   - **xhdpi**: 96x96 → `res/drawable-xhdpi/ic_launcher.png`
   - **xxhdpi**: 144x144 → `res/drawable-xxhdpi/ic_launcher.png`
   - **xxxhdpi**: 192x192 → `res/drawable-xxxhdpi/ic_launcher.png`
3. Copiar cada arquivo para o respectivo diretório
4. Criar `ic_launcher_round.png` (mesmo que `ic_launcher.png`)

#### **Opção C: Usar PNGs Diretamente**
Se você não processar as PNGs, a APK ainda será criada, mas **sem os ícones e imagens Steampunk**. A interface XML será aplicada, mas com assets padrão.

---

### **Passo 5: Executar o Script de Build**

#### **Opção A: Build Completo (Recomendado)**
```bash
# Dar permissão de execução
chmod +x scripts/build_nvr_apk.sh

# Executar o script
./scripts/build_nvr_apk.sh
```

O script fará:
1. Decompilar a APK base
2. Inserir todos os arquivos da interface Steampunk
3. Processar as PNGs (se ImageMagick estiver instalado)
4. Recompilar a APK
5. Alinhar com zipalign
6. Criar keystore debug
7. Assinar a APK
8. Gerar **NVR.apk**

#### **Opção B: Build Manual**

Se preferir fazer manualmente:

```bash
# 1. Decompilar APK base
apktool d MobileGlRENDER_signed5.apk -o decompiled_apk

# 2. Copiar arquivos da interface Steampunk
cp AndroidManifest.xml decompiled_apk/
cp mobileglues.conf decompiled_apk/assets/
cp NexusVkBridge.smali decompiled_apk/smali/com/nexus/vulkan/
cp SettingsActivity.smali decompiled_apk/smali/com/nexus/vulkan/
cp -r res/* decompiled_apk/res/

# 3. Criar slot para lib
mkdir -p decompiled_apk/lib/arm64-v8a
touch decompiled_apk/lib/arm64-v8a/.gitkeep

# 4. Recompilar
apktool b decompiled_apk -o NVR_unsigned.apk

# 5. Alinhar
zipalign -v 4 NVR_unsigned.apk NVR_aligned.apk

# 6. Assinar
./scripts/sign_apk.sh NVR_aligned.apk NVR.apk
```

---

### **Passo 6: Inserir a libmobileglues.so**

Após a APK ser criada, você precisa inserir a **libmobileglues.so** manualmente:

#### **Opção A: Antes de Recompilar**
1. Colocar a lib em: `decompiled_apk/lib/arm64-v8a/libmobileglues.so`
2. Recompilar com `apktool b`

#### **Opção B: Após Recompilar (Recomendado)**
```bash
# 1. Extrair a APK
unzip NVR.apk -d nvr_extracted

# 2. Colocar a lib
mkdir -p nvr_extracted/lib/arm64-v8a
cp libmobileglues.so nvr_extracted/lib/arm64-v8a/

# 3. Reempacotar
cd nvr_extracted
zip -r ../NVR_with_lib.apk *

# 4. Assinar novamente
./scripts/sign_apk.sh ../NVR_with_lib.apk ../NVR_final.apk
```

---

### **Passo 7: Testar a APK**

#### **Instalar no Dispositivo**
```bash
# Via ADB
adb install NVR.apk

# Ou copiar para o dispositivo e instalar manualmente
```

#### **Verificar no Zalith Launcher**
1. Abrir o Zalith Launcher
2. Procurar por **NVR** na lista de apps
3. Lançar o app
4. Verificar se a **interface Steampunk** aparece
5. Testar todas as configurações

---

## 📊 **Checklist de Verificação**

- [ ] **Ferramentas instaladas** (apktool, zipalign, apksigner, ImageMagick, Java)
- [ ] **APK base baixada** (MobileGlRENDER_signed5.apk)
- [ ] **PNGs baixadas** (1782380310184.png, 1782380458552.png)
- [ ] **PNGs processadas** (opcional, para ícones e imagens)
- [ ] **Script de build executado** (`./scripts/build_nvr_apk.sh`)
- [ ] **NVR.apk gerada**
- [ ] **libmobileglues.so inserida** (manual)
- [ ] **APK testada no dispositivo**

---

## ⚠️ **Problemas Comuns e Soluções**

### **1. apktool não encontrado**
**Solução:**
```bash
# Ubuntu/Debian
sudo apt install apktool

# Arch Linux
sudo pacman -S apktool

# macOS
brew install apktool
```

### **2. zipalign não encontrado**
**Solução:**
- Baixar o Android SDK Command-line Tools
- Adicionar ao PATH:
  ```bash
  export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
  ```

### **3. apksigner não encontrado**
**Solução:**
- Baixar o Android SDK
- O apksigner está em: `$ANDROID_HOME/build-tools/<versão>/apksigner`
- Ou instalar via:
  ```bash
  sudo apt install apksigner  # Ubuntu/Debian
  ```

### **4. Erro ao decompilar: "Invalid APK file"**
**Solução:**
- Verificar se o download da APK foi corrompido
- Baixar novamente:
  ```bash
  wget https://github.com/denotas235/MOBILEGLUEREBORN/raw/Brain/MobileGlRENDER_signed5.apk
  ```

### **5. Erro ao recompilar: "Invalid resource directory name"**
**Solução:**
- Verificar se os diretórios `res/drawable-*dpi/` existem
- Remover arquivos com nomes inválidos

### **6. APK não instala no dispositivo**
**Solução:**
- Verificar se a APK está assinada:
  ```bash
  apksigner verify --print-certs NVR.apk
  ```
- Verificar se o dispositivo permite instalação de fontes desconhecidas

### **7. Interface Steampunk não aparece**
**Solução:**
- Verificar se `activity_settings.xml` está no lugar certo
- Verificar se o `AndroidManifest.xml` aponta para a Activity correta
- Verificar logs com:
  ```bash
  adb logcat | grep -i "nexus\|vulkan\|steampunk"
  ```

---

## 📝 **Personalização**

### **1. Mudar o Nome da APK**
Edite `AndroidManifest.xml`:
```xml
<application android:label="NVR | Nexus Vulkan Render" ...>
```

### **2. Mudar o Ícone**
Substitua os arquivos em `res/drawable-*dpi/ic_launcher.png`

### **3. Mudar as Cores do Tema**
Edite `res/values/colors.xml`:
```xml
<color name="steampunk_bronze">#A98467</color>
<color name="steampunk_gold">#E5C298</color>
<color name="steampunk_blue">#5CE1E6</color>
```

### **4. Adicionar Novas Configurações**
1. Adicione a configuração em `mobileglues.conf`
2. Adicione o toggle/spinner em `activity_settings.xml`
3. Adicione o método em `NexusVkBridge.smali`
4. Adicione a string em `res/values/strings.xml`

---

## 📊 **Configurações Recomendadas para TECNO KH7**

### **Para Melhor Performance:**
| Configuração | Valor Recomendado | Motivo |
|--------------|-------------------|--------|
| ANGLE Mode | **Disabled** | Mali-G52 tem bom suporte nativo |
| Custom GL Version | **4.6** | Melhor compatibilidade com shaders |
| Max GLSL Cache Size | **64 MB** | Equilíbrio entre performance e storage |
| MultiDraw Mode | **Prefer Base Vertex** | Melhor suporte no Mali-G52 |
| Framebuffer Fetch | **Enabled** | Otimização específica para Mali |
| Phase 2 Lighting | **Enabled** | Melhora performance em cenas complexas |
| Anisotropic Filtering | **8x** | 4x causava corrupção de cores |
| ARB Compute Shader | **Enabled** | Mali-G52 suporta |
| Direct State Access | **Enabled** | Reduz binds redundantes |

### **Para Debug:**
| Configuração | Valor Recomendado |
|--------------|-------------------|
| Debug Log Level | **Off** (ou **Error**) |
| Timer Query | **Disabled** |
| Hide MG Environment | **Disabled** |

---

## 🎯 **Resultado Esperado**

Após seguir todos os passos, você terá:

1. **NVR.apk** (assinada e pronta para instalar)
2. **Interface Steampunk** funcionando
3. **Todas as configurações** da libmobileglues.so acessíveis
4. **Fluxo do ANGLE preservado** (sem alterações)
5. **Slot vazio** em `lib/arm64-v8a/` para inserir a lib manualmente

---

## 🙏 **Suporte**

Se encontrar algum problema:

1. **Verifique os logs**:
   ```bash
   adb logcat | grep -i "nexus\|mobileglue\|error"
   ```

2. **Verifique a estrutura da APK**:
   ```bash
   unzip -l NVR.apk
   ```

3. **Verifique a assinatura**:
   ```bash
   apksigner verify --print-certs NVR.apk
   ```

4. **Entre em contato** com detalhes do erro

---

## 📄 **Licença**

Este projeto é licenciado sob **LGPL-2.1** (mesma licença da libmobileglues).

---

**Boa sorte!** 🚀

Após concluir o build, você terá uma **NVR.apk** funcional com interface Steampunk e todas as configurações da libmobileglues.so acessíveis. O **slot para a lib está vazio** conforme solicitado, para você inserir manualmente depois.
