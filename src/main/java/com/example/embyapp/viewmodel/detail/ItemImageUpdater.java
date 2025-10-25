package com.example.embyapp.viewmodel.detail;

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
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Lớp helper xử lý logic nghiệp vụ cho việc upload và delete ảnh.
 * (CẬP NHẬT 30): Revert to using shared ImageServiceApi, assuming interceptor fix in EmbyService.
 */
public class ItemImageUpdater {

    private final EmbyService embyService;

    public ItemImageUpdater(EmbyService embyService) {
        this.embyService = embyService;
    }

    // Use shared API instance
    private ImageServiceApi getImageServiceApi() {
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
     * Upload một ảnh lên server.
     * (Use Base64 and pass mimeType parameter)
     */
    public void uploadImage(String itemId, ImageType imageType, File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IOException("File không tồn tại.");
        }

        ImageServiceApi api = getImageServiceApi(); // Use shared instance
        Path path = file.toPath();

        // 1. Đọc file -> Base64 String
        byte[] fileBytes = Files.readAllBytes(path);
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);

        // 2. Lấy MimeType
        String mimeType = Files.probeContentType(path);
        if (mimeType == null) {
            // Fallback
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".png")) mimeType = "image/png";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mimeType = "image/jpeg";
            else if (fileName.endsWith(".webp")) mimeType = "image/webp";
            else throw new IOException("Không thể xác định MimeType cho file: " + file.getName());
        }

        // 3. Gọi API: Pass base64Image as body AND the determined mimeType string
        System.out.println("Đang upload " + imageType + " (" + mimeType + ") cho item " + itemId + " (sử dụng shared ApiClient)");
        api.postItemsByIdImagesByType(base64Image, itemId, imageType, null, mimeType);
        System.out.println("Upload thành công.");
    }

    /**
     * Xóa một ảnh khỏi server. (Không đổi)
     */
    public void deleteImage(String itemId, ImageInfo imageInfo) throws Exception {
        if (imageInfo == null || imageInfo.getImageType() == null) {
            throw new IllegalArgumentException("ImageInfo không hợp lệ.");
        }

        ImageServiceApi api = getImageServiceApi(); // Use shared instance
        ImageType type = imageInfo.getImageType();
        Integer index = imageInfo.getImageIndex() == null ? 0 : imageInfo.getImageIndex();

        System.out.println("Đang xóa ảnh: " + type + ", index: " + index + " của item " + itemId);
        api.deleteItemsByIdImagesByTypeByIndex(itemId, type, index);
        System.out.println("Xóa thành công.");
    }
}