# Firebase Setup Guide cho Android App

## ⚠️ Quan Trọng

File `google-services.json` chứa Firebase configuration và **KHÔNG ĐƯỢC** commit vào git.

## 🔧 Setup Steps

### 1. Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Tạo project mới hoặc chọn project hiện có
3. Thêm Android app vào project

### 2. Cấu Hình Android App

Khi thêm Android app, bạn cần cung cấp:

- **Package name**: `com.example.dubaothoitiet` (hoặc package name của bạn)
- **App nickname**: Weather App (tùy chọn)
- **Debug signing certificate SHA-1**: (tùy chọn, cần cho Google Sign-In)

Để lấy SHA-1:
```bash
cd android
./gradlew signingReport
```

### 3. Download google-services.json

1. Sau khi tạo app, Firebase sẽ cho bạn download file `google-services.json`
2. Đặt file này vào thư mục `app/` của project:
   ```
   DuBaoThoiTiet/
   └── app/
       └── google-services.json  ← Đặt ở đây
   ```

### 4. Enable Firebase Services

Trong Firebase Console, enable các services sau:

#### Cloud Messaging (FCM)
- Vào **Cloud Messaging** trong Firebase Console
- Copy **Server Key** để dùng cho backend

#### Authentication (nếu cần)
- Vào **Authentication** > **Sign-in method**
- Enable các phương thức đăng nhập cần thiết

### 5. Cấu Hình Backend

Backend cần Firebase Admin SDK credentials:

1. Vào **Project Settings** > **Service Accounts**
2. Click **Generate new private key**
3. Download file JSON
4. Đổi tên thành `firebase-service-account.json`
5. Đặt vào thư mục `weather_project/`
6. **KHÔNG commit file này vào git!**

## 🔒 Security Checklist

- [ ] `google-services.json` đã được thêm vào `.gitignore`
- [ ] `firebase-service-account.json` đã được thêm vào `.gitignore`
- [ ] Không share credentials qua email/chat
- [ ] Sử dụng environment variables cho sensitive data
- [ ] Review code trước khi commit

## 📱 Testing FCM

Sau khi setup, test FCM bằng cách:

1. Chạy app trên thiết bị/emulator
2. App sẽ tự động lấy FCM token
3. Token được gửi lên backend
4. Backend có thể gửi test notification

## 🆘 Troubleshooting

### App không nhận được notifications

1. Kiểm tra `google-services.json` đã đúng package name
2. Kiểm tra Firebase project ID khớp với backend
3. Kiểm tra device token đã được đăng ký trên backend
4. Kiểm tra notification permissions đã được grant

### Build error: "google-services.json not found"

1. Đảm bảo file nằm đúng vị trí: `app/google-services.json`
2. Sync Gradle lại
3. Clean và rebuild project

## 📚 Resources

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [FCM Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Console](https://console.firebase.google.com/)
