# 📱 AR Object Measure

> **Android AR app for object measurement using Computer Vision and Machine Learning**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 🎯 Overview

AR Object Measure is an innovative Android application that uses **Augmented Reality**, **Computer Vision**, and **Machine Learning** to measure real-world objects and people through your smartphone camera.

### ✨ Key Features

- 📏 **Height Measurement** - Measure the height of objects and people
- ⚖️ **Weight Estimation** - Estimate weight based on volume and material density  
- 📍 **Distance Calculation** - Calculate distance from camera to target
- 📐 **Inclination Angle** - Measure object tilt and human posture
- 🔍 **Auto Detection** - Automatic recognition of objects and humans
- 📱 **Real-time AR Interface** - Live measurements with AR overlay

## 🚀 Demo

*Coming soon - Screenshots and demo videos will be added as development progresses*

## 🛠️ Technology Stack

### **Core Technologies**
- **Language:** Kotlin
- **Architecture:** Clean Architecture + MVVM
- **UI Framework:** Jetpack Compose + Material 3
- **Build System:** Gradle KTS + Version Catalog

### **AR & Computer Vision**
- **ARCore** - Google's AR platform for Android
- **ML Kit** - Google's machine learning SDK
- **TensorFlow Lite** - On-device machine learning
- **OpenCV** - Computer vision library
- **MediaPipe** - Real-time pose estimation

### **Android Libraries**
- **CameraX** - Modern camera API
- **Room** - Local database
- **Hilt** - Dependency injection
- **Coroutines + Flow** - Asynchronous programming
- **Retrofit** - Network requests

## 📋 Requirements

- **Android 7.0** (API level 24) or higher
- **ARCore supported device** - [Check compatibility](https://developers.google.com/ar/devices)
- **Camera permission** - Required for AR functionality
- **Storage permission** - For saving measurements (optional)

## 🔧 Setup & Installation

### **Prerequisites**
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK 34

### **Clone & Build**
```bash
# Clone the repository
git clone https://github.com/maurizioprizzi/ar-object-measure.git

# Navigate to project directory
cd ar-object-measure

# Open in Android Studio
# OR build from command line:
./gradlew assembleDebug
```

### **Run Tests**
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests
./gradlew connectedDebugAndroidTest
```

## 🏗️ Architecture

This project follows **Clean Architecture** principles with **MVVM** pattern:

```
app/
├── presentation/     # UI Layer (Activities, Fragments, ViewModels)
│   ├── ui/          # Compose screens
│   └── viewmodel/   # ViewModels
├── domain/          # Business Logic Layer
│   ├── model/       # Entities
│   ├── repository/  # Repository interfaces
│   └── usecase/     # Use cases
├── data/            # Data Layer
│   ├── repository/  # Repository implementations
│   ├── datasource/  # Data sources (local/remote)
│   └── mapper/      # Data mappers
└── core/            # Shared utilities
    └── util/        # Extension functions, constants
```

## 📊 Development Status

- **Current Phase:** Foundation & Setup ✅
- **Progress:** 3% (Day 1 of 30 completed)
- **Next Milestone:** Domain Layer Implementation

### **Roadmap**
- [x] **Week 1:** Clean Architecture + Basic AR setup
- [ ] **Week 2:** Core measurement features  
- [ ] **Week 3:** Human detection & measurement
- [ ] **Week 4:** UI polish & optimization

> 📝 **Detailed progress:** See [DIARY.md](DIARY.md) for daily development log

## 🧪 Testing Strategy

- **Unit Tests:** Business logic and utilities
- **Integration Tests:** Repository and use case layers  
- **UI Tests:** Compose components and user flows
- **AR Tests:** ARCore functionality and measurements

**Coverage Goal:** >90% for all layers

## 📱 Measurement Accuracy

*Target accuracy levels (to be validated during development):*
- **Height:** ±2cm for objects, ±5cm for people
- **Distance:** ±5cm up to 10 meters
- **Weight:** ±10% estimation accuracy
- **Angles:** ±2° precision

## 🤝 Contributing

This is a learning/portfolio project, but suggestions and feedback are welcome!

1. **Fork** the project
2. **Create** your feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

## 📚 Documentation

- **Development Diary:** [DIARY.md](DIARY.md) - Daily progress and learnings
- **Architecture Guide:** *Coming soon*
- **API Documentation:** *Coming soon*

## 🔗 References & Inspiration

- [ARCore Documentation](https://developers.google.com/ar)
- [ML Kit Pose Detection](https://developers.google.com/ml-kit/vision/pose-detection)
- [Clean Architecture Guide](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android Architecture Components](https://developer.android.com/topic/architecture)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Maurizio Prizzi**
- GitHub: [@maurizioprizzi](https://github.com/maurizioprizzi)
- Email: maurizioprizzi@gmail.com

---

<p align="center">
  <i>Built with ❤️ using Clean Architecture, TDD, and modern Android development practices</i>
</p>