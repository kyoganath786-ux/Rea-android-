# NICK AI — Mobile On-Device AI, REA Software Analysis & Termux Studio

**NICK AI** is a complete, mobile-first Android application combining:
1. **On-device Local AI:** GGUF inference runtime with offline model catalog, memory checks, quantization, streaming generation, and SAF model importer.
2. **REA (Reverse Engineer Anything) Software Analysis:** Official adapter for `illumi-ai/REA`, diagnostic verification (`npx -y rea-agents@latest doctor --json`), tool discovery, and project inspection.
3. **Termux Integration:** Package detection, permission audit (`com.termux.permission.RUN_COMMAND`), guided copyable CLI setup workflows, and local loopback bridge (`127.0.0.1:8765`).
4. **Voice Assistant (NICK):** Tap-to-talk speech recognition (`SpeechRecognizer`), spoken responses (`TextToSpeech`), animated waveforms, and language configuration.
5. **Software & APK Analysis:** Static APK inspector (manifest permissions, exported components, debuggable flags, DEX headers), safe ZIP extractor with ZipSlip path traversal protection, and Project Reconstruction Assistant.
6. **Secure Tool Registry:** Sandboxed execution pipeline with explicit confirmation gates before consequential file or system operations.
7. **Optional Cloud AI:** Gemini API & OpenAI-compatible endpoints with strict user consent gates (no cloud transmission without explicit authorization).

---

## Architecture Overview

```
com.example
├── MainActivity.kt                 # Adaptive edge-to-edge Compose UI entry point
├── ai/
│   ├── LocalModelProvider.kt       # On-device GGUF inference, RAM/Storage checks, download manager
│   └── CloudModelProvider.kt       # Optional Gemini/OpenAI client with consent gate
├── rea/
│   └── REAAdapter.kt               # illumi-ai/REA adapter, doctor JSON parser, bridge client
├── termux/
│   └── TermuxIntegration.kt        # Termux package detection, guided terminal steps, bridge
├── analysis/
│   └── ProjectAnalyzer.kt          # ZipSlip defense, static APK parser, AST reconstruction
├── tools/
│   └── ToolRegistry.kt             # Sandboxed tool registry, confirmation gates, audit log
├── voice/
│   └── VoiceAssistantManager.kt    # Android SpeechRecognizer & TextToSpeech engine
├── data/
│   ├── local/                      # Room Database (conversations, messages, projects, tools)
│   └── preferences/                # Jetpack DataStore (AI mode, models, bridge, consent)
└── ui/
    ├── NickAiViewModel.kt          # Unified state container & orchestrator
    ├── theme/                      # M3 Cyber-Intelligence Theme (Dark Obsidian & Neon Cyan)
    ├── components/                 # Navigation, Voice dialog, Tool confirmation modal
    └── screens/                    # Dashboard, Chat, Analysis, TermuxRea, ToolManager, Settings
```

---

## Operating Modes

- **`LOCAL_ONLY`**: Strictly offline. Uses on-device GGUF models (`TinyLlama 1.1B`, `Qwen 2.5 0.5B`, `SmolLM2 1.7B`, `Gemma 2 2B`) and local tools. Zero network transmission.
- **`LOCAL_FIRST`** (Default): Local intelligence first. Prompts user for confirmation before external transfers.
- **`CLOUD_ONLY`**: Directs complex queries to configured cloud endpoints (Gemini, OpenRouter) with explicit user consent.
- **`AUTO`**: Prefers local execution when memory allows, falling back to cloud only when authorized.

---

## Termux & REA Setup Instructions

### 1. Install Termux
Install the official Termux release from GitHub:
[Termux Releases on GitHub](https://github.com/termux/termux-app/releases)

### 2. Guided Terminal Workflow
Run the following commands inside Termux:

```bash
# Step 1: Update Termux & install Node.js LTS
pkg update -y && pkg install -y nodejs-lts git

# Step 2: Verify runtime
node --version
npm --version

# Step 3: Run REA Diagnostics
npx -y rea-agents@latest doctor --json

# Step 4: Inspect REA CLI commands
npx -y rea-agents@latest --help

# Step 5: (Optional) Guided REA setup for coding agents
npx rea-agents@latest setup

# Step 6: Start Local Loopback Bridge for NICK AI
npx -y rea-agents@latest serve --port 8765 --token nick-local-sec
```

### 3. Android Bionic Libc Note
Termux runs on Android Bionic libc. If specialized native addons (e.g. Tree-Sitter native bindings) fail to compile in Termux directly:
1. Use the **Local Loopback Bridge** (`http://127.0.0.1:8765`) or `proot-distro install ubuntu`.
2. Configure a **Remote REA Host** (`https://user-controlled-server/api`) in Settings.
3. Use NICK AI's **Built-in Static Analyzer**, which operates 100% locally on Android without external dependencies.

---

## Security Policies

- **Zero Untrusted Execution:** APK files are analyzed purely statically. Binaries and classes are never executed on the phone.
- **ZipSlip Protection:** Archive entries validate canonical destination paths before writing (`canonicalPath.startsWith(destinationFolder)`).
- **Tool Confirmation Gate:** File creation, storage export, and Termux command dispatch require interactive user authorization.
- **Arbitrary Shell Execution Prohibited:** The tool registry deterministically rejects unverified commands (e.g. `rm -rf`, fork-bombs, arbitrary scripts).
- **Private Secrets Sandboxing:** API keys and credentials are saved in app-private sandbox storage and never logged or included in export files.

---

## Build & Test Instructions

### Build Debug APK:
```bash
gradle assembleDebug
```
The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Test Suite:
```bash
gradle testDebugUnitTest
```
Includes Robolectric tests for local model hardware checks, REA doctor JSON parsing, tool security denial, and context verification.
