package com.example.embyapp.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class I18nManager {

    private static I18nManager instance;
    private final Map<String, Object> config = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    private I18nManager() {
        loadConfig();
    }

    public static synchronized I18nManager getInstance() {
        if (instance == null) {
            instance = new I18nManager();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream is = getClass().getResourceAsStream("/com/example/embyapp/config.json")) {
            if (is == null) {
                System.err.println("CRITICAL: config.json not found in resources!");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(is)) {
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> loadedMap = gson.fromJson(reader, type);
                if (loadedMap != null) {
                    config.putAll(loadedMap);
                    // System.out.println("Configuration loaded successfully.");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load config.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lấy một chuỗi string từ file config.
     *
     * @param key     Key chính (ví dụ: "loginView")
     * @param subKey  Key phụ (ví dụ: "title")
     * @param args    Các đối số để format (ví dụ: cho "Hello {0}")
     * @return Chuỗi đã được định dạng, hoặc key nếu không tìm thấy.
     */
    @SuppressWarnings("unchecked")
    public String getString(String key, String subKey, Object... args) {
        try {
            Map<String, String> section = (Map<String, String>) config.get(key);
            if (section != null) {
                String pattern = section.get(subKey);
                if (pattern != null) {
                    if (args.length > 0) {
                        return MessageFormat.format(pattern, args);
                    }
                    return pattern;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting string for key: " + key + "." + subKey + " | " + e.getMessage());
        }

        // Fallback
        return key + "." + subKey;
    }
}