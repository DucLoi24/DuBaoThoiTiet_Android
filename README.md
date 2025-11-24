# Ứng dụng Dự báo Thời tiết - Android

<div align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase" />
</div>

## 📱 Giới thiệu

Ứng dụng Android Dự báo Thời tiết cung cấp thông tin thời tiết chi tiết, cảnh báo thiên tai, và tư vấn AI về thời tiết. Ứng dụng được xây dựng bằng Kotlin với Jetpack Compose, tuân theo kiến trúc MVVM và Clean Architecture principles.

### ✨ Tính năng chính

- 🌤️ **Dự báo thời tiết chi tiết**: Thông tin thời tiết hiện tại, dự báo theo giờ và theo ngày
- 📊 **Biểu đồ trực quan**: Biểu đồ Rain, UV Index, và Air Quality Index
- 🚨 **Cảnh báo thời tiết**: Nhận cảnh báo về thời tiết nguy hiểm (mưa lớn, bão, nhiệt độ cực đoan)
- 🤖 **Tư vấn AI**: Lời khuyên thông minh dựa trên điều kiện thời tiết
- 📍 **Theo dõi nhiều vị trí**: Quản lý và theo dõi thời tiết tại nhiều địa điểm
- 🔔 **Thông báo Push**: Nhận thông báo real-time qua Firebase Cloud Messaging
- ⚙️ **Cài đặt linh hoạt**: Tùy chỉnh loại thông báo, lịch trình, và preferences cho từng vị trí
- 📜 **Lịch sử thông báo**: Xem lại tất cả thông báo đã nhận
- 📶 **Offline Support**: Hoạt động offline với dữ liệu được cache local
- 🔄 **Auto-sync**: Tự động đồng bộ khi có mạng trở lại

---

## 🏗️ Kiến trúc

Ứng dụng được xây dựng theo **MVVM (Model-View-ViewModel)** kết hợp với **Clean Architecture**:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI - Jetpack Compose + ViewModels)    │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Domain Layer                   │
│    (Business Logic + Use Cases)         │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│           Data Layer                    │
│  (Repositories + API + Database)        │
└─────────────────────────────────────────┘
```

### Công nghệ sử dụng

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| **Kotlin** | 2.0.21 | Ngôn ngữ chính |
| **Jetpack Compose** | BOM latest | UI framework hiện đại |
| **Retrofit** | 2.9.0 | HTTP client |
| **Room** | 2.6.1 | Local database |
| **Firebase Messaging** | 32.7.0 | Push notifications |
| **Coil** | 2.6.0 | Image loading |
| **Navigation Compose** | 2.8.0-beta05 | Navigation |
| **Coroutines** | - | Async operations |

---

## 🚀 Quick Start

### Yêu cầu hệ thống

- **Android Studio**: Hedgehog (2023.1.1) hoặc mới hơn
- **JDK**: Java 11
- **Android SDK**: Min API 24 (Android 7.0), Target API 36
- **Gradle**: 8.x

### Cài đặt

#### 1. Clone repository

```bash
git clone <repository-url>
cd DuBaoThoiTiet
```

#### 2. Cấu hình Firebase

Xem hướng dẫn chi tiết tại: [FIREBASE_SETUP_GUIDE.md](FIREBASE_SETUP_GUIDE.md)

**Tóm tắt:**
1. Tạo Firebase project tại [Firebase Console](https://console.firebase.google.com/)
2. Thêm Android app với package name: `com.example.dubaothoitiet`
3. Download file `google-services.json`
4. Đặt file vào thư mục `app/`
5. Enable Firebase Cloud Messaging trong Firebase Console

⚠️ **Lưu ý**: File `google-services.json` đã được thêm vào `.gitignore` và không được commit lên git.

#### 3. Cấu hình Backend

Đảm bảo backend Django đang chạy tại `http://127.0.0.1:8000/` (hoặc cập nhật base URL trong code).

Xem hướng dẫn setup backend tại: [../weather_project/README.md](../weather_project/README.md)

#### 4. Build và chạy

