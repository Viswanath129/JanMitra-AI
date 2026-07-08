**JanMitra AI — People's Voice. Intelligent Development.**

View your app in AI Studio: https://ai.studio/apps/654791f8-4f91-456b-952f-7b8bb99ae915
<div align="center">

# 🇮🇳 JanMitra AI

### People's Voice. Intelligent Development.

*An AI-powered citizen engagement and development intelligence platform enabling multilingual issue reporting, geospatial analysis, and data-driven public infrastructure planning.*

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blueviolet)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)
![FastAPI](https://img.shields.io/badge/FastAPI-Backend-009688)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-336791)
![Gemini AI](https://img.shields.io/badge/Gemini-AI-4285F4)
![License](https://img.shields.io/badge/License-MIT-green)

</div>

---

# 📌 Overview

JanMitra AI is an AI-powered citizen participation platform that transforms how public infrastructure issues are reported, analyzed, and prioritized.

Instead of functioning as a traditional grievance portal, JanMitra AI combines:

- 🤖 Artificial Intelligence
- 🌍 Geospatial Intelligence (GIS)
- 🎤 Voice-first reporting
- 📸 Multimedia evidence
- 📍 GPS-based issue mapping
- 📊 Data-driven development insights

to help build smarter, more responsive communities.

---

# 🚀 Features

## 📱 Citizen Reporting

- Voice Reporting
- Text Reporting
- Photo Upload
- Video Upload
- GPS Location
- Google Maps Integration
- Offline Draft Saving
- AI-generated Issue Summary
- Timeline Tracking

---

## 🤖 AI Intelligence

- Gemini AI Integration
- Automatic Issue Classification
- Multilingual Support
- AI Summarization
- Duplicate Detection
- Priority Recommendation
- Explainable AI Responses

---

## 📍 Maps & GIS

- Google Maps SDK
- Current GPS
- Marker Selection
- Reverse Geocoding
- Village Identification
- Address Resolution

---

## 📂 Media Support

- CameraX
- Gallery Picker
- Video Upload
- Image Compression
- Media Preview

---

## 🔐 Authentication

- Firebase Authentication
- Google Sign-In
- Phone Authentication
- Anonymous Login

---

## ☁ Backend

- FastAPI
- REST APIs
- PostgreSQL
- PostGIS
- JWT Authentication
- Docker Deployment

---

## 📶 Offline First

- Room Database
- WorkManager Sync
- Retry Queue
- Automatic Synchronization

---

## 🔔 Notifications

- Firebase Cloud Messaging
- Status Updates
- AI Analysis Complete
- Report Progress Tracking

---

# 🏗 Architecture

```text
                ┌──────────────────────────────┐
                │      Android Application     │
                │                              │
                │  Jetpack Compose + MVVM      │
                └──────────────┬───────────────┘
                               │
                          HTTPS / REST
                               │
                ┌──────────────▼──────────────┐
                │        FastAPI Backend      │
                └───────┬───────────┬─────────┘
                        │           │
                  Gemini AI     PostgreSQL
                        │           │
                  AI Engine     PostGIS
                        │
                  Firebase Storage
```

---

# 📱 Screens

- Splash
- Authentication
- Home
- Report Issue
- Google Maps
- AI Chat
- Timeline
- Notifications
- Profile
- Settings

---

# 🛠 Tech Stack

## Android

- Kotlin
- Jetpack Compose
- Material Design 3
- MVVM
- StateFlow
- Room
- Retrofit
- Moshi
- CameraX
- Google Maps SDK
- WorkManager

---

## Backend

- FastAPI
- SQLAlchemy
- PostgreSQL
- PostGIS
- JWT
- Docker
- Nginx

---

## AI

- Google Gemini
- Structured JSON
- AI Summarization
- Issue Classification
- Duplicate Detection

---

## Cloud

- Firebase Authentication
- Firebase Storage
- Firebase Cloud Messaging

---

# 📂 Project Structure

```
JanMitra-AI/
│
├── app/
│   ├── data/
│   ├── ui/
│   ├── viewmodel/
│   ├── repository/
│   ├── network/
│   └── worker/
│
├── server/
│   ├── auth.py
│   ├── ai_service.py
│   ├── database.py
│   ├── routes/
│   └── main.py
│
├── docker/
│
├── docs/
│
└── README.md
```

---

# 🔒 Security

- JWT Authentication
- Role-Based Access Control
- HTTPS
- Secure API Communication
- Offline Data Protection
- Permission Handling
- Input Validation

---

# 🌍 Future Enhancements

- Satellite Imagery Analysis
- Computer Vision Road Detection
- Predictive Infrastructure Planning
- RAG Knowledge Base
- WhatsApp Reporting
- Multi-language Voice Assistant
- Government API Integration

---

# 🚀 Getting Started

Clone the repository

```bash
git clone https://github.com/yourusername/JanMitra-AI.git
```

Open the Android project

```bash
Android Studio
```

Run the backend

```bash
cd server
pip install -r requirements.txt
uvicorn main:app --reload
```

---

# 🤝 Contributing

Contributions are welcome.

1. Fork the repository
2. Create a new branch
3. Commit your changes
4. Push to your fork
5. Open a Pull Request

---

# 📄 License

This project is licensed under the MIT License.

---

<div align="center">

## 🇮🇳 Built to empower citizens through AI-driven governance.

**JanMitra AI — People's Voice. Intelligent Development.**

</div>
