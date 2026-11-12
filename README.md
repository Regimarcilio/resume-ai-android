<div align="center">

# 📄 Resume AI Builder

**AI-powered resume builder for Android — 100% offline-first, LGPD-compliant**

[English](#english) · [Português](#português)

[![CI](https://github.com/Regimarcilio/resume-ai-android/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/Regimarcilio/resume-ai-android/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Regimarcilio/resume-ai-android?label=release)](https://github.com/Regimarcilio/resume-ai-android/releases/latest)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/API-26%2B-3DDC84.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

</div>

---

<a name="english"></a>

# 🇬🇧 English

## What it is

An Android app that builds professional resumes with AI assistance, scores them against
ATS (Applicant Tracking Systems), and exports pixel-perfect PDFs — **all data stays on the device**.

It is designed for **sideload / enterprise distribution** (GitHub Releases, FDroid, MDM).
There is no Play Store dependency, no billing, and no server-side account.

## Highlights

| Feature | Description |
|---|---|
| 🧠 **AI Assist** | Improves bullet points and extracts keywords through a self-hosted gateway |
| 📊 **ATS Score** | 0-100 score with actionable, per-section suggestions |
| 📄 **PDF Export** | Pixel-perfect PDF generation, fully offline (PDFBox 3) |
| 🎨 **Templates** | Modern, Classic, Creative and ATS-optimized layouts |
| 🔒 **LGPD by design** | Granular per-purpose consent + full Art. 18 rights (export, anonymize, delete, revoke) |
| ✈️ **Offline-first** | Room + DataStore locally; the sync queue retries and dead-letters |
| 🔌 **Self-hosted AI** | OpenAI-compatible mock gateway with semantic cache, guardrail and sanitized traces |

## Screens

`Onboarding` (Welcome → Privacy policy → Consent → Template) → `Resume list` → `Editor` (Personal info → Sections) → `ATS`

A bottom bar do app principal leva a `Currículos`, `Templates` e `Configurações` (→ `Meus Dados`, Art. 18 LGPD).

## Requirements

| | |
|---|---|
| JDK | 21 (Temurin) |
| Android SDK | platform 34, build-tools 34.0.0 |
| Gradle | 8.7 (via the wrapper) |
| Device | Android 8.0 (API 26) or newer |

## Quick start

```bash
# 1. Point the build at your SDK
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. Build the debug APK
./gradlew apk

# 3. The APK is copied to ./dist/
ls -lh dist/
#  -> dist/resume-ai-1.0.0-debug.apk

# 4. Install it
adb install -r dist/resume-ai-1.0.0-debug.apk
```

> Every `assembleDebug` (local **or** CI) automatically drops a copy of the APK in
> `<project-root>/dist/`. The directory is git-ignored — it is a build output.

## Project layout

```
resume-ai-android/
├── app/                 # Composition root: Hilt graph, navigation, theme, launcher icon
├── core/
│   ├── domain/          # Models, use cases, repository contracts (pure Kotlin)
│   ├── data/            # Room, DataStore, repository implementations, offline sync
│   └── ai/              # Gateway client, semantic cache, guardrail, traces (pure JVM)
├── features/
│   ├── onboarding/      # Consent flow + privacy policy
│   ├── resume/          # List + editor
│   ├── ats/             # ATS scoring
│   ├── templates/       # Template gallery
│   ├── settings/        # Settings + My Data (LGPD Art. 18)
│   └── ai-assist/       # Inline AI components
├── docs/
│   ├── mock-gateway/    # FastAPI AI gateway (Docker + Redis)
│   ├── 04-ADRs/         # Architecture decision records (ADR-005: PDF library)
│   └── pdf-benchmark/   # PDF rendering benchmark
├── kalimaster-sast/     # Security static analysis module
└── dist/                # Generated APKs (git-ignored)
```

### Dependency rule

```
app  ──►  features/*  ──►  core:domain  ──►  core:ai
 │                              ▲
 └──────────────────────────────┘
        core:data  ──►  core:domain
```

`core:domain` never depends on Android framework classes, so business rules stay
portable and unit-testable.

## The EngAI pattern

Every AI call flows through a single, auditable pipeline:

```
UI → UseCase → AIGatewayClient
                  ├─ 1. Guardrail: sanitizeInput()      (blocks prompt injection)
                  ├─ 2. Semantic cache (Room-backed)     (skips the network)
                  ├─ 3. HTTP → self-hosted gateway
                  ├─ 4. Guardrail: validateOutput()      (blocks leaked data)
                  ├─ 5. Cache store
                  └─ 6. Trace logging (sanitized, no PII)
```

The gateway is OpenAI-compatible, so any local or hosted LLM can sit behind it.

### Running the mock gateway

```bash
cd docs/mock-gateway
docker compose up -d
curl http://localhost:8081/health
```

The Android emulator reaches the host at `http://10.0.2.2:8081`.
Without the gateway running, AI features return a failure result — the rest of the app keeps working.

## Build commands

| Command | What it does |
|---|---|
| `./gradlew apk` | Builds the debug APK and copies it to `dist/` |
| `./gradlew :app:assembleDebug` | Same build (the copy is wired via `finalizedBy`) |
| `./gradlew :app:assembleRelease` | Release build (R8/minify — **not yet validated**, see below) |
| `./gradlew clean` | Removes all build outputs |
| `./gradlew bumpVersion -PversionName=1.1.0 -PversionCode=2` | Bumps the app version |

## CI/CD

`.github/workflows/ci.yml` runs on every push to `develop`/`main` and on pull requests:

1. JDK 21 + Gradle 8.7
2. `./gradlew :app:assembleDebug`
3. Copies the APK to `dist/`
4. Uploads the `debug-apk` artifact

Releases are published as GitHub Releases with the APK attached.

## Security & privacy (LGPD)

- **Consent** — per-purpose, revocable, recorded with version + timestamp + policy hash
- **Art. 18 rights** — export (JSON), anonymize, delete all, revoke consent
- **Data minimisation** — only what you type is processed
- **Local storage** — Room + DataStore; backup excluded
- **AI privacy** — input sanitization, output validation, sanitized traces, semantic cache
- **PDF export** — generated on-device, never uploaded

## Known limitations

1. **The AI features require the mock gateway** (Docker + Redis). Without it, `AIGatewayClient` returns a failure.
2. **AI traces are not persisted** — `TraceLogger` is injected into the use cases but never called (wiring pending).
3. **Unit tests do not run in CI** — `core:ai` has a test referencing a class that moved to the `app` module.
4. **SQLCipher was removed** — Room currently uses the default (unencrypted) factory, while the privacy policy states AES-256. This must be reconciled before any wide distribution.
5. **The release build with R8/minify is not validated** — published artifacts are debug builds.
6. The privacy policy still contains placeholders (`[Empresa]`, `dpo@empresa.com`).
7. **Some actions are still placeholders**: duplicating a resume, editing a section item, the AI-assist button in the editor, and PDF export.

## Roadmap

- [ ] Encryption at rest (SQLCipher / encrypted DataStore) to match the policy
- [ ] Release keystore + validated R8 rules
- [ ] Wire the trace logger into the AI use cases
- [ ] Navigation entry point for the ATS screen
- [ ] Fix and enable the unit test suite in CI
- [ ] Replace the privacy-policy placeholders with real company data

## License

MIT — see [LICENSE](LICENSE).

---

<a name="português"></a>

# 🇧🇷 Português

## O que é

Um app Android que monta currículos profissionais com apoio de IA, calcula o **score ATS**
(Applicant Tracking Systems) e exporta PDFs perfeitos — **tudo offline, no próprio aparelho**.

Feito para **distribuição via sideload / enterprise** (GitHub Releases, FDroid, MDM).
Sem dependência da Play Store, sem cobrança e sem conta no servidor.

## Destaques

| Recurso | Descrição |
|---|---|
| 🧠 **IA Assist** | Melhora bullet points e extrai keywords via gateway auto-hospedado |
| 📊 **ATS Score** | Score 0-100 com sugestões acionáveis por seção |
| 📄 **Export PDF** | Geração de PDF pixel-perfect, 100% offline (PDFBox 3) |
| 🎨 **Templates** | Layouts Moderno, Clássico, Criativo e otimizado para ATS |
| 🔒 **LGPD por design** | Consentimento granular por finalidade + todos os direitos do Art. 18 (exportar, anonimizar, excluir, revogar) |
| ✈️ **Offline-first** | Room + DataStore localmente; fila de sincronização com retry e dead-letter |
| 🔌 **IA auto-hospedada** | Gateway mock compatível com OpenAI, com cache semântico, guardrail e traces sanitizados |

## Telas

`Onboarding` (Welcome → Política → Consentimento → Template) → `Lista de currículos` → `Editor` (Dados pessoais → Seções) → `ATS`

A bottom bar do app principal leva a `Currículos`, `Templates` e `Configurações` (→ `Meus Dados`, Art. 18 LGPD).

## Requisitos

| | |
|---|---|
| JDK | 21 (Temurin) |
| Android SDK | platform 34, build-tools 34.0.0 |
| Gradle | 8.7 (via wrapper) |
| Aparelho | Android 8.0 (API 26) ou superior |

## Início rápido

```bash
# 1. Apontar o build para o seu SDK
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. Gerar o APK debug
./gradlew apk

# 3. O APK é copiado para ./dist/
ls -lh dist/
#  -> dist/resume-ai-1.0.0-debug.apk

# 4. Instalar
adb install -r dist/resume-ai-1.0.0-debug.apk
```

> Todo `assembleDebug` — **local ou no CI** — deixa automaticamente uma cópia do APK em
> `<project-root>/dist/`. O diretório está no `.gitignore`, pois é artefato de build.

## Estrutura do projeto

```
resume-ai-android/
├── app/                 # Raiz de composição: grafo Hilt, navegação, tema, ícone do launcher
├── core/
│   ├── domain/          # Modelos, use cases, contratos de repositório (Kotlin puro)
│   ├── data/            # Room, DataStore, implementações de repositório, sync offline
│   └── ai/              # Cliente do gateway, cache semântico, guardrail, traces (JVM puro)
├── features/
│   ├── onboarding/      # Fluxo de consentimento + política de privacidade
│   ├── resume/          # Lista + editor
│   ├── ats/             # Score ATS
│   ├── templates/       # Galeria de templates
│   ├── settings/        # Configurações + Meus Dados (LGPD Art. 18)
│   └── ai-assist/       # Componentes de IA inline
├── docs/
│   ├── mock-gateway/    # Gateway de IA em FastAPI (Docker + Redis)
│   ├── 04-ADRs/         # Architecture Decision Records
│   └── pdf-benchmark/   # Benchmark de renderização de PDF
├── kalimaster-sast/     # Módulo de análise estática de segurança
└── dist/                # APKs gerados (ignorado pelo git)
```

### Regra de dependências

```
app  ──►  features/*  ──►  core:domain  ──►  core:ai
 │                              ▲
 └──────────────────────────────┘
        core:data  ──►  core:domain
```

`core:domain` nunca depende de classes do framework Android, mantendo as regras de
negócio portáteis e testáveis em unidade.

## O padrão EngAI

Toda chamada de IA passa por um único pipeline auditável:

```
UI → UseCase → AIGatewayClient
                  ├─ 1. Guardrail: sanitizeInput()      (bloqueia prompt injection)
                  ├─ 2. Cache semântico (Room)           (evita a ida à rede)
                  ├─ 3. HTTP → gateway auto-hospedado
                  ├─ 4. Guardrail: validateOutput()      (bloqueia vazamento de dados)
                  ├─ 5. Cache store
                  └─ 6. Trace logging (sanitizado, sem PII)
```

O gateway é compatível com OpenAI, então qualquer LLM local ou hospedado pode ficar
atrás dele.

### Subindo o mock gateway

```bash
cd docs/mock-gateway
docker compose up -d
curl http://localhost:8081/health
```

O emulador Android alcança o host em `http://10.0.2.2:8081`.
Sem o gateway no ar, as features de IA retornam falha — o resto do app continua funcionando.

## Comandos de build

| Comando | O que faz |
|---|---|
| `./gradlew apk` | Gera o APK debug e copia para `dist/` |
| `./gradlew :app:assembleDebug` | Mesmo build (a cópia é wired via `finalizedBy`) |
| `./gradlew :app:assembleRelease` | Build release (R8/minify — **ainda não validado**) |
| `./gradlew clean` | Remove todos os artefatos de build |
| `./gradlew bumpVersion -PversionName=1.1.0 -PversionCode=2` | Sobe a versão do app |

## CI/CD

`.github/workflows/ci.yml` roda a cada push em `develop`/`main` e em pull requests:

1. JDK 21 + Gradle 8.7
2. `./gradlew :app:assembleDebug`
3. Copia o APK para `dist/`
4. Publica o artefato `debug-apk`

Os releases são publicados como GitHub Releases com o APK anexado.

## Segurança e privacidade (LGPD)

- **Consentimento** — por finalidade, revogável, gravado com versão + timestamp + hash da política
- **Direitos do Art. 18** — exportar (JSON), anonimizar, excluir tudo, revogar consentimento
- **Minimização** — só é processado o que você digita
- **Armazenamento local** — Room + DataStore; backup excluído
- **Privacidade da IA** — sanitização de entrada, validação de saída, traces sanitizados, cache semântico
- **Export PDF** — gerado no aparelho, nunca enviado

## Limitações conhecidas

1. **As features de IA exigem o mock gateway** (Docker + Redis). Sem ele, o `AIGatewayClient` retorna falha.
2. **Traces de IA não são persistidos** — o `TraceLogger` é injetado nos use cases mas nunca é chamado (falta o wiring).
3. **Os testes unitários não rodam no CI** — o `core:ai` tem um teste referenciando uma classe que foi movida para o módulo `app`.
4. **O SQLCipher foi removido** — o Room usa o factory padrão (sem criptografia), mas a política de privacidade declara AES-256. Precisa ser reconciliado antes de qualquer distribuição ampla.
5. **O build release com R8/minify não foi validado** — os artefatos publicados são debug.
6. A política de privacidade ainda contém placeholders (`[Empresa]`, `dpo@empresa.com`).
7. **Algumas ações ainda são placeholders**: duplicar currículo, editar item de seção, o botão de IA Assist no editor e a exportação de PDF.

## Roadmap

- [ ] Criptografia em repouso (SQLCipher / DataStore cifrado) para alinhar com a política
- [ ] Keystore de release + regras R8 validadas
- [ ] Conectar o trace logger aos use cases de IA
- [ ] Ponto de entrada de navegação para a tela de ATS
- [ ] Corrigir e habilitar a suíte de testes unitários no CI
- [ ] Substituir os placeholders da política por dados reais da empresa

## Licença

MIT — veja [LICENSE](LICENSE).

---

<div align="center">

**[⬆ Voltar ao topo](#resume-ai-builder)**

</div>
