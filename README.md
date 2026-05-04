# 🤖 ConvoAI

ConvoAI is an intelligent, feature-rich **AI chat application** built natively for Android.  
It operates on a **dual-app system** (User App & Admin App) and features conversational AI, image generation, real-time voice mode, and a fully integrated virtual economy.

---

## ✨ Features
- 💬 **Smart AI Chat**: Context-aware conversations powered by advanced LLMs (via Retrofit API).
- 👁️ **Vision Capabilities**: Upload images and ask the AI to analyze or describe them.
- 🪄 **Magic Wand (Image Generation)**: Generate high-quality images directly from text prompts using Pollinations.ai.
- 🎙️ **Hands-Free Voice Mode**: Voice interface with STT & TTS, plus pulsing UI animations.
- 📜 **Local Chat History**: Secure offline conversation logging with Room Database.
- 💰 **Dynamic Coin Economy**:
  - Watch **Rewarded Ads** to earn coins.
  - Upgrade to **Pro Subscription** for unlimited access.
- 🛡️ **Admin Dashboard**: Monitor users and manage app states.
- ☁️ **Cloud Configured**: Update API keys, models, and pricing via Firebase Remote Config.
- 🔒 **Native Security**: API keys protected with C++ (JNI).

---

## 🛠️ Tech Stack
| Layer            | Technology |
|------------------|------------|
| Language         | Kotlin, C++ (JNI) |
| Architecture     | Single-Activity |
| Local Storage    | Room DB, SharedPreferences |
| Backend & Cloud  | Firebase Auth, Firestore, Remote Config |
| Networking       | Retrofit2, Gson |
| Image Loading    | Coil |
| Monetization     | AdMob, Google Play Billing |

---

## 📂 Project Structure
- **ConvoAI (User App)** → Client-side app for AI chat, history, and coin economy.
- **ConvoAI Admin (Admin App)** → Secured dashboard with Firestore rules.

---

## 🚀 Setup & Installation

### Prerequisites
- Android Studio (latest)
- Firebase Project

### 1. Firebase Setup
- Add both apps (`com.duddletech.convoai` & `com.duddletech.convoaiadmin`) to Firebase.
- Add SHA-1 & SHA-256 fingerprints.
- Place `google-services.json` in each app directory.
- Enable Email/Password Auth + Firestore.

### 2. Firestore Security Rules
```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null && 
        request.auth.token.email.matches("(?i)your_admin_email@gmail.com");
    }
  }
}
```

## 📸 App Preview

### User App
<p align="center">
  <img src="https://github.com/user-attachments/assets/69c958aa-a9d9-44d1-9495-2ae1500312aa" width="250" />
  <img src="https://github.com/user-attachments/assets/1743ba5d-bf42-42dd-ac8c-e2e187cdc8aa" width="250" />
  <img src="https://github.com/user-attachments/assets/9594f18a-6302-4d29-939b-0012302bc01f" width="250" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/8ed96bad-8cfb-4e10-b6f8-8bca546d64c4" width="250" />
  <img src="https://github.com/user-attachments/assets/d1325435-6a25-4f42-b0cb-be5d86491615" width="250" />
  <img src="https://github.com/user-attachments/assets/33194116-b0ca-452d-b32c-d5c7da336cf2" width="250" />
</p>
