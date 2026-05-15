# Raitha Bharosa – Agricultural Support Platform

> **Raitha Bharosa** (ರೈತ ಭರೋಸ) means *"Farmer's Trust"* in Kannada.  
> A comprehensive Android application bridging farmers and agricultural labourers in Karnataka, India.

---

## Overview

Raitha Bharosa is a full-stack Android application designed to empower farmers and agricultural workers. It provides real-time commodity market prices, weather forecasts, a labour booking marketplace, agri-input sourcing, and a farmer community — all in a single, multilingual platform.

---

## Features

| Module | Description |
|---|---|
| **Authentication** | Email/password and Google Sign-In via Firebase Auth |
| **Role Selection** | Separate onboarding flows for Farmers and Agricultural Labourers |
| **Dashboard** | Live weather data, crop market prices (Agmarknet/Mandi) |
| **Labour Marketplace** | Search, filter, and book labourers by skill, location, and wage |
| **Labourer Profile** | Work history, ratings, availability, and skill tags |
| **Crop Management** | Track crop cycles, inputs, and harvest planning |
| **Agri Inputs Store** | Browse and source seeds, fertilizers, and pesticides |
| **Community Board** | Post, read, and interact with the farmer community |
| **Payments** | Secure booking payments via Razorpay |
| **Notifications** | SMS alerts via Twilio; WhatsApp Business API integration |
| **Offline Support** | Local caching with Room database |
| **Multilingual** | Kannada and English language support |

---

## Tech Stack

### Android
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + Repository pattern) |
| Navigation | Jetpack Navigation Compose |
| Local DB | Room (SQLite) |
| Image Loading | Coil |
| HTTP Client | Retrofit 2 + OkHttp |
| Async | Kotlin Coroutines + Flow |

### Backend / Cloud
| Service | Purpose |
|---|---|
| Firebase Firestore | Real-time NoSQL database |
| Firebase Auth | User authentication |
| Firebase Storage | Profile photos and media |
| Agmarknet API | Live commodity market prices |
| OpenWeatherMap API | Real-time weather data |
| Razorpay | Payment gateway |
| Twilio | SMS notifications |
| WhatsApp Business API | WhatsApp notifications |

---

## Project Structure

```
RaithaBharosaAndroid/
├── app/
│   ├── src/main/
│   │   ├── java/com/raitha/bharosa/
│   │   │   ├── data/              # Models, repositories, Room DB, API services
│   │   │   ├── ui/
│   │   │   │   ├── screens/       # Composable screens (Dashboard, Labour, Crops, …)
│   │   │   │   └── theme/         # Material 3 theme, typography, colours
│   │   │   ├── viewmodel/         # ViewModels for each feature
│   │   │   ├── utils/             # Utility helpers
│   │   │   ├── engine/            # Core business logic
│   │   │   └── MainActivity.kt
│   │   ├── res/                   # Drawables, strings, themes
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   ├── google-services.json.example   # Firebase config template (copy → google-services.json)
│   └── .env.example                   # API keys template (copy → .env)
├── firebase/
│   ├── firestore.rules            # Firestore security rules
│   ├── firestore.indexes.json
│   └── storage.rules
├── gradle/
│   └── libs.versions.toml         # Centralised dependency versions
├── scripts/
│   └── seed_labour_data     
└── build.gradle.kts
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11+
- Android SDK 35 (compile), min SDK 26
- A Firebase project with Firestore, Auth, and Storage enabled
- Node.js ≥ 18 (for the seed script only)

### 1. Clone the Repository

```bash
git clone https://github.com/yogeeshsm/RaithaBharosa_app.git
cd RaithaBharosa_app
```

### 2. Firebase Setup

1. Create a project at [Firebase Console](https://console.firebase.google.com/).
2. Add an **Android app** with package name `com.raitha.bharosa`.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Enable **Email/Password** and **Google** sign-in providers in Firebase Auth.
5. Create a **Firestore** database (Native mode) in your preferred region.
6. Deploy security rules:
   ```bash
   cd firebase
   npx firebase-tools@latest deploy --only firestore:rules,storage
   ```

### 3. Configure API Keys

Copy the environment template and fill in your credentials:

```bash
cp app/.env.example app/.env
```

Edit `app/.env` — refer to the template comments for where to obtain each key.

### 4. Build & Run

Open the project in Android Studio, sync Gradle, and run on a device or emulator (API 26+).

```bash
# Or build from CLI
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Firestore Security Rules

Production-ready rules are in [`firebase/firestore.rules`](firebase/firestore.rules).  
Key policies:
- Users can only read/write their own profile documents.
- Labourer profiles are publicly readable but only owner-writable.
- Booking records are accessible only to the involved farmer and labourer.

---

## Seed Data (Development)

Populate Firestore with sample labourer profiles for development and testing:

```bash
cd scripts
npm install
# Place your Firebase Admin serviceAccountKey.json in scripts/
node seed_labour_data.js
```

> The `serviceAccountKey.json` must **never** be committed to version control.

---

## Architecture

```
UI Layer (Composables)
       │
       ▼
ViewModel Layer (StateFlow)
       │
       ▼
Repository Layer (AppRepository / LabourRepository)
       │
  ┌────┴────┐
  ▼         ▼
Room DB   Firebase / REST APIs
```

---

## Security

- `google-services.json` and `app/.env` are excluded from version control via `.gitignore`.
- API keys are injected at build time via `BuildConfig` from the `.env` file — no secrets in source code.
- Firestore rules enforce per-user data isolation.

---

## License

Developed as an academic capstone project. All rights reserved.

---

## Author

**Yogeesh S M**  
GitHub: [@yogeeshsm](https://github.com/yogeeshsm)
- ⚠️ Razorpay Payment (optional - needs credentials)

## 🧪 Testing

### Run Tests
```bash
./gradlew test
```

### Run on Device
```bash
./gradlew installDebug
```

## 📄 License

Copyright © 2026 RaithaBharosa Team

## 🆘 Support

For issues and questions:
- Check [documentation](docs/)
- Review [setup guide](docs/ANDROID_STUDIO_SETUP.md)
- See [troubleshooting](docs/ANDROID_STUDIO_SETUP.md#troubleshooting)

---

**Build Status**: ✅ SUCCESS  
**APK Size**: 21.01 MB  
**Last Build**: May 13, 2026 at 20:27  
**APK Location**: `app/build/outputs/apk/debug/app-debug.apk`
