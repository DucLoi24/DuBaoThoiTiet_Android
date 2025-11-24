# Notification Database

Room Database cho hệ thống thông báo thời tiết nâng cao.

## Cấu trúc Database

### Entities

1. **NotificationPreferencesEntity**: Lưu trữ preferences thông báo của user
   - userId (Primary Key)
   - enabledEventTypes: Danh sách loại sự kiện được bật
   - notificationSchedule: Lịch trình thông báo ("24_7" hoặc "daytime_only")
   - morningSummaryEnabled: Bật/tắt tóm tắt buổi sáng
   - tomorrowForecastEnabled: Bật/tắt dự báo ngày mai
   - weeklySummaryEnabled: Bật/tắt tóm tắt tuần
   - timezone: Múi giờ của user
   - lastSyncedAt: Timestamp lần sync cuối

2. **LocationNotificationPreferencesEntity**: Preferences thông báo cho từng location
   - userId, locationId (Composite Primary Key)
   - notificationsEnabled: Bật/tắt thông báo cho location này
   - lastSyncedAt: Timestamp lần sync cuối

3. **NotificationRecordEntity**: Lịch sử thông báo đã nhận
   - id (Auto-generated Primary Key)
   - userId: ID của user
   - locationId: ID của location (nullable)
   - notificationType: Loại thông báo
   - title: Tiêu đề thông báo
   - body: Nội dung thông báo
   - priority: Mức độ ưu tiên
   - receivedAt: Timestamp nhận thông báo
   - data: Dữ liệu bổ sung (Map)
   - read: Đã đọc hay chưa

### DAOs

1. **NotificationPreferencesDao**: CRUD operations cho preferences
2. **LocationNotificationPreferencesDao**: CRUD operations cho location preferences
3. **NotificationRecordDao**: CRUD operations cho notification history

## Sử dụng

### Khởi tạo Database

```kotlin
val database = NotificationDatabase.getDatabase(context)
```

### Truy cập DAOs

```kotlin
val preferencesDao = database.notificationPreferencesDao()
val locationPreferencesDao = database.locationNotificationPreferencesDao()
val recordDao = database.notificationRecordDao()
```

### Ví dụ sử dụng

```kotlin
// Lưu preferences
val preferences = NotificationPreferencesEntity(
    userId = 1,
    enabledEventTypes = listOf("heavy_rain", "storm"),
    notificationSchedule = "24_7"
)
preferencesDao.insertPreferences(preferences)

// Lấy preferences
val userPreferences = preferencesDao.getPreferences(userId = 1)

// Observe preferences với Flow
preferencesDao.observePreferences(userId = 1).collect { prefs ->
    // Update UI
}

// Lưu notification record
val record = NotificationRecordEntity(
    userId = 1,
    locationId = 123,
    notificationType = "alert",
    title = "Cảnh báo mưa lớn",
    body = "Mưa lớn dự kiến trong 2 giờ tới",
    priority = "high",
    receivedAt = System.currentTimeMillis()
)
recordDao.insertRecord(record)
```

## Type Converters

Database sử dụng Gson để convert:
- `List<String>` ↔ JSON String
- `Map<String, String>` ↔ JSON String

## Migrations

Database hiện tại ở version 1. Khi cần thay đổi schema:

1. Tăng version number trong `@Database` annotation
2. Tạo Migration object trong `NotificationDatabase.companion`
3. Thêm migration vào `.addMigrations()` khi build database

Ví dụ:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE notification_preferences ADD COLUMN new_column TEXT")
    }
}
```

## Lưu ý

- Database sử dụng singleton pattern để đảm bảo chỉ có một instance
- Tất cả operations đều là suspend functions (trừ Flow observers)
- Sử dụng `fallbackToDestructiveMigration()` trong development (xóa và tạo lại DB khi schema thay đổi)
- Trong production, nên implement proper migrations