```bash
# Sync Gradle
./gradlew sync

# Build debug APK
./gradlew assembleDebug

# Install và chạy trên device/emulator
./gradlew installDebug
```

Hoặc sử dụng Android Studio:
1. Mở project trong Android Studio
2. Sync Gradle files
3. Click "Run" (Shift + F10)

---

## 📖 Hướng dẫn sử dụng

### Đăng nhập / Đăng ký

1. Mở ứng dụng
2. Click icon tài khoản ở góc trên bên phải
3. Chọn tab "Đăng nhập" hoặc "Đăng ký"
4. Nhập thông tin và submit

### Xem thời tiết

1. Nhập tên thành phố vào ô tìm kiếm
2. Hoặc click icon "Vị trí hiện tại" để lấy thời tiết tại vị trí của bạn
3. Xem thông tin chi tiết:
   - Nhiệt độ hiện tại và điều kiện
   - Dự báo theo giờ (24 giờ)
   - Dự báo theo ngày (3 ngày)
   - Biểu đồ Rain, UV, AQI
   - Cảnh báo thời tiết (nếu có)
   - Tư vấn AI (nếu có)

### Theo dõi vị trí

1. Tìm kiếm thành phố muốn theo dõi
2. Click nút "Theo dõi vị trí này"
3. Vị trí sẽ được thêm vào danh sách theo dõi
4. Xem danh sách: Menu → "Các vị trí đã theo dõi"

### Cài đặt thông báo

#### Cài đặt chung:
1. Menu → "Cài đặt thông báo"
2. Bật/tắt thông báo tổng thể
3. Chọn loại cảnh báo muốn nhận:
   - Lũ lụt và Mưa
   - Bão và Gió
   - Nhiệt độ
   - Sức khỏe (UV & AQI)
   - Cảnh báo chính thức
4. Chọn lịch trình:
   - Luôn luôn
   - Chỉ ban ngày (6:00 - 22:00)
   - Chỉ khi quan trọng
5. Bật/tắt thông báo định kỳ:
   - Tóm tắt buổi sáng (7:00 AM)
   - Dự báo ngày mai (8:00 PM)
   - Tóm tắt tuần (8:00 PM Chủ nhật)

#### Cài đặt cho từng vị trí:
1. Vào "Các vị trí đã theo dõi"
2. Click icon settings trên location card
3. Bật/tắt thông báo cho vị trí đó

### Xem lịch sử thông báo

1. Menu → "Lịch sử thông báo"
2. Xem danh sách thông báo đã nhận
3. Click vào notification để xem chi tiết
4. Sử dụng filter để lọc theo:
   - Loại thông báo
   - Khoảng thời gian

---

## 📂 Cấu trúc Project

```
DuBaoThoiTiet/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/dubaothoitiet/
│   │   │   │   ├── data/              # Data layer
│   │   │   │   │   ├── models/        # Data models
│   │   │   │   │   ├── api/           # Retrofit API
│   │   │   │   │   ├── database/      # Room database
│   │   │   │   │   └── repositories/  # Repositories
│   │   │   │   ├── ui/                # UI screens
│   │   │   │   │   ├── theme/         # Material theme
│   │   │   │   │   └── *.kt           # Composable screens
│   │   │   │   ├── viewmodel/         # ViewModels
│   │   │   │   ├── service/           # Services
│   │   │   │   │   ├── MyFirebaseMessagingService.kt
│   │   │   │   │   ├── WeatherNotificationService.kt
│   │   │   │   │   ├── NotificationChannelManager.kt
│   │   │   │   │   └── FirebaseTokenManager.kt
│   │   │   │   ├── MainActivity.kt    # Main activity
│   │   │   │   └── WeatherApplication.kt  # Application class
│   │   │   ├── res/                   # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                      # Unit tests
│   ├── build.gradle.kts               # App build config
│   └── google-services.json           # Firebase config (gitignored)
├── docs/                              # Documentation
│   ├── SYSTEM_DESIGN.md
│   ├── UI_DOCUMENTATION.md
│   ├── NAVIGATION.md
│   └── SERVICES.md
├── build.gradle.kts                   # Project build config
├── settings.gradle.kts
├── FIREBASE_SETUP_GUIDE.md
├── FIREBASE_ANDROID_SETUP.md
└── README.md
```

