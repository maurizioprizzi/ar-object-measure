# 📱 AR Object Measure

> **Android AR app for object measurement using Computer Vision and Machine Learning**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-24%20Passing-brightgreen.svg)](#testing-strategy)
[![Coverage](https://img.shields.io/badge/Coverage-100%25-brightgreen.svg)](#testing-strategy)

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

# Run domain layer tests specifically
./gradlew test --tests "com.objectmeasure.ar.domain.*"
```

## 🏗️ Architecture

This project follows **Clean Architecture** principles with **MVVM** pattern:

```
app/
├── presentation/     # UI Layer (Activities, Fragments, ViewModels)
│   ├── ui/          # Compose screens
│   └── viewmodel/   # ViewModels
├── domain/ ✅       # Business Logic Layer [IMPLEMENTED]
│   ├── model/       # Entities (DetectedObject, Measurements)
│   ├── repository/  # Repository interfaces
│   └── usecase/     # Use cases (ValidateObjectUseCase)
├── data/            # Data Layer [NEXT: Day 3]
│   ├── repository/  # Repository implementations
│   ├── datasource/  # Data sources (local/remote)
│   └── mapper/      # Data mappers
└── core/            # Shared utilities
    └── util/        # Extension functions, constants
```

### **Domain Layer (Complete ✅)**
The domain layer is fully implemented with:
- **5 domain models** with comprehensive validation
- **Repository interfaces** defining data contracts
- **Use cases** encapsulating business logic
- **19 unit tests** with 100% coverage

> 📖 **Architecture Details:** See [ARCHITECTURE.md](ARCHITECTURE.md) for comprehensive design patterns documentation

## 📊 Development Status

- **Current Phase:** Domain Layer Complete ✅
- **Progress:** 7% (Day 2 of 30 completed)
- **Next Milestone:** Data Layer Implementation

### **Roadmap**
- [x] **Day 1:** Clean Architecture Foundation ✅
- [x] **Day 2:** Domain Layer (Models, Use Cases, Repository Interfaces) ✅
- [ ] **Day 3-4:** Data Layer Implementation  
- [ ] **Day 5-7:** Presentation Layer + Basic UI
- [ ] **Week 2:** Camera integration + ARCore setup
- [ ] **Week 3:** ML Kit integration + Object detection
- [ ] **Week 4:** Measurement algorithms + UI polish

### **Architecture Progress**
- **✅ Domain Layer:** 100% complete (5 classes, 19 tests)
- **🔄 Data Layer:** 0% (starting Day 3)
- **🔄 Presentation Layer:** 0% (starting Day 4)

> 📝 **Detailed progress:** See [DIARY.md](DIARY.md) for daily development log

## 🧪 Testing Strategy

- **Unit Tests:** Business logic and utilities ✅
- **Integration Tests:** Repository and use case layers (Day 3)
- **UI Tests:** Compose components and user flows (Day 4+)
- **AR Tests:** ARCore functionality and measurements (Week 2+)

### **Current Testing Status**
- **Total Tests:** 24 (5 core utils + 19 domain layer)
- **Coverage:** 100% for implemented layers
- **Build Status:** All tests passing ✅
- **Coverage Goal:** >90% for all layers

## 📱 Measurement Accuracy

*Target accuracy levels (to be validated during development):*
- **Height:** ±2cm for objects, ±5cm for people
- **Distance:** ±5cm up to 10 meters
- **Weight:** ±10% estimation accuracy
- **Angles:** ±2° precision

## 🔬 Domain Model Overview

The application recognizes and measures the following object types:
- **👤 Person** - Height, posture, distance measurement
- **🍼 Bottle** - Height, volume-based weight estimation
- **📱 Phone** - Dimensions and distance
- **📚 Book** - Height and thickness
- **🪑 Chair/Table** - Furniture dimensions

Each measurement includes confidence scoring and validation rules implemented in the domain layer.

## 🤝 Contributing

This is a learning/portfolio project, but suggestions and feedback are welcome!

1. **Fork** the project
2. **Create** your feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

## 📚 Documentation

- **Development Diary:** [DIARY.md](DIARY.md) - Daily progress and learnings
- **Architecture Guide:** [ARCHITECTURE.md](ARCHITECTURE.md) - Design patterns and technical decisions
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