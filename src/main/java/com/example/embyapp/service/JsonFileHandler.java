package com.example.embyapp.service;

import embyclient.model.BaseItemDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

// === IMPORT MỚI ĐỂ SỬA LỖI JSON ===
import java.time.OffsetDateTime;
import embyclient.JSON.OffsetDateTimeTypeAdapter;
// ===================================

public class JsonFileHandler {

    // === SỬA DÒNG KHỞI TẠO GSON NÀY ===
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter()) // <-- THÊM DÒNG NÀY
            .setPrettyPrinting()
            .create();
    // ==================================

    /**
     * Hiển thị cửa sổ MỞ file JSON.
     * @param ownerStage Stage hiện tại (để khóa)
     * @return File đã chọn, hoặc null nếu hủy.
     */
    public static File showOpenJsonDialog(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nManager.getInstance().getString("jsonFileHandler", "importTitle")); // <-- MODIFIED
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        );
        return fileChooser.showOpenDialog(ownerStage);
    }

    /**
     * Hiển thị cửa sổ LƯU file JSON.
     * @param ownerStage Stage hiện tại (để khóa)
     * @param initialFileName Tên file gợi ý (ví dụ: "item-name.json")
     * @return File đích (nơi lưu), hoặc null nếu hủy.
     */
    public static File showSaveJsonDialog(Stage ownerStage, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nManager.getInstance().getString("jsonFileHandler", "exportTitle")); // <-- MODIFIED
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        );
        return fileChooser.showSaveDialog(ownerStage);
    }

    /**
     * Đọc file JSON và chuyển đổi thành đối tượng (như bạn yêu cầu).
     */
    public static BaseItemDto readJsonFileToObject(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            // Giờ đây gson đã biết cách đọc chuỗi ngày tháng nhờ adapter
            return gson.fromJson(reader, BaseItemDto.class);
        }
    }

    /**
     * Ghi một đối tượng (BaseItemDto) ra file JSON.
     */
    public static void writeObjectToJsonFile(BaseItemDto object, File file) throws Exception {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(object, writer);
        }
    }
}