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