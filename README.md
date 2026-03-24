# 🎵 WristWave : Wear Os Music Player

A **Wear OS music companion app** built in **Kotlin using Jetpack Compose**, allowing users to control music playback directly from their smartwatch using **touch interactions and wrist gestures**.

---

## 📱 Project Overview

This project is a **multi-module Android application** composed of:

- `app/` → Mobile application (music source & controller)
- `wear/` → Wear OS application (remote control & UI)
- Communication via **Google Wearable Data Layer API**

The goal is to provide a **smooth, responsive, and intuitive music experience** on a smartwatch.

---

## 📂 Project Structure
.
├── app/
│   ├── src/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── proguard-rules.pro
│
├── wear/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/epfl/esl/musicplayer/   (Kotlin + Compose UI, sensors, ViewModel)
│   │       ├── res/                           (Resources: icons, themes, etc.)
│   │       └── AndroidManifest.xml
│   │
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── lint.xml
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat

---

## 🚀 Features

### 🎧 Music Control (Wear → Phone)
- ▶️ Play / Pause  
- ⏮ Previous Track  
- ⏭ Next Track  

Commands are sent using the **Message API** for fast interaction.

---

### 🔄 Real-Time Synchronization (Phone → Watch)
- Song title  
- Album cover  
- Playback state  
- Track duration  
- Current position  

Uses:
- `DataClient` (Data API)
- `/static_songInfo` → metadata  
- `/dynamic_songInfo` → playback position  

---

### ⌚ Smartwatch UI (Jetpack Compose)
- Fully built with **Jetpack Compose**
- Album artwork display  
- Song title (ellipsis / marquee-ready)  
- Progress bar  
- Time indicator (MM:SS)  

---

### ⏱ Local Time Tracking
- The watch does **not rely on constant updates** from the phone  
- Receives position occasionally and updates locally every second  

---

### 🖐 Gesture Controls (Gyroscope-based)

Control music using wrist movements:

- 👉 Flick RIGHT → Previous track  
- 👉 Flick LEFT → Next track  
- 👉 Flick UP → Play / Pause  

#### ⚙️ Implementation Details
- Uses `SensorManager` and `TYPE_GYROSCOPE`
- Custom `WristFlickGyroDetector`
- Movement windowing (~120 ms)
- Signal averaging
- Cooldown to avoid multiple triggers

---

### 🔋 Smart Screen Behavior
- Screen stays **ON while music is playing**
- When paused:
  - stays ON for **3 seconds**
  - then returns to ambient mode  

Uses:
- `FLAG_KEEP_SCREEN_ON`
- Kotlin coroutines (`delay`, `launch`)

---

## 🏗 Architecture

### Separation of Concerns

| Layer | Responsibility |
|------|--------------|
| UI (Jetpack Compose) | Display state |
| ViewModel | Handle actions |
| Sensors | Detect gestures |
| Data Layer | Sync phone ↔ watch |

---

### Key Components

- `MainActivity (wear)`
  - Compose UI entry point  
  - Sensor handling  
  - Data listener (`onDataChanged`)  
  - Screen behavior logic  

- `WearPlayViewModel`
  - Sends commands via Message API  
  - Handles connected nodes  

- `WristFlickGyroDetector`
  - Converts raw gyroscope data → gestures  

---

## 🔄 Communication Model

### Phone → Watch
- Uses **Data API**
- Sends:
  - Song metadata  
  - Playback state  
  - Position updates  

### Watch → Phone
- Uses **Message API**
- Sends:
  - Play/Pause  
  - Next / Previous  

---

## 🧠 Technologies Used

- **Kotlin**
- **Jetpack Compose (UI)**
- Kotlin Coroutines  
- Wear OS APIs  
- SensorManager (Gyroscope)  
- Google Wearable Data Layer  

---

## ⚠️ Limitations

- Gesture calibration may vary across devices  
- No shuffle gesture implemented yet  
- No offline playback on watch  
- Limited ambient mode handling  

---

## 💡 Future Improvements

- 🎲 Shuffle gesture (gesture combinations)  
- 🔊 Volume control via wrist tilt  
- 🤖 Machine learning gesture recognition  
- 🎵 Streaming service integration  

---

## 👨‍💻 Author

Developed as part of an Android / Wear OS project focusing on:
- real-time systems  
- sensor interaction  
- wearable UI design  
