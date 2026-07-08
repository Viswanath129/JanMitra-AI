# JanMitra AI 🇮🇳
### People's Voice. Intelligent Development.

> An offline-first AI-powered Android platform that empowers citizens to report local development issues and enables evidence-based constituency planning.

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Language](https://img.shields.io/badge/Kotlin-100%25-purple)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-orange)
![Database](https://img.shields.io/badge/Database-Room-success)
![Status](https://img.shields.io/badge/Release-v1.0.0-blue)

---
Live Demo Link : https://ai.studio/apps/654791f8-4f91-456b-952f-7b8bb99ae915
# 📌 Overview

JanMitra AI is an **offline-first Android application** designed to bridge the gap between citizens and public representatives.

Citizens can report issues related to:

- 🛣 Roads
- 💧 Water Supply
- ⚡ Electricity
- 🚑 Healthcare
- 🏫 Schools
- 🚰 Drainage
- 🌾 Agriculture
- 💡 Street Lights
- 🌐 Internet Connectivity
- 🧹 Sanitation
- 👮 Public Safety

using

- Voice
- Text
- GPS
- Photos
- Videos
- Documents

The application analyzes reports locally, stores them securely, and works even without an internet connection.

---

# ✨ Key Features

## 📱 Offline First

- Local Room Database
- Draft Saving
- Background Synchronization
- WorkManager
- No Internet Required

---

## 📍 Smart Location

- GPS Integration
- Current Location
- Manual Pin
- Google Maps
- Address Detection

---

## 🎤 Voice Reporting

- Speech-to-Text
- Local Language Support
- AI Summary
- Issue Classification

---

## 📷 Multimedia Evidence

Supports

- Images
- Videos
- Audio
- PDF Documents

---

## 🤖 AI Assisted Analysis

- Priority Analysis
- Issue Categorization
- Duplicate Detection
- Report Summarization

---

## 📄 PDF Report Generation

Generate professional reports directly on-device.

---

## 🔐 Security

- AES-GCM Encryption
- Android Keystore
- Secure Local Storage
- Runtime Permissions
- Safe Media Handling

---

## 📊 Tracking

Every report includes

- Unique Report ID
- Timeline
- Status Tracking
- Priority
- Evidence
- GPS Location

---

# 🏗 Architecture

```
Jetpack Compose
        │
        ▼
ViewModel
        │
        ▼
Repository
        │
        ▼
Room Database
        │
        ▼
DAO
```

Background Tasks

```
WorkManager
        │
        ▼
Pending Reports
        │
        ▼
Processing
        │
        ▼
Reported
```

---

# 📂 Tech Stack

## Android

- Kotlin
- Jetpack Compose
- Material Design 3
- MVVM
- Navigation Compose
- StateFlow
- Coroutines

## Storage

- Room Database
- SQLite
- DataStore Preferences

## Background

- WorkManager

## Maps

- Google Play Services Location
- Google Maps SDK

## Media

- CameraX
- MediaStore
- FileProvider
- PDFDocument API

## Security

- Android Keystore
- AES-GCM Encryption

## Testing

- Robolectric
- JUnit
- Compose UI Testing

---

# 🎯 Problem Statement

Citizens submit grievances through multiple disconnected channels, making it difficult for decision-makers to identify recurring issues and prioritize development work.

JanMitra AI consolidates citizen feedback into structured reports that can support data-driven planning.

---

# 🚀 Highlights

- Offline-first architecture
- Clean MVVM
- Material Design 3
- Responsive UI
- Secure Local Storage
- GPS Enabled
- Voice Reporting
- AI-Assisted Prioritization
- PDF Export
- Background Processing

---

# 📱 Screens

- Splash
- Login
- Home
- Report Wizard
- Maps
- AI Assistant
- Timeline
- PDF Export
- Profile

---

# 🔒 Current Scope

This repository focuses on a **fully functional offline-first Android application**.

Current release includes:

- Local Authentication (Demo)
- Offline AI Workflow
- Local Database
- Secure Storage
- GPS
- Media Capture
- Report Tracking

Future cloud integrations will include:

- Firebase Authentication
- Push Notifications
- Cloud Synchronization
- Administrative Dashboard

---

# 🛣 Roadmap

### Version 2

- Firebase Authentication
- Cloud Sync
- Push Notifications
- Analytics Dashboard

### Version 3

- AI Prediction
- GIS Heatmaps
- Computer Vision
- Multilingual LLM
- Decision Intelligence Dashboard

---

# 📸 Demo

Demo Video

> Add your YouTube or Drive link here

---

# 📄 Documentation

Detailed Engineering Report

> Add PDF link here

---

# 👨‍💻 Developer

**Viswanath**

Electronics & Communication Engineering Student

Passionate about AI, Android Development, Robotics, and Intelligent Civic Technologies.

GitHub:
https://github.com/Viswanath129

---

# 📜 License

This project was developed for educational, research, and hackathon purposes.
