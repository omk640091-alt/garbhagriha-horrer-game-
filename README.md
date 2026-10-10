# 🪔 Garbhagriha: The Last Sanctum (3D First-Person Horror Game)

## 📱 Android APK & Play Store Build Guide

### 1. AI Studio से सीधे अपने मोबाइल में डाउनलोड कैसे करें:
- स्क्रीन के सबसे ऊपर दाईं तरफ (Top-Right) **Settings (⚙️)** या **Download / Export** आइकन पर टैप करें।
- **Generate APK** (या **Download APK**) चुनें — गेम सीधा आपके मोबाइल में `.apk` फ़ाइल के रूप में डाउनलोड हो जाएगा।

### 2. GitHub पर Push होने के बाद 1-Click Cloud APK कैसे बनाएँ (बिना कंप्यूटर के):
अगर आप GitHub पर ही क्लाउड से APK बनवाना चाहते हैं:
1. अपने GitHub रिपॉजिटरी में ऊपर **Actions** टैब पर टैप करें।
2. **"Set up a workflow yourself"** (या **Android CI**) चुनें और Commit कर दें — GitHub अपने आप आपके लिए `app-debug.apk` तैयार कर देगा!

### 3. Android Studio से Build करें:
```bash
./gradlew assembleDebug
```
APK पथ: `app/build/outputs/apk/debug/app-debug.apk`
