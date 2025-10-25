package com.example.embyapp.viewmodel.detail;

// (*** THÊM CÁC IMPORT NÀY ***)
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType; // <-- THÊM MỚI
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody; // <-- THÊM MỚI
import com.squareup.okhttp.Response;
import embyclient.ApiClient;
// (*** KẾT THÚC THÊM IMPORT ***)

import embyclient.api.ImageServiceApi;
import embyclient.model.ImageInfo;
import embyclient.model.ImageType;
import com.example.embyapp.service.EmbyService; // Correct import
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
// import java.util.Base64; // <-- (*** KHÔNG CẦN BASE64 NỮA ***)
import java.util.Collections;
import java.util.List;

/**
 * Lớp helper xử lý logic nghiệp vụ cho việc upload và delete ảnh.
 * (CẬP NHẬT 38 - SỬA LỖI 404 "DOUBLE SLASH"):
 * - Sửa lỗi: Xóa dấu / thừa khi xây dựng URL.
 */
public class ItemImageUpdater {

    private final EmbyService embyService;

    public ItemImageUpdater(EmbyService embyService) {
        this.embyService = embyService;
    }

    // (Vẫn dùng hàm này cho 'deleteImage')
    private ImageServiceApi getSharedImageServiceApi() {
        ImageServiceApi service = embyService.getImageServiceApi();
        if (service == null) {
            throw new IllegalStateException("Không thể lấy ImageServiceApi từ EmbyService.");
        }
        return service;
    }

    /**
     * Mở FileChooser để chọn ảnh. (Không đổi)
     */
    public List<File> chooseImages(Stage ownerStage, boolean allowMultiple) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(allowMultiple ? "Chọn các ảnh Backdrop" : "Chọn ảnh Primary");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                new FileChooser.ExtensionFilter("Tất cả file", "*.*")
        );

        if (allowMultiple) {
            List<File> files = fileChooser.showOpenMultipleDialog(ownerStage);
            return (files != null) ? files : Collections.emptyList();
        } else {
            File file = fileChooser.showOpenDialog(ownerStage);
            return (file != null) ? Collections.singletonList(file) : Collections.emptyList();
        }
    }

    /**
     * (*** SỬA LỖI: THAY ĐỔI URL ***)
     *
     * Upload một ảnh lên server.
     * (Sử dụng OkHttp thủ công)
     */
    public void uploadImage(String itemId, ImageType imageType, File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IOException("File không tồn tại.");
        }

        Path path = file.toPath();

        // 1. Lấy MimeType (QUAN TRỌNG)
        String mimeType = Files.probeContentType(path);
        if (mimeType == null) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".png")) mimeType = "image/png";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mimeType = "image/jpeg";
            else if (fileName.endsWith(".webp")) mimeType = "image/webp";
            else throw new IOException("Không thể xác định MimeType cho file: " + file.getName());
        }

        // 2. Đọc file -> byte[] thô
        byte[] fileBytes = Files.readAllBytes(path);

        // 3. Lấy ApiClient GỐC từ EmbyService
        ApiClient baseClient = embyService.getApiClient();
        if (baseClient == null) {
            throw new IllegalStateException("ApiClient gốc bị null.");
        }

        // 4. Lấy OkHttpClient ĐÃ XÁC THỰC (đã có Interceptor)
        OkHttpClient authenticatedClient = baseClient.getHttpClient();

        // 5. (*** SỬA LỖI XÂY DỰNG URL (BỎ DẤU / Ở ĐẦU) ***)
        String serverUrl = baseClient.getBasePath();
        // serverUrl (getBasePath()) đã bao gồm dấu / ở cuối (ví dụ: "http://host:port/")
        // Bắt đầu chuỗi format bằng "Items/" thay vì "/Items/"
        String url = String.format("%sItems/%s/Images?type=%s",
                serverUrl,
                itemId,
                imageType.getValue());

        // 6. Tạo RequestBody (Giữ nguyên - gửi byte thô)
        MediaType mediaType = MediaType.parse(mimeType);
        RequestBody body = RequestBody.create(mediaType, fileBytes);

        // 7. Tạo Request (Giữ nguyên)
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // 8. Thực thi request thủ công
        System.out.println("Đang upload " + imageType + " (" + mimeType + ") đến URL: " + url + " (sử dụng OkHttp thủ công + byte[])");

        Response response = authenticatedClient.newCall(request).execute();

        // 9. Kiểm tra kết quả
        if (!response.isSuccessful()) {
            // Ném lỗi với thông báo từ server
            String responseBody = response.body() != null ? response.body().string() : "Không có body";
            throw new IOException("Lỗi upload: " + response.code() + " " + response.message() + " - " + responseBody);
        }

        System.out.println("Upload thành công.");

        // Đóng response body an toàn
        if (response.body() != null) {
            response.body().close();
        }
    }

    /**
     * Xóa một ảnh khỏi server.
     * (Hàm này không lỗi, dùng ApiService chung)
     */
    public void deleteImage(String itemId, ImageInfo imageInfo) throws Exception {
        if (imageInfo == null || imageInfo.getImageType() == null) {
            throw new IllegalArgumentException("ImageInfo không hợp lệ.");
        }

        // Dùng ApiService được chia sẻ (shared)
        ImageServiceApi api = getSharedImageServiceApi();
        ImageType type = imageInfo.getImageType();
        Integer index = imageInfo.getImageIndex() == null ? 0 : imageInfo.getImageIndex();

        System.out.println("Đang xóa ảnh: " + type + ", index: " + index + " của item " + itemId);

        // Chữ ký hàm đúng: (String itemId, Integer index, ImageType type)
        api.deleteItemsByIdImagesByTypeByIndex(itemId, index, type);

        System.out.println("Xóa thành công.");
    }
}