# Arquivo IV — Build de Release (APK assinada)

Guia local para desenvolvedores. As credenciais de assinatura estão em `keystore.properties` (gitignored) e **nunca** devem ser commitadas.

## Pré-requisitos
- Android Studio Hedgehog+ (2023.1+)
- JDK 17 (Eclipse Adoptium)
- Android SDK 34

## Configuração de Signing (keystore.properties)

Crie um ficheiro `keystore.properties` na raiz do projeto (já incluído no `.gitignore`):

```properties
RELEASE_STORE_FILE=arquivoiv-release.jks
RELEASE_STORE_PASSWORD=********
RELEASE_KEY_ALIAS=********
RELEASE_KEY_PASSWORD=********
```

> **IMPORTANTE:** O ficheiro `arquivoiv-release.jks` também está no `.gitignore` e **nunca** deve ser commitado.  
> As passwords reais estão apenas em `keystore.properties` no seu disco local.

## Passos para Gerar a APK Assinada

### Opção 1: Android Studio (Recomendado)

1. Abrir o projeto: `File → Open → C:\Users\hah_c\Desktop\ArquivoIV`
2. Aguardar a sincronização do Gradle (Gradle Sync)
3. Menu: `Build → Generate Signed Bundle / APK...`
4. Selecionar **APK**
5. Em **Key store path**: selecionar `arquivoiv-release.jks` (na raiz do projeto)
6. Preencher com as credenciais de `keystore.properties`
7. Selecionar **release** como build variant
8. Marcar **V1 (Jar Signature)** e **V2 (Full APK Signature)**
9. Clicar **Finish**

A APK será gerada em:
```
app/release/app-release.apk
```

### Opção 2: Linha de Comandos

```batch
set ANDROID_HOME=C:\Users\hah_c\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
gradlew assembleRelease
```

## Configuração de Assinatura

- **Keystore**: `arquivoiv-release.jks` (raiz do projeto — NÃO cometer)
- **Alias**: em `keystore.properties`
- **Password**: em `keystore.properties`
- **Validade**: 10.000 dias (≈ 27 anos)
- **Algoritmo**: RSA 2048 + SHA256withRSA

## Build Config (app/build.gradle.kts)

- `versionCode = 1`
- `versionName = "1.0.0"`
- `minSdk = 26` (Android 8.0)
- `targetSdk = 34` (Android 14)
- `isMinifyEnabled = true` (R8)
- `isShrinkResources = true`

## Publicação no GitHub Releases

1. Gerar a APK assinada (`app/release/app-release.apk`)
2. Criar uma release no GitHub: `https://github.com/halexys-dotcom/Arquivo_IV/releases`
3. Tag semântica: `v1.0.0`
4. Fazer upload da APK como asset
5. A app verificará automaticamente a nova versão ao arrancar

## ⚠️ Importante

**GUARDE a keystore `arquivoiv-release.jks` em local seguro e NUNCA a perca.**
Sem esta keystore, não será possível publicar atualizações da aplicação.
Guarde também as passwords num gestor de passwords seguro.