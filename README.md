# NEXUS — Agentic Day Planner for Android

[![Nexus Android CI/CD](https://github.com/atharv-pingle/nexus-app/actions/workflows/android.yml/badge.svg)](https://github.com/atharv-pingle/nexus-app/actions/workflows/android.yml)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](version.properties)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://android.com)
[![Design](https://img.shields.io/badge/design-anti--slop%20minimal-black.svg)](https://github.com/miqdadbadjuber/anti-slop)

**Nexus** is an autonomous, science-backed agentic day planner for Android. It eliminates procrastination by adapting your daily schedule dynamically, learning your energy patterns, and keeping your career, fitness, and side-projects in frictionless orbit.

---

## ⚡ Core Philosophy & Behavioral Science

Most day planners treat humans like rigid machines: you schedule a 9:00 AM block, miss it by 15 minutes, experience the *"what-the-hell"* defeatist effect, and abandon the day. 

Nexus fixes this with four science-backed systems:
1. **Dynamic Elastic Rebalancing**: Tasks never break when life happens. Tap `+15m`, `+30m`, or speak to the agent, and your afternoon gracefully recalibrates without dropped tasks.
2. **The 15:30 Work-Life Interceptor**: Work-from-home causes cognitive exhaustion at the end of the workday. Instead of an uncontrolled 2-hour sleep coma and phone scrolling, Nexus triggers a controlled 20-minute power nap / NSDR (Non-Sleep Deep Rest) timer.
3. **2-Minute Launchpad (Implementation Intentions)**: Overcoming starting friction is 90% of the battle. Every block includes an actionable 2-minute starter step.
4. **Dr. Lally 66-Day Habit Trajectory (UCL Model)**: Tracks automaticity across 3 phases (Resistance → Installation → Automaticity). Missing one day does not reset your progress; it activates **Rebound Resilience**.

---

## 📱 Features

* **Daily Orbit (Timeline)**: Live active block with countdown, 2-minute micro action, and 1-tap tactile delay buttons (`+15m`, `+30m`, `Skip`).
* **Hands-Free Voice Mode**: Tap the single center mic button on the bottom bar to update the agent naturally (*"Meeting ran over by 30 mins, push evening back"* or *"Start power nap"*). Built with Android `SpeechRecognizer` and `TextToSpeech`.
* **Nightly AI Architect**: 20-second debrief (energy rating + friction tags). The OpenRouter AI engine synthesizes today's slips and auto-generates tomorrow's master plan.
* **Autonomous AI Coach**: Direct on-demand dialogue with your AI Chief of Staff (brainstorm SaaS ideas, swap home workout exercises, adjust schedule).
* **66-Day Habit & Friction Telemetry**: Automaticity progress curves for side projects and home dumbbell workouts, plus actionable friction audits (detecting phone scrolling traps).
* **Anti-Slop Minimal Design**: Built following `miqdadbadjuber/anti-slop` principles—pure black, zinc, and high-contrast typography with zero decorative AI clutter or pastel gradients.

---

## 🏗️ Architecture & Tech Stack

```
app/
 ├── data/
 │   ├── local/          # Room DB (ScheduleBlock, Habit, FrictionLog, UserProfile)
 │   ├── remote/         # OpenRouter API Client (Structured JSON schema)
 │   └── repository/     # NexusRepository (Offline-first, dynamic delay arithmetic)
 ├── service/            # VoiceInteractionManager (SpeechRecognizer & TTS)
 └── ui/
     ├── timeline/       # Daily Orbit timeline & tactile controls
     ├── architect/      # Nightly debrief & next-day plan generator
     ├── coach/          # Direct OpenRouter agent chat
     ├── stats/          # 66-day habit curves & friction analytics
     └── components/     # Center voice modal sheet & tactile components
```

* **OS**: Android (minSdk 26, targetSdk 34)
* **Language & UI**: Kotlin, Jetpack Compose, Material 3
* **Database**: Room Database (Local-first, encrypted, 100% private)
* **AI Engine**: OpenRouter API (`google/gemini-2.0-flash-exp:free`, `anthropic/claude-3.5-sonnet`, `meta-llama/llama-3.3-70b-instruct`)
* **CI/CD**: GitHub Actions automated pipeline with dynamic semantic versioning

---

## 🚀 CI/CD & Dynamic Versioning

The GitHub Actions workflow (`.github/workflows/android.yml`) automatically builds and tests the app on every push to `main` and on pull requests:

* **Dynamic Semantic Versioning**:
  - `version.properties` defines `major`, `minor`, and `patch`.
  - In CI/CD, the version code and name are dynamically computed using `GITHUB_RUN_NUMBER`:
    - Version Code: `${GITHUB_RUN_NUMBER}`
    - Version Name: `1.0.0.${GITHUB_RUN_NUMBER}`
* **Automated Artifacts & Releases**:
  - Every successful CI run uploads the debug APK artifact to GitHub Actions.
  - Commits on `main` automatically publish a GitHub Release with the APK ready to install.

---

## 🔑 Configuration (OpenRouter API Key)

Nexus is local-first and works immediately out of the box with offline intelligence. To enable cloud LLM synthesis:
1. Get an API key from [OpenRouter](https://openrouter.ai/keys).
2. Save your key in the app settings, or the agent can securely store it via `OpenRouterClient.saveApiKey("YOUR_KEY")`.

---

## 📦 Building Locally

```bash
# Clone the repository
git clone https://github.com/atharv-pingle/nexus-app.git
cd nexus-app

# Build debug APK
./gradlew assembleDebug
```
The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
