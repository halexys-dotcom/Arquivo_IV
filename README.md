# Arquivo IV

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-orange.svg)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.06-blue.svg)](https://developer.android.com/compose)

**Arquivo digital de faturas de fornecedores.** Aplicação Android para arquivamento, gestão e controlo de faturas com retenção legal de 5 anos, operação 100% offline.

---

## 📌 Visão Geral

O **Arquivo IV** é uma solução local desenhada para simplificar o fluxo de entrada e consulta de faturas. A aplicação elimina dependências de serviços na cloud para a gestão documental: todas as bases de dados, índices e ficheiros permanecem exclusivamente no armazenamento do dispositivo.

* **Público-alvo:** Utilização individual / PME / Arquivo interno.
* **Idioma:** Português (pt-PT).
* **Conectividade:** 100% offline para operações de negócio (acesso à rede restrito à verificação de atualizações OTA via GitHub Releases).

---

## ✨ Funcionalidades

- 📄 **Registo de faturas** com anexo único (foto via câmara ou ficheiro PDF/imagem via SAF)
- 🔍 **OCR offline** (ML Kit) que autopreenche fornecedor, nº da fatura e data a partir do anexo
- 📋 **Pesquisa combinada** por fornecedor (texto parcial, case-insensitive) e intervalo de datas
- 📊 **Dashboard** com contagens e lista de fornecedores
- 📤 **Exportação em lote** para PDF e CSV
- 📤 **Partilha individual** de uma fatura em PDF (com anexo/imagem embutido)
- ⏰ **Notificações de retenção legal** aos 30 dias antes e ao atingir os 5 anos
- 💾 **Backups locais** em ZIP (BD + anexos + manifesto) — cria, exporta, restaura
- 🔄 **Atualizações OTA** via GitHub Releases

---

## 🛠️ Stack Tecnológico

| Componente | Tecnologia |
|---|---|
| **Linguagem** | Kotlin |
| **Interface** | Jetpack Compose + Material 3 |
| **Arquitetura** | MVVM + Clean Architecture |
| **Injeção de Dependências** | Hilt (Dagger) |
| **Persistência Local** | Room (SQLite) + Preferences DataStore |
| **Tarefas em Background** | WorkManager |
| **Carregamento de Imagens** | Coil |
| **Manipulação de PDF** | iText 7 + PdfRenderer (nativo) |
| **Processamento OCR** | ML Kit Text Recognition v2 (modelo bundled, offline) |
| **Navegação** | Navigation Compose |

### Identificadores da Aplicação
* **Package Name (Release):** `pt.haconnect.arquivoiv`
* **Package Name (Debug):** `pt.haconnect.arquivoiv.debug`
* **Nome da Base de Dados:** `arquivoiv_database`
* **SharedPreferences:** `arquivoiv_prefs`
* **Room Schema:** v8

---

## 🗄️ Estrutura de Dados (Room Schema)

### `faturas`
| Campo | Tipo | Descrição |
|---|---|---|
| `id` | INTEGER | Chave primária (Auto-increment) |
| `fornecedor` | TEXT | Nome do emitente (Obrigatório) |
| `numero_fatura` | TEXT | Identificador do documento (Obrigatório) |
| `data_emissao` | INTEGER | Timestamp da data de emissão |
| `caminho_anexo` | TEXT | Caminho local do anexo (1:1) |
| `data_insercao` | INTEGER | Timestamp de registo (base do cálculo dos 5 anos) |
| `notificado_30dias` | INTEGER | Flag anti-spam (0/1) |
| `notificado_5anos` | INTEGER | Flag anti-spam (0/1) |

### `fornecedores`
| Campo | Tipo | Descrição |
|---|---|---|
| `id` | INTEGER | Chave primária (Auto-increment) |
| `nome` | TEXT | Nome normalizado (Único, anti-duplicação) |
| `dataPrimeiroRegisto` | INTEGER | Timestamp do primeiro registo |

---

## 🚀 Compilação

### Pré-requisitos
- **JDK 17** (Eclipse Adoptium recomendado)
- **Android SDK 34**
- **Android Studio** Hedgehog (2023.1+) ou superior

### Passos
```bash
# Clone o repositório
git clone https://github.com/halexys-dotcom/Arquivo_IV.git
cd Arquivo_IV

# Build debug APK
./gradlew assembleDebug
```

O APK de debug estará em: `app/build/outputs/apk/debug/app-debug.apk`

> **Nota:** Para gerar a APK de release assinada, consulte [BUILD_RELEASE.md](BUILD_RELEASE.md).

---

## 📄 Licença

Arquivo IV — Arquivo digital de faturas  
Copyright (C) 2026 HAConnect

Este programa é software livre: pode redistribuir e/ou modificar
sob os termos da **GNU General Public License v3.0**.  
Consulte o ficheiro [LICENSE](LICENSE) para detalhes completos.

---

## 📱 Lançamentos

As releases assinadas são publicadas em [GitHub Releases](https://github.com/halexys-dotcom/Arquivo_IV/releases).  
A aplicação verifica automaticamente novas versões ao arrancar (OTA).

---

## ⚠️ Aviso Legal

A retenção legal de faturas de fornecedores em Portugal exige conservação por **5 anos** (Código do Processo Civil, art. 1.080). Verifique sempre com um técnico ou assessor legal se a sua necessidade de retenção se enquadra nesta aplicação.