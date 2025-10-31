package com.example.embyapp.viewmodel.detail;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Objects;

/**
 * (MỚI) Lớp Model đại diện cho một TagItem đã được phân tích.
 * Nó có thể là một chuỗi đơn giản, hoặc một cặp Key-Value từ JSON.
 * (CẬP NHẬT) Thêm trường 'id' để lưu ID gốc (nếu có).
 * (SỬA LỖI) Xóa các constructor 1-arg và 2-arg bị trùng lặp.
 */
public class TagModel {

    private static final Gson gson = new Gson();

    private final boolean isJson;
    private final String simpleName;
    private final String key;
    private final String value;
    private final String id; // <-- Trường ID

    /*
     * (XÓA BỎ) Constructor (String simpleName)
     * Đã bị xóa để tránh nhầm lẫn. Sử dụng parse(simpleName)
     * hoặc new TagModel(simpleName, null)
     */

    /*
     * (XÓA BỎ) Constructor (String key, String value)
     * Đã bị xóa do trùng lặp chữ ký với (String simpleName, String id)
     * Sử dụng parse(jsonString) hoặc new TagModel(key, value, null)
     */

    /**
     * Constructor đầy đủ cho tag chuỗi đơn giản với ID.
     */
    public TagModel(String simpleName, String id) {
        this.isJson = false;
        this.simpleName = simpleName;
        this.key = null;
        this.value = null;
        this.id = id;
    }

    /**
     * Constructor đầy đủ cho tag Key-Value (từ JSON) với ID.
     */
    public TagModel(String key, String value, String id) {
        this.isJson = true;
        this.simpleName = null;
        this.key = key;
        this.value = value;
        this.id = id;
    }

    /**
     * Phân tích một chuỗi 'Name' (từ NameLongIdPair) thành một TagModel.
     * Tự động phát hiện JSON.
     */
    public static TagModel parse(String rawName) {
        return parse(rawName, null); // Gọi hàm parse đầy đủ
    }

    /**
     * (MỚI) Phân tích một chuỗi 'Name' và lưu trữ ID.
     */
    public static TagModel parse(String rawName, String id) {
        if (rawName == null || rawName.isEmpty()) {
            return new TagModel("Trống", id); // <-- SỬA ĐỔI
        }

        // Kiểm tra xem có phải là chuỗi JSON thô hay không
        if (rawName.startsWith("{") && rawName.endsWith("}")) {
            try {
                // Thử parse JSON
                JsonObject jsonObject = gson.fromJson(rawName, JsonObject.class);

                // Lấy entry ĐẦU TIÊN (theo yêu cầu của bạn "Body | Đẹp")
                Map.Entry<String, com.google.gson.JsonElement> firstEntry = jsonObject.entrySet().stream().findFirst().orElse(null);

                if (firstEntry != null) {
                    // Trả về dạng Key-Value
                    return new TagModel(firstEntry.getKey(), firstEntry.getValue().getAsString(), id); // <-- SỬA ĐỔI
                }
            } catch (JsonSyntaxException | IllegalStateException e) {
                // Không phải JSON hợp lệ, hoặc cấu trúc không mong muốn
                // Sẽ rơi xuống và coi là chuỗi thường
                System.err.println("Lỗi parse JSON trong TagModel, coi là chuỗi thường: " + rawName);
            }
        }

        // Mặc định là chuỗi thường
        return new TagModel(rawName, id); // <-- SỬA ĐỔI
    }


    /**
     * Chuyển đổi TagModel này TRỞ LẠI thành chuỗi String để LƯU vào DTO (trường Tags).
     */
    public String serialize() {
        if (isJson) {
            // Tạo lại đối tượng JSON
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(key, value);
            return gson.toJson(jsonObject);
        } else {
            return simpleName;
        }
    }

    /**
     * Lấy chuỗi hiển thị cho UI (cái "chip").
     */
    public String getDisplayName() {
        if (isJson) {
            return String.format("%s | %s", key, value);
        } else {
            return simpleName;
        }
    }

    public boolean isJson() {
        return isJson;
    }

    // (*** THÊM CÁC HÀM GETTERS MỚI ***)
    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    // (*** THÊM GETTER CHO ID ***)
    public String getId() {
        return id;
    }
    // (*** KẾT THÚC THÊM MỚI ***)

    // (QUAN TRỌNG) Thêm equals và hashCode để ListChangeListener có thể so sánh
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagModel tagModel = (TagModel) o;
        return isJson == tagModel.isJson &&
                Objects.equals(simpleName, tagModel.simpleName) &&
                Objects.equals(key, tagModel.key) &&
                Objects.equals(value, tagModel.value) &&
                Objects.equals(id, tagModel.id); // <-- THÊM ID VÀO SO SÁNH
    }

    @Override
    public int hashCode() {
        return Objects.hash(isJson, simpleName, key, value, id); // <-- THÊM ID VÀO HASH
    }

    @Override
    public String toString() {
        return "TagModel{" + getDisplayName() + (id != null ? ", id=" + id : "") + "}"; // <-- Sửa toString
    }
}