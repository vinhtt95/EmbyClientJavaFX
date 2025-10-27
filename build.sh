#!/bin/bash
# Script để build ứng dụng EmbyClientJavaFX (tạo ra file "fat JAR")

echo "Bắt đầu build project..."

# 1. (Quan trọng) Cấp quyền thực thi cho Maven Wrapper (nếu chưa có)
chmod +x mvnw

# 2. Chạy lệnh build (clean và package)
# Lệnh này sẽ dùng maven-shade-plugin (đã cấu hình trong pom.xml)
# để tạo file .jar trong thư mục target/
./mvnw clean package

# 3. Kiểm tra kết quả
if [ $? -eq 0 ]; then
  echo "----------------------------------------------------"
  echo "✅ BUILD THÀNH CÔNG!"
  echo "File JAR đã được tạo tại: target/emby-javafx-client-1.0-SNAPSHOT.jar"
  echo "Mày có thể chạy app bằng cách gõ: ./run.sh"
else
  echo "----------------------------------------------------"
  echo "❌ BUILD THẤT BẠI!"
  echo "Vui lòng kiểm tra lỗi ở trên."
fi