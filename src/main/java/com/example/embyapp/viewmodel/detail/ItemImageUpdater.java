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
import com.example.embyapp.service.I18nManager; // <-- IMPORT
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
// (*** THÊM IMPORT Base64 ***)
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Lớp helper xử lý logic nghiệp vụ cho việc upload và delete ảnh.
 * (CẬP NHẬT 41 - SỬA LỖI 400 "MIME TYPE"):
 * - Gửi dữ liệu Base64 (đúng ý server từ lỗi 500).
 * - Đặt Content-Type là MimeType gốc (ví dụ: 'image/jpeg')
 * (để sửa lỗi 400 'text/plain').
 * (CẬP NHẬT 42 - SỬA LỖI BIÊN DỊCH):
 * - Sửa lỗi copy-paste 'FileLoopback' trong hàm 'chooseImages'.
 */
public class ItemImageUpdater {

    private final EmbyService embyService;
    private final I18nManager i18n; // <-- ADDED

    // Client riêng biệt cho upload (Giữ nguyên từ lần sửa trước)
    private OkHttpClient uploadHttpClient;

    public ItemImageUpdater(EmbyService embyService) {
        this.embyService = embyService;
        this.i18n = I18nManager.getInstance(); // <-- ADDED
    }

    // (Giữ nguyên từ lần sửa trước)
    private OkHttpClient getUploadHttpClient() {
        if (uploadHttpClient == null) {
            this.uploadHttpClient = new OkHttpClient();
            Interceptor authInterceptor = embyService.getAuthHeaderInterceptor();
            if (authInterceptor != null) {
                this.uploadHttpClient.interceptors().add(authInterceptor);
            } else {
                System.err.println("CẢNH BÁO: Không thể lấy AuthHeaderInterceptor từ EmbyService. Upload có thể thất bại.");
            }
        }
        return this.uploadHttpClient;
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
     * Mở FileChooser để chọn ảnh.
     * (*** ĐÃ SỬA LỖI BIÊN DỊCH TẠI ĐÂY ***)
     */
    public List<File> chooseImages(Stage ownerStage, boolean allowMultiple) {
        FileChooser fileChooser = new FileChooser();

        // <-- MODIFIED: Use I18nManager for titles -->
        fileChooser.setTitle(allowMultiple ?
                i18n.getString("itemImageUpdater", "selectBackdropsTitle") :
                i18n.getString("itemImageUpdater", "selectPrimaryTitle"));


        // (*** SỬA LỖI: Trả lại logic FileChooser.ExtensionFilter ***)
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

    // (*** HÀM UPLOAD (LOGIC ĐÚNG) - Giữ nguyên ***)
    /**
     * Upload một file ảnh lên server Emby bằng OkHttp.
     * @param itemId ID của item
     * @param imageType Loại ảnh (Primary, Backdrop, v.v.)
     * @param imageFile File ảnh trên máy
     * @throws IOException Nếu upload thất bại
     */
    public void uploadImage(String itemId, ImageType imageType, File imageFile) throws IOException {
        if (embyService.getApiClient() == null) {
            throw new IllegalStateException("ApiClient is null. Cannot upload.");
        }
        if (embyService.getCurrentAccessToken() == null) {
            throw new IllegalStateException("Not logged in. Cannot upload.");
        }

        // 1. Lấy OkHttpClient (client SẠCH, chỉ có auth)
        OkHttpClient client = getUploadHttpClient();

        // 2. Xác định URL (Giữ nguyên)
        String serverUrl = embyService.getApiClient().getBasePath();
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        String url = String.format("%s/Items/%s/Images/%s",
                serverUrl,
                itemId,
                imageType.getValue());

        // (*** SỬA LỖI: LOGIC MỚI - KẾT HỢP Base64 và MimeType gốc ***)

        // 3. Đọc tất cả byte của file
        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());

        // 4. Mã hóa sang Base64
        String base64String = Base64.getEncoder().encodeToString(fileBytes);

        // 5. Xác định MediaType gốc
        MediaType originalMediaType = getMediaType(imageFile);
        if (originalMediaType == null) {
            throw new IOException("Không hỗ trợ định dạng file ảnh: " + imageFile.getName());
        }

        // 6. Tạo RequestBody từ chuỗi Base64
        // NHƯNG khai báo Content-Type là MimeType GỐC.
        RequestBody body = RequestBody.create(originalMediaType, base64String);

        // 7. Build Request
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                // Content-Type đã được set bởi RequestBody.create()
                .build();

        // 8. Thực thi
        // System.out.println("Đang upload ảnh lên: " + url + " (dạng Base64, khai báo là " + originalMediaType + ")");
        Response response = client.newCall(request).execute();

        // 9. Kiểm tra kết quả
        if (!response.isSuccessful()) {
            String responseBody = response.body() != null ? response.body().string() : "No response body";
            response.body().close(); // Đóng body
            throw new IOException("Upload thất bại (Code " + response.code() + "): " + responseBody);
        } else {
            // System.out.println("Upload thành công.");
            response.body().close(); // Luôn đóng body
        }
    }

    // (*** HÀM NÀY GIỜ RẤT QUAN TRỌNG ***)
    private MediaType getMediaType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return MediaType.parse("image/png");
        if (name.endsWith(".jpg")) return MediaType.parse("image/jpeg");
        if (name.endsWith(".jpeg")) return MediaType.parse("image/jpeg");
        if (name.endsWith(".webp")) return MediaType.parse("image/webp");
        return null; // Không hỗ trợ
    }


    public void deleteImage(String itemId, ImageInfo imageInfo) throws Exception {
        if (imageInfo == null || imageInfo.getImageType() == null) {
            throw new IllegalArgumentException("ImageInfo không hợp lệ.");
        }

        // Dùng ApiService được chia sẻ (shared)
        ImageServiceApi api = getSharedImageServiceApi();
        ImageType type = imageInfo.getImageType();
        Integer index = imageInfo.getImageIndex() == null ? 0 : imageInfo.getImageIndex();

        // System.out.println("Đang xóa ảnh: " + type + ", index: " + index + " của item " + itemId);

        // Chữ ký hàm đúng: (String itemId, Integer index, ImageType type)
        api.deleteItemsByIdImagesByTypeByIndex(itemId, index, type);

        // System.out.println("Xóa thành công.");
    }
}