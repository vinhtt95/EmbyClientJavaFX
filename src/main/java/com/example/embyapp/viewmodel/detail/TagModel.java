package com.example.embyapp.viewmodel.detail;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Objects;

/**
 * (MỚI) Lớp Model đại diện cho một TagItem đã được phân tích.
 * Nó có thể là một chuỗi đơn giản, hoặc một cặp Key-Value từ JSON.
 */
public class TagModel {

    private static final Gson gson = new Gson();

    private final boolean isJson;
    private final String simpleName;
    private final String key;
    private final String value;

    /**
     * Constructor cho tag chuỗi đơn giản.
     */
    public TagModel(String simpleName) {
        this.isJson = false;
        this.simpleName = simpleName;
        this.key = null;
        this.value = null;
    }

    /**
     * Constructor cho tag dạng Key-Value (từ JSON).
     */
    public TagModel(String key, String value) {
        this.isJson = true;
        this.simpleName = null;
        this.key = key;
        this.value = value;
    }

    /**
     * Phân tích một chuỗi 'Name' (từ NameLongIdPair) thành một TagModel.
     * Tự động phát hiện JSON.
     */
    public static TagModel parse(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return new TagModel("Trống");
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
                    return new TagModel(firstEntry.getKey(), firstEntry.getValue().getAsString());
                }
            } catch (JsonSyntaxException | IllegalStateException e) {
                // Không phải JSON hợp lệ, hoặc cấu trúc không mong muốn
                // Sẽ rơi xuống và coi là chuỗi thường
                System.err.println("Lỗi parse JSON trong TagModel, coi là chuỗi thường: " + rawName);
            }
        }

        // Mặc định là chuỗi thường
        return new TagModel(rawName);
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

    // (QUAN TRỌNG) Thêm equals và hashCode để ListChangeListener có thể so sánh
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagModel tagModel = (TagModel) o;
        return isJson == tagModel.isJson &&
                Objects.equals(simpleName, tagModel.simpleName) &&
                Objects.equals(key, tagModel.key) &&
                Objects.equals(value, tagModel.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isJson, simpleName, key, value);
    }

    @Override
    public String toString() {
        return "TagModel{" + getDisplayName() + "}";
    }
}