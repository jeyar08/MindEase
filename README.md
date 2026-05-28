# MindEase — Application Documentation

> **Version:** 1.0  
> **Platform:** Android (minSdk 24 / Android 7.0+, targetSdk 36)  
> **Language:** Java  
> **Package:** `com.jeyar.mindease`  
> **Last updated:** May 2026

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Features](#2-features)
3. [Architecture](#3-architecture)
4. [Project Structure](#4-project-structure)
5. [Data Layer](#5-data-layer)
6. [Network Layer](#6-network-layer)
7. [UI Layer](#7-ui-layer)
8. [Security Model](#8-security-model)
9. [Configuration & Setup](#9-configuration--setup)
10. [Building & Running](#10-building--running)
11. [Third-Party Dependencies](#11-third-party-dependencies)
12. [Known Limitations](#12-known-limitations)
13. [Future Recommendations](#13-future-recommendations)

---

## 1. Product Overview

**MindEase** is an Android mobile application that provides users with a private, AI-powered emotional support companion. Users can hold free-form conversations with a Gemini-backed assistant that is carefully prompted to be empathetic, non-judgmental, and transparent about its limitations.

### Core value proposition

| Pillar | Description |
|---|---|
| **Emotional support** | A safe, private space to reflect on thoughts and feelings |
| **AI-powered** | Backed by Google's Gemini language models via the Generative Language API |
| **Privacy-first** | All conversation data is stored locally on the device — nothing is sent to a third-party server other than the Gemini API itself |
| **Safety-aware** | Hardcoded system instruction prevents diagnosis, limits medical advice, and mandates crisis escalation when self-harm signals are detected |

### Disclaimer
MindEase is **not** a medical application, therapeutic service, or crisis line. It is a supportive companion tool. The system instruction embedded in every API request makes this clear to the model, and the UI surfaces a disclaimer to every user.

---

## 2. Features

### 2.1 Authentication
- **Sign up** with email and password (stored locally, never transmitted)
- **Log in** with existing credentials
- Persistent session via `SharedPreferences` — users stay logged in across app restarts
- **Log out** clears the session but preserves conversation history

### 2.2 Conversation Management
- **Conversation list** — all past conversations shown in reverse chronological order by last activity
- **New chat** — floating action button starts a blank conversation
- **Auto-title** — the first message (truncated to 40 characters) becomes the conversation title automatically
- **Delete conversation** — long-press any conversation row → confirmation dialog → permanently deletes the conversation and all its messages
- **Empty state** — friendly prompt shown when no conversations exist

### 2.3 Chat
- **Send messages** — multi-line input, sends on button tap
- **Gemini responses** — assistant replies are displayed in chat bubbles with a typing indicator while the response is loading
- **Full conversation history** — the entire message history is sent to Gemini as context on every request, giving the assistant memory of the current conversation
- **Message queue** — messages typed while Gemini is processing are queued and automatically sent in order after each response arrives; a banner shows the queue depth with a **Clear** option
- **Edit message** — long-press a user bubble → **Edit message** → pre-filled dialog → re-sends from that point with the revised text; history after the edited message is discarded
- **Retry response** — long-press any bubble → **Retry response** → re-generates the assistant reply from the last user message
- **Error handling** — graceful user-facing messages for timeouts, rate limits, auth failures, and offline state; a **Retry** action is available from the error snackbar

### 2.4 Settings
- **Gemini model selector** — choose from:
  - `gemini-2.5-flash` *(default)*
  - `gemini-2.5-pro`
  - `gemini-2.0-flash`
  - `gemini-1.5-flash`
  - `gemini-1.5-pro`
- **Custom API key** — users can supply their own Gemini API key to override the build-time key baked into the app; leaving it blank falls back to the default key

### 2.5 Safety & Support
- **Disclaimer dialog** — shown once at first use
- **Support & Safety sheet** — accessible from the chat toolbar; displays the app disclaimer and configurable regional crisis hotlines

---

## 3. Architecture

MindEase follows a standard **MVVM (Model-View-ViewModel)** architecture with a Repository layer, as recommended by Android Architecture Components.

```
┌─────────────────────────────────────────────────────┐
│                      UI Layer                        │
│  Activity → ViewModel (LiveData) → Adapter/View     │
└────────────────────────┬────────────────────────────┘
                         │ observes / calls
┌────────────────────────▼────────────────────────────┐
│                  Repository Layer                    │
│          MindEaseRepository (singleton)              │
│  Coordinates Room (IO thread) and Gemini (enqueue)  │
└──────────┬─────────────────────────┬────────────────┘
           │                         │
┌──────────▼──────┐       ┌──────────▼──────────────┐
│   Data Layer    │       │      Network Layer        │
│  Room Database  │       │  Retrofit + Gemini API   │
│  (local SQLite) │       │  (HTTPS, no server)      │
└─────────────────┘       └──────────────────────────┘
```

### Key design decisions

| Decision | Rationale |
|---|---|
| Single-activity per screen | Simple; avoids fragment backstack complexity for this app's scope |
| Repository singleton | Shared across ViewModels; single source of truth for DB + API coordination |
| `ExecutorService` (single thread) for DB | Serialises all Room operations, preventing race conditions without Coroutines |
| `Handler(mainLooper)` for UI callbacks | All LiveData `.setValue()` calls happen on the main thread |
| `LiveData` for conversation list | Room emits updates automatically when conversations are inserted/deleted |
| In-memory `history` list in ViewModel | Avoids re-querying the DB after every message; kept in sync manually |
| `ArrayDeque` message queue | FIFO delivery; messages sent while Gemini is busy are drained one-by-one after each response |

---

## 4. Project Structure

```
MindEase/
├── app/
│   ├── build.gradle.kts          ← Gradle config, API key injection
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jeyar/mindease/
│       │   ├── data/             ← Room entities + DAOs + AppDatabase
│       │   │   ├── AppDatabase.java
│       │   │   ├── Conversation.java
│       │   │   ├── ConversationDao.java
│       │   │   ├── Message.java
│       │   │   ├── MessageDao.java
│       │   │   ├── User.java
│       │   │   └── UserDao.java
│       │   ├── network/          ← Retrofit + Gemini API models
│       │   │   ├── GeminiApi.java
│       │   │   ├── GeminiRequest.java
│       │   │   ├── GeminiResponse.java
│       │   │   └── RetrofitClient.java
│       │   ├── repository/       ← Single source of truth
│       │   │   ├── GeminiCallback.java
│       │   │   ├── MindEaseRepository.java
│       │   │   └── RepoCallback.java
│       │   └── ui/               ← Activities, ViewModels, Adapters
│       │       ├── ChatActivity.java
│       │       ├── ChatViewModel.java
│       │       ├── ConversationAdapter.java
│       │       ├── ConversationListActivity.java
│       │       ├── ConversationListViewModel.java
│       │       ├── LoginActivity.java
│       │       ├── MessageAdapter.java
│       │       ├── SettingsActivity.java
│       │       └── SignUpActivity.java
│       │   └── util/
│       │       ├── PasswordHasher.java
│       │       ├── SafetyConstants.java
│       │       └── SessionManager.java
│       └── res/
│           ├── drawable/         ← Vector drawables, chat bubbles, input bg, icon layers
│           ├── layout/           ← XML layouts for every Activity and item row
│           ├── menu/             ← Toolbar overflow menus
│           ├── mipmap-*/         ← Launcher icons (PNG, all densities + adaptive XML)
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml
├── build.gradle.kts              ← Root build config
├── local.properties              ← GEMINI_API_KEY (NOT committed to VCS)
└── settings.gradle.kts
```

---

## 5. Data Layer

All data is stored **on-device** in a SQLite database managed by Room (`mindease.db`).

### 5.1 Entities

#### `User`
| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER` PK autoincrement | |
| `email` | `TEXT` | Unique index; normalised to lowercase |
| `passwordHash` | `TEXT` | SHA-256 iterated hash (10 000 rounds) |
| `salt` | `TEXT` | 16-byte cryptographic random salt, Base64-encoded |

#### `Conversation`
| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER` PK autoincrement | |
| `userId` | `INTEGER` | FK → `users.id` (index) |
| `title` | `TEXT` | Derived from first message (≤40 chars) |
| `createdAt` | `INTEGER` | Unix epoch ms |
| `updatedAt` | `INTEGER` | Updated on every new message |

#### `Message`
| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER` PK autoincrement | |
| `conversationId` | `INTEGER` | FK → `conversations.id` (index) |
| `role` | `TEXT` | `"user"` or `"model"` |
| `text` | `TEXT` | Raw message content |
| `timestamp` | `INTEGER` | Unix epoch ms |

### 5.2 DAOs

| DAO | Key operations |
|---|---|
| `UserDao` | `insert`, `getByEmail` |
| `ConversationDao` | `insert`, `observeForUser` (LiveData), `getById`, `updateTimestamp`, `deleteById` |
| `MessageDao` | `insert`, `getForConversation`, `deleteForConversation`, `deleteAfterMessageId`, `updateText` |

### 5.3 Database version
`AppDatabase` is at **version 1**. No schema migrations have been applied. Future schema changes must either add a `Migration` or call `fallbackToDestructiveMigration()` (which wipes user data).

---

## 6. Network Layer

### 6.1 Endpoint
All requests go to the **Google Generative Language API**:

```
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={API_KEY}
```

The model and API key are resolved at runtime from `SessionManager` (user-supplied values take precedence over the build-time key).

### 6.2 Request structure

```json
{
  "system_instruction": {
    "parts": [{ "text": "<SafetyConstants.SYSTEM_INSTRUCTION>" }]
  },
  "contents": [
    { "role": "user",  "parts": [{ "text": "..." }] },
    { "role": "model", "parts": [{ "text": "..." }] },
    ...
  ]
}
```

The full in-memory message history is sent on every request so Gemini has conversation context.

### 6.3 HTTP timeouts

| Timeout | Value |
|---|---|
| Connect | 30 s |
| Read | 45 s |
| Write | 30 s |

### 6.4 Error handling

| HTTP Code | User-facing message |
|---|---|
| 400 | Problem with the request — check API key |
| 401 / 403 | Access denied — invalid API key |
| 429 | Too many requests — wait and retry |
| Other 4xx/5xx | Generic "something went wrong" + code |
| Timeout | "Took longer than expected" |
| No connection | "You appear to be offline" |

All errors surface as a `Snackbar` with a **Retry** action. On error, the message queue is cleared to prevent stale messages from replaying after a failed context.

### 6.5 Logging
In `DEBUG` builds, OkHttp logs full request/response bodies (`BODY` level). In `RELEASE` builds, logging is set to `NONE`.

---

## 7. UI Layer

### 7.1 Screens

| Screen | Class | Entry point |
|---|---|---|
| Login | `LoginActivity` | App launcher |
| Sign up | `SignUpActivity` | Link on Login screen |
| Conversation list | `ConversationListActivity` | After successful login |
| Chat | `ChatActivity` | Tap a conversation or New Chat FAB |
| Settings | `SettingsActivity` | Overflow menu on conversation list |

### 7.2 Navigation flow

```
LoginActivity ──────────────────────────► ConversationListActivity
     │                                            │    ▲
     ▼                                            │    │ Back
SignUpActivity ──── success ────────────►         │
                                                  ▼
                                           ChatActivity
                                                  │
                                                  ▼
                                          SettingsActivity
```

### 7.3 Key UI components

#### Message bubbles
- **User messages** — right-aligned, primary teal background, white text
- **Model messages** — left-aligned, light grey background, dark text
- Long-press either → context menu for **Edit** (user) / **Retry** (both)

#### Queue banner
Appears between the typing indicator and the input bar when messages are queued. Shows "N message(s) queued" with a spinner and a **Clear** button. Fades in/out with system animations.

#### Typing indicator
Visible whenever a Gemini request is in-flight. Shows a small `ProgressBar` and "MindEase is typing…" text.

### 7.4 Adapters

| Adapter | Drives | Interactions |
|---|---|---|
| `ConversationAdapter` | Conversation list RecyclerView | Click → open chat; Long-press → delete dialog |
| `MessageAdapter` | Chat RecyclerView | Long-press → edit/retry dialog |

### 7.5 ViewModels

#### `ChatViewModel`
The most complex ViewModel. Holds:
- `history` — in-memory ordered list of all messages in the current conversation
- `messageQueue` — `ArrayDeque<String>` of pending outbound messages
- LiveData: `messages`, `loading`, `title`, `error`, `queueCount`

Key methods:

| Method | Description |
|---|---|
| `sendMessage(text)` | Enqueues if busy, otherwise dispatches immediately |
| `editMessage(target, newText)` | Truncates DB + memory history, updates text, re-sends |
| `retryFromMessage(target)` | Walks back to last user message, truncates, re-sends |
| `retry()` | Re-sends after a snackbar retry; clears queue |
| `clearQueue()` | Empties the message queue; sets `queueCount` to 0 |

#### `ConversationListViewModel`
Thin wrapper; exposes `observeConversations(userId)` as `LiveData` and delegates `deleteConversation` to the repository.

---

## 8. Security Model

### 8.1 Password storage
Passwords are **never stored in plaintext**. The hashing scheme is:

1. Generate a 16-byte cryptographically random salt (`SecureRandom`)
2. Compute `SHA-256(salt ‖ password)` iterated **10 000 times**
3. Store the Base64-encoded hash and salt in the `users` table

Verification uses `MessageDigest.isEqual()` (constant-time comparison) to prevent timing attacks.

> [!WARNING]
> The current scheme uses iterated SHA-256, not a dedicated password KDF such as **PBKDF2**, **bcrypt**, or **Argon2**. For a production application handling real user credentials, upgrading to `PBKDF2WithHmacSHA256` (available in Android via `SecretKeyFactory`) is strongly recommended.

### 8.2 API key handling
The Gemini API key is injected at build time from `local.properties` (never committed to version control) into `BuildConfig.GEMINI_API_KEY`. Users may override it at runtime via the Settings screen; this override is stored in `SharedPreferences`.

> [!CAUTION]
> API keys stored in `BuildConfig` are extractable from a release APK. For a public release, consider proxying Gemini calls through a backend server you control, so the API key never leaves your infrastructure.

### 8.3 Data privacy
- All conversation data lives in Room on the device.
- No analytics SDK, crash reporter, or telemetry library is included.
- The only outbound network call is to `generativelanguage.googleapis.com` (Google's API).
- `SharedPreferences` stores: user ID, email, disclaimer shown flag, selected Gemini model, and optional custom API key (cleartext — same risk profile as any stored credential on Android).

### 8.4 Session management
`SessionManager` stores `userId` and `email` in `SharedPreferences`. Log out removes both keys. Settings (model, key) and the disclaimer flag are intentionally retained across logouts so users don't lose their preferences.

---

## 9. Configuration & Setup

### 9.1 Prerequisites

| Tool | Minimum version |
|---|---|
| Android Studio | Iguana (2023.2.1) or newer |
| JDK | 11 |
| Android SDK | API 36 (compileSdk) |
| Gemini API key | Obtain free at [aistudio.google.com](https://aistudio.google.com) |

### 9.2 API key setup

Create or open `local.properties` in the project root and add:

```properties
GEMINI_API_KEY=your_api_key_here
```

This file is listed in `.gitignore` and must **never** be committed.

### 9.3 Crisis resources
The crisis hotline strings are intentionally left as editable placeholders. Before publishing, update `res/values/strings.xml` → `crisis_resources` with real, regionally appropriate hotline numbers.

```xml
<string name="crisis_resources">
    <!-- Replace with real local crisis numbers -->
    ...
</string>
```

### 9.4 Gemini model default
The default model is `gemini-2.5-flash`. To change it programmatically, update `SessionManager.DEFAULT_GEMINI_MODEL`. Users can also change it at runtime via the Settings screen.

---

## 10. Building & Running

### Debug build
```bash
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Release build
```bash
./gradlew assembleRelease
```
A signing configuration must be set up in `build.gradle.kts` before distributing.

### Install on connected device
```bash
./gradlew installDebug
```

### Run tests
```bash
./gradlew test               # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests (requires device/emulator)
```

---

## 11. Third-Party Dependencies

| Library | Purpose | License |
|---|---|---|
| **AndroidX AppCompat** | Backward-compatible Activity/Fragment base | Apache 2.0 |
| **Material Components** | Buttons, dialogs, FAB, toolbar, snackbar | Apache 2.0 |
| **RecyclerView** | Conversation and message lists | Apache 2.0 |
| **Lifecycle ViewModel** | State survival across configuration changes | Apache 2.0 |
| **Lifecycle LiveData** | Reactive UI updates | Apache 2.0 |
| **Room Runtime** | Local SQLite ORM | Apache 2.0 |
| **Room Compiler** | Annotation processing for Room | Apache 2.0 |
| **Retrofit 2** | Type-safe HTTP client for Gemini API | Apache 2.0 |
| **Gson Converter** | JSON serialisation/deserialisation | Apache 2.0 |
| **OkHttp Logging Interceptor** | Debug network logging | Apache 2.0 |

All dependencies are declared in the Gradle version catalog (`libs.versions.toml`). No proprietary or GPL-licensed libraries are used.

---

## 12. Known Limitations

| # | Limitation | Impact |
|---|---|---|
| 1 | **No server-side backend** — all data is on-device | If the user uninstalls the app or clears storage, all conversations are lost |
| 2 | **No multi-device sync** | Conversations are not available on other devices or after re-install |
| 3 | **Iterated SHA-256 password hashing** | Weaker than PBKDF2/bcrypt for a server-facing credential store; acceptable for purely local auth |
| 4 | **API key in APK** | The build-time key is extractable from a release APK; must be protected server-side for production |
| 5 | **Full history in every request** | For very long conversations, token costs and latency will increase; no truncation strategy is implemented |
| 6 | **No offline message drafting** | If the device is offline, the send button does nothing (an error snackbar is shown) |
| 7 | **No push notifications** | The app cannot alert the user to anything while it is in the background |
| 8 | **Single user per device install** | The auth system supports multiple accounts but no account-switching UI is provided |
| 9 | **No message search** | There is no way to search across conversations or within a conversation |
| 10 | **Database version 1, no migration** | Structural database changes in future versions will require a migration strategy |

---

## 13. Future Recommendations

### Short-term (before public release)
- [ ] Replace iterated SHA-256 with **PBKDF2WithHmacSHA256** for password hashing
- [ ] Add a **backend proxy** for the Gemini API key to prevent key leakage
- [ ] Update `crisis_resources` strings with real, localised hotline numbers
- [ ] Enable **ProGuard / R8** minification for the release build
- [ ] Add **app signing** configuration
- [ ] Implement **context windowing** — cap the message history sent to Gemini (e.g., last 20 messages) to control token costs and latency

### Medium-term
- [ ] **Cloud sync** via Firebase Firestore or a custom backend, enabling cross-device access and backup
- [ ] **Biometric lock** — require fingerprint/face authentication to open the app
- [ ] **Message search** across all conversations
- [ ] **Dark mode** support
- [ ] **Accessibility** audit (content descriptions, minimum touch target sizes, TalkBack compatibility)
- [ ] **Conversation export** — let users download a transcript as plain text or PDF

### Long-term / Growth
- [ ] **Mood tracking** — periodic check-ins with a simple emoji/slider scale, stored locally
- [ ] **Insight summaries** — weekly digest of themes from the user's conversations (processed on-device or via Gemini)
- [ ] **Guided exercises** — breathing timers, grounding techniques surfaced contextually
- [ ] **Therapist handoff flow** — structured prompt to help the user prepare for a professional appointment

---

*This document was prepared at the completion of the MindEase v1.0 development milestone.*