---

## 🔔 Push Notifications

Ứng dụng sử dụng Firebase Cloud Messaging (FCM) để nhận push notifications.

### Loại thông báo

1. **Cảnh báo thời tiết** (`alert`)
   - Priority: HIGH
   - Vibration: Có
   - Sound: Có
   - Bypass DND: Có
   - Ví dụ: Mưa lớn, bão, nhiệt độ cực đoan

2. **Tóm tắt buổi sáng** (`morning_summary`)
   - Priority: MEDIUM
   - Thời gian: 7:00 AM
   - Nội dung: Tóm tắt thời tiết trong ngày

3. **Dự báo ngày mai** (`tomorrow_forecast`)
   - Priority: MEDIUM
   - Thời gian: 8:00 PM
   - Nội dung: Dự báo thời tiết ngày mai

4. **Tóm tắt tuần** (`weekly_summary`)
   - Priority: LOW
   - Thời gian: 8:00 PM Chủ nhật
   - Nội dung: Tóm tắt thời tiết tuần qua

### Notification Channels

- **Cảnh báo khẩn cấp**: High importance, sound + vibration
- **Tóm tắt định kỳ**: Default importance, sound only
- **Thông báo chung**: Low importance, silent

---

## 🧪 Testing

### Chạy Unit Tests

```bash
./gradlew test
```

### Chạy Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

### Test Coverage

Xem chi tiết tại: [docs/TESTING.md](docs/TESTING.md)

---

## 📚 Documentation

Tài liệu chi tiết về từng phần của ứng dụng:

- **[System Design](docs/SYSTEM_DESIGN.md)**: Kiến trúc tổng thể, data flow, components
- **[UI Documentation](docs/UI_DOCUMENTATION.md)**: Tất cả screens và UI components
- **[Navigation](docs/NAVIGATION.md)**: Navigation flow và deep linking
- **[Services](docs/SERVICES.md)**: Background services, FCM, notifications
- **[Data Layer](docs/DATA_LAYER.md)**: Repositories, API, database (coming soon)
- **[Project Structure](docs/PROJECT_STRUCTURE.md)**: Cấu trúc thư mục chi tiết (coming soon)
- **[Testing](docs/TESTING.md)**: Testing strategy và test cases (coming soon)

---

## 🔧 Configuration

### API Endpoint

Mặc định: `http://127.0.0.1:8000/`

Để thay đổi, cập nhật trong `WeatherApiService.kt`:

```kotlin
private const val BASE_URL = "http://your-backend-url:8000/"
```

### Notification Settings

Cấu hình trong `NotificationChannelManager.kt`:
- Channel IDs
- Importance levels
- Sound và vibration settings

### Database

Room database schema location: `app/schemas/`

---

## 🐛 Troubleshooting

### Không nhận được notifications

1. Kiểm tra Firebase configuration (`google-services.json`)
2. Verify FCM token đã được gửi lên backend
3. Check notification permissions đã được grant
4. Kiểm tra notification channels chưa bị disable

### Build errors

1. Clean project: `./gradlew clean`
2. Sync Gradle: `./gradlew sync`
3. Invalidate caches: File → Invalidate Caches / Restart

### Location permission

Đảm bảo đã grant location permission trong Settings → Apps → Weather App → Permissions

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👥 Authors

- **Your Name** - Initial work

---

## 🙏 Acknowledgments

- [OpenWeatherMap API](https://openweathermap.org/) - Weather data provider
- [Firebase](https://firebase.google.com/) - Push notifications
- [Material Design 3](https://m3.material.io/) - Design system
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI framework

---

## 📞 Support

Nếu bạn gặp vấn đề hoặc có câu hỏi, vui lòng:
- Mở issue trên GitHub
- Liên hệ qua email: your-email@example.com

---

**Made with ❤️ using Kotlin and Jetpack Compose**
