# Hướng dẫn Setup Firebase cho Android

## Bước 1: Thêm google-services.json

1. Copy file `google-services.json` (đã tải từ Firebase Console)
2. Paste vào thư mục: `app/` (cùng cấp với `build.gradle.kts`)

## Bước 2: Cập nhật build.gradle files

### File `build.gradle.kts` (Project level)

Thêm vào `plugins`:
```kotlin
id("com.google.gms.google-services") version "4.4.0" apply false
```

### File `app/build.gradle.kts` (App level)

Thêm vào cuối file `plugins`:
```kotlin
id("com.google.gms.google-services")
```

Thêm vào `dependencies`:
```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")
```

## Bước 3: Sync Gradle

Click "Sync Now" trong Android Studio

## Bước 4: Code đã được tạo sẵn!

Tôi đã tạo sẵn các file:
- `FirebaseMessagingService.kt` - Nhận notifications
- `NotificationHelper.kt` - Hiển thị notifications
- Cập nhật `MainActivity.kt` - Đăng ký device token

## Bước 5: Thêm permissions vào AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
```

Thêm service vào trong `<application>`:
```xml
<service
    android:name=".service.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## Bước 6: Build và chạy app!

Khi app khởi động, nó sẽ tự động:
1. Lấy FCM token
2. Gửi token lên server Django
3. Nhận push notifications khi thời tiết thay đổi!

---

## Test thử

1. Theo dõi một vị trí trong app
2. Đợi 1-2 phút
3. Nếu thời tiết thay đổi, bạn sẽ nhận được notification!
